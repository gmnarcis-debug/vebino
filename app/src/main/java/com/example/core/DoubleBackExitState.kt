package com.example.core

import android.os.SystemClock

class DoubleBackExitState(private val windowMs: Long = 2000L) {
    private var lastBackPressedMs: Long = Long.MIN_VALUE

    fun onBackPressed(nowMs: Long = SystemClock.elapsedRealtime()): Boolean {
        val withinWindow = lastBackPressedMs != Long.MIN_VALUE && (nowMs - lastBackPressedMs) in 0L..windowMs
        if (withinWindow) {
            lastBackPressedMs = Long.MIN_VALUE
            return true
        }
        lastBackPressedMs = nowMs
        return false
    }

    fun reset() {
        lastBackPressedMs = Long.MIN_VALUE
    }
}
