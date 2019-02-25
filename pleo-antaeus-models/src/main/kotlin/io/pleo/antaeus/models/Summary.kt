package io.pleo.antaeus.models

/**
 * Run counters
 */
data class Summary(
        val paid: Int,
        val failed: Int,
        val invalid: Int,
        val skipped: Int,
        val total: Int
)