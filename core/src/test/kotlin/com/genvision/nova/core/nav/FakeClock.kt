package com.genvision.nova.core.nav

class FakeClock(var now: Long = 0L) : Clock {
    override fun nowMs() = now
    fun advance(ms: Long) { now += ms }
}
