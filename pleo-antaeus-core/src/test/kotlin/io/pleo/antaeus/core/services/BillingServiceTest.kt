package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.random.Random

class BillingServiceTest {

    /**
     * Mock Customer DAL
     */
    private val customerDal = mockk<AntaeusDal>(relaxed = true) {
        every { fetchCustomers() } returns listOf(
                Customer(1, Currency.EUR), // working
                Customer(2, Currency.USD), // working
                Customer(3, Currency.GBP)  // working
        )
        every { fetchCustomer(404) } returns null // customer not found
        every { fetchCustomer(5) } returns Customer(5, Currency.DKK) // working
        every { fetchCustomer(6) } returns Customer(6, Currency.USD) // currency doesn't match
    }

    /**
     * Mock Invoice DAL
     */
    private val invoiceDal = mockk<AntaeusDal>(relaxed = true) {
        every { fetchInvoices() } returns listOf(
                Invoice(1, 1, Money(
                        value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                        currency = Currency.EUR
                ), InvoiceStatus.PENDING), // working
                Invoice(2, 2, Money(
                        value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                        currency = Currency.USD
                ), InvoiceStatus.PENDING), // working
                Invoice(3, 3, Money(
                        value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                        currency = Currency.GBP
                ), InvoiceStatus.PENDING) // working
        )
        every { fetchInvoice(4) } returns
                Invoice(4, 404, Money(
                        value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                        currency = Currency.SEK
                ), InvoiceStatus.PENDING)  // customer not found
        every { fetchInvoice(5) } returns
                Invoice(5, 5, Money(
                        value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                        currency = Currency.DKK
                ), InvoiceStatus.PENDING)  // working
        every { fetchInvoice(6) } returns
                Invoice(6, 6, Money(
                        value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                        currency = Currency.EUR
                ), InvoiceStatus.PENDING)  // currency doesn't match
    }

    /*
        === Core Services ===
     */
    private val invoiceService = InvoiceService(dal = invoiceDal)
    private val customerService = CustomerService(dal = customerDal)

    /*
        === Billing Services ===
     */
    private val alwaysTrueBillingService = BillingService(
            paymentProvider = mockPaymentProvider(true),
            invoiceService = invoiceService,
            customerService = customerService)
    private val alwaysFalseBillingService = BillingService(
            paymentProvider = mockPaymentProvider(false),
            invoiceService = invoiceService,
            customerService = customerService)

    /*
        === Tests ===
     */
    @Test
    fun `will successfully pay all invoices`() {
        // Todo: Add success & failure counter and test on result
        // assert(alwaysTrueBillingService.payInvoices())
    }

    @Test
    fun `will throw if customer is not found`() {
        assertThrows<CustomerNotFoundException> {
            alwaysTrueBillingService.payInvoice(4)
        }
    }

    @Test
    fun `will successfully pay an invoice`() {
        assertTrue(alwaysTrueBillingService.payInvoice(5))
    }

    @Test
    fun `will attempt to pay an invoice and fail`() {
        assertFalse(alwaysFalseBillingService.payInvoice(5))
    }

    @Test
    fun `will throw if currency doesn't match`() {
        assertThrows<CurrencyMismatchException> {
            alwaysTrueBillingService.payInvoice(6)
        }
    }

    @Test
    fun `will throw if there is a network exception`() {
        assertThrows<NetworkException> {
            // Todo: implement network check and drop
            //  alwaysTrueBillingService.payInvoices()
        }
    }

    @Test
    fun `will pay an invoice and update payment status`() {
        assertTrue(invoiceService.fetch(5).status == InvoiceStatus.PENDING)
        alwaysTrueBillingService.payInvoice(5)
        assertTrue(invoiceService.fetch(5).status == InvoiceStatus.PAID)
    }

}