package httptest.util

import io.prometheus.client.Gauge
import io.prometheus.client.SimpleTimer

internal inline fun <T> Gauge.Child.incWhileRunning(block: () -> T): T = try {
    inc()
    block()
} finally {
    dec()
}

internal inline fun <T> timed(block: () -> T): Pair<T, Double> {
    val timer = SimpleTimer()
    return Pair(
        block(),
        timer.elapsedSeconds()
    )
}
