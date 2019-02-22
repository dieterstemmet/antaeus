package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging
import java.math.BigDecimal

class BillingService(private val paymentProvider: PaymentProvider, private val invoiceService: InvoiceService, private val customerService: CustomerService) {

    private val logger = KotlinLogging.logger {}

    internal var successCnt = 0
    internal var failureCnt = 0
    private var retryList = ArrayList<Int>()

    // Leaving this option so it can be exposed in later versions
    private var retry = true

    /**
     * Pays all invoices
     */
    fun payInvoices() {

        val invoices = invoiceService.fetchAll()

        if (invoices.isNotEmpty()) {
            logger.debug { "Charging ${invoices.size} invoices" }
        } else {
            logger.debug { "No invoices to charge" }
        }

        for (invoice in invoices) {

            if (payInvoice(invoice)) {
                successCnt++
                invoiceService.updateInvoice(invoice.id, InvoiceStatus.PAID)
            } else {
                failureCnt++
                invoiceService.updateInvoice(invoice.id, InvoiceStatus.PENDING)
            }

        }

        retryFailures()

        logger.debug { "Payment summary [successful payments: $successCnt; failures: $failureCnt]" }

    }

    private fun retryFailures() {

        if (retry && retryList.isNotEmpty()) {

            logger.debug { "Charging ${retryList.size} failed invoices" }

            for (invoice in retryList) {

                if (payInvoice(invoice)) {

                    retryList.remove(invoice)
                    successCnt++
                    invoiceService.updateInvoice(invoice, InvoiceStatus.PAID)
                    failureCnt--

                }

            }
        }

    }

    /**
     * Pays single invoice
     */
    private fun payInvoice(invoice: Invoice): Boolean {

        logger.trace {
            "Attempting to charge customer: ${invoice.customerId} " +
                    "for invoice: ${invoice.id} " +
                    "[${invoice.amount.value.setScale(2, BigDecimal.ROUND_HALF_UP)} " +
                    "${invoice.amount.currency}]"
        }
        if (invoice.status == InvoiceStatus.PENDING) {

            try {

                val customer = customerService.fetch(invoice.customerId)
                if (customer.currency != invoice.amount.currency) {

                    // Throwing this here instead of waiting for the payment provider so we can fail faster
                    throw CurrencyMismatchException(invoice.id, customer.id)

                }

                val result = paymentProvider.charge(invoice)

                logger.trace {
                    "Successfully charged: ${invoice.customerId} " +
                            "for invoice: ${invoice.id} " +
                            "[${invoice.amount.value.setScale(2, BigDecimal.ROUND_HALF_UP)} " +
                            "${invoice.amount.currency}]"
                }
                return result

            } catch (e: NetworkException) {
                logger.error(e) {
                    "Could not charge: ${invoice.customerId} " +
                            "for invoice: ${invoice.id} " +
                            "[${invoice.amount.value.setScale(2, BigDecimal.ROUND_HALF_UP)} " +
                            "${invoice.amount.currency}]"
                }
                retryList.add(invoice.id)
            } catch (e: CustomerNotFoundException) {
                logger.error(e) {
                    "Could not charge: ${invoice.customerId} " +
                            "for invoice: ${invoice.id} " +
                            "[${invoice.amount.value.setScale(2, BigDecimal.ROUND_HALF_UP)} " +
                            "${invoice.amount.currency}], skipping invoice"
                }

            } catch (e: CurrencyMismatchException) {
                logger.error(e) {
                    "Could not charge: ${invoice.customerId} " +
                            "for invoice: ${invoice.id} " +
                            "[${invoice.amount.value.setScale(2, BigDecimal.ROUND_HALF_UP)} " +
                            "${invoice.amount.currency}], skipping invoice"
                }

            }
        }

        return false

    }

    /**
     * Pays single invoice wrapper
     */
    fun payInvoice(invoiceId: Int): Boolean {

        val result = payInvoice(invoiceService.fetch(invoiceId))

        if (result) {
            successCnt++
            invoiceService.updateInvoice(invoiceId, InvoiceStatus.PAID)

        } else {
            failureCnt++
            invoiceService.updateInvoice(invoiceId, InvoiceStatus.PENDING)
        }

        return result

    }

}