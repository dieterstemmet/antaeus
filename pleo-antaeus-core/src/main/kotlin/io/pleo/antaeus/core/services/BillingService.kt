package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice

class BillingService(private val paymentProvider: PaymentProvider, private val invoiceService: InvoiceService, private val customerService: CustomerService) {

    // Todo Add logging
    // private val logger = KotlinLogging.logger {}

    /**
     * Pays all invoices
     */
    fun payInvoices() {

        val invoices = invoiceService.fetchAll()
        for (invoice in invoices) {
            payInvoice(invoice)
        }

    }

    /**
     * Pays single invoice
     */
    private fun payInvoice(invoice: Invoice): Boolean {
        val customer = customerService.fetch(invoice.customerId)
        if (customer.currency != invoice.amount.currency) {
            throw CurrencyMismatchException(invoice.id, customer.id)
        }
        return paymentProvider.charge(invoice)
    }

    /**
     * Pays single invoice wrapper
     */
    fun payInvoice(invoiceId: Int): Boolean {
        return payInvoice(invoiceService.fetch(invoiceId))
    }

    /* Todo: Check and throw exception
         3. when a network error happens - `NetworkException`
    */
}