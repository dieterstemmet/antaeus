package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider

class BillingService(private val paymentProvider: PaymentProvider) {

    // Todo: Inject invoice and customer services

    // Todo Implement charge
    fun charge() {}

    /* Todo: Check and throw exception
         1. when no customer has the given id - `CustomerNotFoundException`
         2. when the currency does not match the customer account - `CurrencyMismatchException`
         3. when a network error happens - `NetworkException`
    */
}