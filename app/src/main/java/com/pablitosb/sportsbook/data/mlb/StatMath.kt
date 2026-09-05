package com.pablitosb.sportsbook.data.mlb

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

object StatMath {
    fun parseInnings(raw: String?): Float {
        if (raw.isNullOrBlank()) return 0f
        val parts = raw.split(".")
        val whole = parts[0].toFloatOrNull() ?: return 0f
        val frac = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return whole + (frac.coerceIn(0, 2) / 3f)
    }

    fun rate(num: Int, den: Int): Float? {
        if (den <= 0) return null
        return num.toFloat() / den.toFloat()
    }

    fun shrink(raw: Float, n: Float, prior: Float, priorN: Float): Float {
        return (raw * n + prior * priorN) / (n + priorN)
    }

    fun poissonCdfLeq(k: Int, lambda: Double): Double {
        if (k < 0) return 0.0
        if (lambda <= 0.0) return 1.0
        var term = exp(-lambda)
        var sum = term
        for (i in 1..k) {
            term *= lambda / i
            sum += term
            if (term < 1e-15) break
        }
        return sum.coerceIn(0.0, 1.0)
    }

    /** P(X > line) for a .5 line (e.g. 6.5 → P(X ≥ 7)). */
    fun poissonOver(line: Double, lambda: Double): Float {
        val threshold = kotlin.math.floor(line).toInt()
        return (1.0 - poissonCdfLeq(threshold, lambda)).toFloat().coerceIn(0.01f, 0.99f)
    }

    fun impliedFromAmerican(odds: Int): Float {
        return if (odds < 0) {
            val a = -odds.toFloat()
            a / (a + 100f)
        } else {
            100f / (odds + 100f)
        }
    }

    fun nearestHalf(value: Float): Float {
        val snapped = (value * 2f).roundToInt() / 2f
        val base = if (snapped == snapped.toInt().toFloat()) snapped - 0.5f else snapped
        return base.coerceAtLeast(0.5f)
    }

    fun americanLabel(odds: Int): String = if (odds > 0) "+$odds" else odds.toString()

    fun logLossSafe(p: Float): Float = (-ln(p.coerceIn(0.01f, 0.99f))).toFloat()
}
