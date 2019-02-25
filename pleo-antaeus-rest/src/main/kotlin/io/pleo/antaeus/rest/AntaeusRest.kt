/*
    Configures the rest app along with basic exception handling and URL endpoints.
 */

package io.pleo.antaeus.rest

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.EntityNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import mu.KotlinLogging
import java.math.BigDecimal

private val logger = KotlinLogging.logger {}

class AntaeusRest (
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
    private val billingService: BillingService
) : Runnable {

    override fun run() {
        app.start(7000)
    }

    // Set up Javalin rest app
    private val app = Javalin
        .create()
        .apply {
            // CustomerNotFoundException: return 403 HTTP status code
            exception(CustomerNotFoundException::class.java) { _, ctx ->
                ctx.status(403)
            }
            // NetworkException: return 403 HTTP status code
            exception(NetworkException::class.java) { _, ctx ->
                ctx.status(403)
            }
            // CurrencyMismatchException: return 403 HTTP status code
            exception(CurrencyMismatchException::class.java) { _, ctx ->
                ctx.status(403)
            }
            // InvoiceNotFoundException: return 404 HTTP status code
            exception(EntityNotFoundException::class.java) { _, ctx ->
                ctx.status(404)
            }
            // Unexpected exception: return HTTP 500
            exception(Exception::class.java) { e, _ ->
                logger.error(e) { "Internal server error" }
            }
            // On 404: return message
            error(404) { ctx -> ctx.json("not found") }
        }

    init {
        // Set up URL endpoints for the rest app
        app.routes {
           path("rest") {
               // Route to check whether the app is running
               // URL: /rest/health
               get("health") {
                   it.json("ok")
               }

               // V1
               path("v1") {
                   path("invoices") {
                       // URL: /rest/v1/invoices
                       get {
                           it.json(invoiceService.fetchAll())
                       }

                       // URL: /rest/v1/invoices/{:id}
                       get(":id") {
                          it.json(invoiceService.fetch(it.pathParam("id").toInt()))
                       }
                   }

                   path("customers") {
                       // URL: /rest/v1/customers
                       get {
                           it.json(customerService.fetchAll())
                       }

                       // URL: /rest/v1/customers/{:id}
                       get(":id") {
                           it.json(customerService.fetch(it.pathParam("id").toInt()))
                       }
                   }

                   // Path for billing runs
                   path("billing") {
                       // URL: /rest/v1/billing
                       get {
                           it.json(billingService.payInvoices(it.queryParam("retry", "true")!!.toBoolean(), it.queryParam("retryAfter", "0.25")!!.toDouble()))
                       }

                       // URL: /rest/v1/billing/{:id}
                       get(":id") {
                           it.json(billingService.payInvoice(it.pathParam("id").toInt()))
                       }
                   }

               }
           }
        }
    }
}
