package com.genvision.nova.core.nav

/** Source of time for every timer, so tests can use a fake clock. */
interface Clock {
    fun nowMs(): Long
}
