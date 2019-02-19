package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class BillingServiceTest {


    private val customerDal = mockk<AntaeusDal> {
        every { fetchCustomer(203) } returns null
    }

    private val paymentProvider = mockkClass(PaymentProvider::class) {
        every { charge(Invoice(1, 1, Money(BigDecimal.valueOf(50.0), Currency.EUR), InvoiceStatus.PAID)) } returns false
    }

    /* Might need this later

       private val invoiceDal = mockk<AntaeusDal> {
           every { fetchInvoice(203) } returns null
       }
       private val invoiceService = InvoiceService(dal = invoiceDal)

    */

    private val customerService = CustomerService(dal = customerDal)
    private val billingService = BillingService(paymentProvider)

    @Test
    fun `will throw if customer is not found`() {
        assertThrows<CustomerNotFoundException> {
            customerService.fetch(203)
        }
    }

    @Test
    fun `will throw if currency doesn't match`() {
        assertThrows<CurrencyMismatchException> {
            billingService.charge()
            // Todo: implement currency change and check
        }
    }

    @Test
    fun `will throw if there is a network exception`() {
        assertThrows<NetworkException> {
            billingService.charge()
            // Todo: implement network check and drop
        }
    }

}