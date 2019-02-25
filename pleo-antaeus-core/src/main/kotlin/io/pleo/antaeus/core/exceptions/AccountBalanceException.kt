package io.pleo.antaeus.core.exceptions

class AccountBalanceException(customerId: Int, invoiceId: Int) : Exception("The customer '$customerId' account balance did not allow the charge for invoice '$invoiceId'")