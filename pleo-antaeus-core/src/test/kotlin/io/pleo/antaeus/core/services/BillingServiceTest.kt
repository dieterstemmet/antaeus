package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.tasks.PaymentTask
import io.pleo.antaeus.core.tasks.brokenPaymentProvider
import io.pleo.antaeus.core.tasks.mockPaymentProvider
import io.pleo.antaeus.core.tasks.nextTwoSeconds
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.*
import io.pleo.antaeus.models.Currency
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*
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
        every { fetchCustomer(1) } returns Customer(1, Currency.EUR) // working
        every { fetchCustomer(2) } returns Customer(2, Currency.USD) // working
        every { fetchCustomer(3) } returns Customer(3, Currency.GBP) // working

        every { fetchCustomer(404) } returns null // customer not found
        every { fetchCustomer(5) } returns Customer(5, Currency.DKK) // working
        every { fetchCustomer(6) } returns Customer(6, Currency.USD) // currency doesn't match
        every { fetchCustomer(7) } returns Customer(7, Currency.USD) // already paid
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
        every { fetchInvoice(1) } returns
                Invoice(1, 1, Money(
                        value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                        currency = Currency.EUR
                ), InvoiceStatus.PENDING) // working
        every { fetchInvoice(2) } returns
                Invoice(2, 2, Money(
                        value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                        currency = Currency.USD
                ), InvoiceStatus.PENDING) // working
        every { fetchInvoice(3) } returns
                Invoice(3, 3, Money(
                        value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                        currency = Currency.GBP
                ), InvoiceStatus.PENDING) // working
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
        every { fetchInvoice(7) } returns
                Invoice(7, 7, Money(
                        value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                        currency = Currency.USD
                ), InvoiceStatus.PAID)  // already paid
    }

    /**
     * Core services
     */
    private val invoiceService = InvoiceService(dal = invoiceDal)
    private val customerService = CustomerService(dal = customerDal)

    /**
     * Billing services
     */
    private val successBillingService = BillingService(
            paymentProvider = mockPaymentProvider(true),
            invoiceService = invoiceService,
            customerService = customerService)
    private val failureBillingService = BillingService(
            paymentProvider = mockPaymentProvider(false),
            invoiceService = invoiceService,
            customerService = customerService)
    private val networkIssueBillingService = BillingService(
            paymentProvider = brokenPaymentProvider(),
            invoiceService = invoiceService,
            customerService = customerService)

    /**
     * Tests
     */
    @Test
    fun `will successfully pay all invoices`() {
        successBillingService.payInvoices(true)
        assertEquals(3, successBillingService.successCnt)
        assertEquals(0, successBillingService.failureCnt)
    }

    @Test
    fun `will attempt and fail to pay all invoices`() {
        failureBillingService.payInvoices(true, 0.0)
        assertEquals(0, failureBillingService.successCnt)
        // Sleep to wait for task completion
        Thread.sleep(2000)
        assertEquals(12, failureBillingService.failureCnt)
    }

    @Test
    fun `will fail because customer is not found`() {
        assertFalse(successBillingService.payInvoice(4))
        assertEquals(1, successBillingService.invalidCnt)
    }

    @Test
    fun `will successfully pay a single invoice`() {
        assertTrue(successBillingService.payInvoice(5))
        assertEquals(1, successBillingService.successCnt)
    }

    @Test
    fun `will attempt to pay a single invoice and fail`() {
        assertFalse(failureBillingService.payInvoice(5))
        assertEquals(1, failureBillingService.failureCnt)
    }

    @Test
    fun `will fail because currency doesn't match`() {
        assertFalse(successBillingService.payInvoice(6))
        assertEquals(1, successBillingService.invalidCnt)
    }

    @Test
    fun `will fail because of a network exception`() {
        assertFalse(networkIssueBillingService.payInvoice(5))
        assertEquals(1, networkIssueBillingService.failureCnt)
    }

    @Test
    fun `will skip invoice because it has already been paid`() {
        assertFalse(successBillingService.payInvoice(7))
        assertEquals(1, successBillingService.skippedCnt)
    }

    @Test
    fun `test scheduler`() {
        val timer = Timer()
        timer.schedule(PaymentTask(successBillingService, timer), nextTwoSeconds())
        // Sleep to wait for task completion
        Thread.sleep(3000)
    }

}