package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice

/**
 * Mock payment provider with set outcome
 */
internal fun mockPaymentProvider(success: Boolean): PaymentProvider {
    return object : PaymentProvider {
        override fun charge(invoice: Invoice): Boolean {
            return success
        }
    }
}