package com.emmanuela.launcher.data

/** Expired limits already have a blocking result; never turn them into a polling loop. */
object RuleDeadline {
    fun next(candidates: List<Long>): Long? = candidates.filter { it > 0 }.minOrNull()
}
