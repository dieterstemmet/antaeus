package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.AccountBalanceException
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Summary
import mu.KotlinLogging
import java.math.BigDecimal
import java.util.concurrent.Executors


class BillingService(private val paymentProvider: PaymentProvider, private val invoiceService: InvoiceService, private val customerService: CustomerService) {

    private val logger = KotlinLogging.logger {}

    // Summary counters
    internal var successCnt = 0
    internal var failureCnt = 0
    internal var invalidCnt = 0
    internal var skippedCnt = 0

    // Whether or not a retry should be run for failed charges
    private var retry = true

    // Amount of times to retry
    private val retryLimit = 3
    private var retryCnt = 0
    private var retryAfterMin = 0.25

    // List of charges to retry
    private val retryList = mutableListOf<Int>()

    /**
     * Pays all invoices
     */
    fun payInvoices(retryFailures: Boolean = true, retryAfter: Double = 0.25): Summary {

        retry = retryFailures
        retryAfterMin = retryAfter

        val invoices = invoiceService.fetchAll()

        if (invoices.isNotEmpty()) {
            clearCounters()

            logger.info { "Found ${invoices.size} invoices" }
            for (invoice in invoices) {
                payInvoice(invoice)
            }

        } else {
            logger.warn { "No invoices found to charge" }
        }

        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            retryCnt = 0
            retryFailures()
        }

        val summary = Summary(successCnt, failureCnt, invalidCnt, skippedCnt, invoices.size)
        logger.info { "Payment summary: $summary" }
        return summary

    }

    /**
     * Pays a single invoice
     */
    private fun payInvoice(invoice: Invoice): Boolean {

        when {

            invoice.status == InvoiceStatus.PENDING -> {

                logger.info {
                    "Attempting to charge customer: ${invoice.customerId} " +
                            "for invoice: ${invoice.id} " +
                            "[${invoice.amount.value.setScale(2, BigDecimal.ROUND_HALF_UP)} " +
                            "${invoice.amount.currency}]"
                }

                return chargePaymentProvider(invoice)

            }

            invoice.status == InvoiceStatus.PAID -> {

                logger.info {
                    "Skipping customer: ${invoice.customerId} invoice: ${invoice.id} " +
                            "[${invoice.amount.value.setScale(2, BigDecimal.ROUND_HALF_UP)} " +
                            "${invoice.amount.currency}] already paid"
                }
                skippedCnt++

            }
            else -> {
                logger.warn {
                    "Unknown invoice status: ${invoice.status}, cannot pay"
                }
                invalidCnt++
            }
        }

        return false

    }

    /**
     * Charges the payment provider
     */
    private fun chargePaymentProvider(invoice: Invoice): Boolean {

        try {

            val customer = customerService.fetch(invoice.customerId)
            if (customer.currency != invoice.amount.currency) {

                // Throwing this here instead of waiting for the payment provider so we can fail faster
                throw CurrencyMismatchException(invoice.id, customer.id)

            }

            if (paymentProvider.charge(invoice)) {

                logger.info {
                    "Successfully charged customer: ${invoice.customerId} " +
                            "for invoice: ${invoice.id} " +
                            "[${invoice.amount.value.setScale(2, BigDecimal.ROUND_HALF_UP)} " +
                            "${invoice.amount.currency}]"
                }

                markSuccess(invoice.id)
                return true

            } else {
                throw AccountBalanceException(invoice.customerId, invoice.id)
            }

        } catch (e: NetworkException) {
            logger.error {
                "Could not charge: ${invoice.customerId} " +
                        "for invoice: ${invoice.id} " +
                        "[${invoice.amount.value.setScale(2, BigDecimal.ROUND_HALF_UP)} " +
                        "${invoice.amount.currency}]: ${e.message}"
            }
            failureCnt++
        } catch (e: CustomerNotFoundException) {
            logger.error {
                "Could not charge: ${invoice.customerId} " +
                        "for invoice: ${invoice.id} " +
                        "[${invoice.amount.value.setScale(2, BigDecimal.ROUND_HALF_UP)} " +
                        "${invoice.amount.currency}]: ${e.message}, skipping invoice"
            }
            invalidCnt++
        } catch (e: CurrencyMismatchException) {
            logger.error {
                "Could not charge: ${invoice.customerId} " +
                        "for invoice: ${invoice.id} " +
                        "[${invoice.amount.value.setScale(2, BigDecimal.ROUND_HALF_UP)} " +
                        "${invoice.amount.currency}]: ${e.message}, skipping invoice"
            }
            invalidCnt++
        } catch (e: AccountBalanceException) {
            logger.error {
                "Could not charge: ${invoice.customerId} " +
                        "for invoice: ${invoice.id} " +
                        "[${invoice.amount.value.setScale(2, BigDecimal.ROUND_HALF_UP)} " +
                        "${invoice.amount.currency}]: ${e.message}"
            }
            failureCnt++
        }

        if (!retryList.contains(invoice.id)) {
            retryList.add(invoice.id)
        }
        return false

    }


    /**
     * Pay single invoice wrapper
     */
    fun payInvoice(invoiceId: Int): Boolean {
        return payInvoice(invoiceService.fetch(invoiceId))
    }

    /**
     * Marks an invoice payment as successful
     */
    private fun markSuccess(invoiceId: Int) {
        successCnt++
        invoiceService.updateStatus(invoiceId, InvoiceStatus.PAID)
    }

    /**
     * Retries all failed payments
     */
    private fun retryFailures() {

        if (retry && retryList.isNotEmpty()) {

            while (retryCnt < retryLimit) {
                retryCnt++

                logger.info { "Running retry after $retryAfterMin minutes" }
                Thread.sleep((retryAfterMin * 60000).toLong())

                logger.info { "Paying ${retryList.size} failed invoices [retry run: $retryCnt/$retryLimit]" }

                val iter = retryList.iterator()
                while (iter.hasNext()) {
                    val invoiceId = iter.next()
                    if (payInvoice(invoiceId)) {
                        logger.info { "Removing invoice: $invoiceId from the retry list" }
                        iter.remove()
                        failureCnt--
                    }
                }

                val summary = Summary(successCnt, failureCnt, invalidCnt, skippedCnt, retryCnt)
                logger.info { "Retry summary: $summary" }

            }

        } else {
            logger.info { "No failed invoices to pay" }
        }

    }

    /**
     * Clears summary counters
     */
    private fun clearCounters() {
        successCnt = 0
        failureCnt = 0
        invalidCnt = 0
        skippedCnt = 0
    }

}