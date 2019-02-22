package io.pleo.antaeus.core.tasks

import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.*

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

/**
 * Mock payment provider which throws a network exception
 */
internal fun brokenPaymentProvider(): PaymentProvider {
    return object : PaymentProvider {
        override fun charge(invoice: Invoice): Boolean {
            throw NetworkException()
        }
    }
}

/**
 * Date utilities
 */
internal fun localDateTimeToDate(dateToConvert: LocalDateTime): Date {
    return Date.from(dateToConvert.atZone(ZoneId.systemDefault()).toInstant())
}

internal fun nextMonthsRunDate(): Date {
    return localDateTimeToDate(LocalDateTime
            .now()
            .withHour(8)
            .withMinute(5)
            .withSecond(0)
            .withDayOfMonth(1)
            .withMonth(YearMonth.now().monthValue + 1))
}

internal fun nextTwoSeconds(): Date {
    return localDateTimeToDate(LocalDateTime
            .now()
            .plusSeconds(2))
}