package io.pleo.antaeus.core.tasks

import io.pleo.antaeus.core.services.BillingService
import mu.KotlinLogging
import java.util.*

class PaymentTask(private val billingService: BillingService, private val timer: Timer) : TimerTask() {

    private val logger = KotlinLogging.logger {}

    override fun run() {
        try {
            logger.info { "Running scheduled payments" }
            billingService.payInvoices()
            logger.info { "Finished scheduled payments" }
        } finally {
            val runDate = nextMonthsRunDate()
            logger.info { "Scheduling next task for $runDate" }
            timer.schedule(PaymentTask(billingService, timer), runDate)
        }
    }

}