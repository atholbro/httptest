package httptest.http

import com.github.michaelbull.result.Result
import io.prometheus.client.SimpleTimer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelFutureOnCancellation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.hc.client5.http.async.methods.SimpleBody
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.core5.concurrent.FutureCallback

interface HttpClient {
    suspend fun get(
        url: String,
        block: SimpleRequestBuilder.() -> SimpleRequestBuilder = { this },
    ): Result<SimpleHttpResponse, Throwable>

    suspend fun head(
        url: String,
        block: SimpleRequestBuilder.() -> SimpleRequestBuilder = { this },
    ): Result<SimpleHttpResponse, Throwable>

    suspend fun post(
        url: String,
        body: SimpleBody,
        block: SimpleRequestBuilder.() -> SimpleRequestBuilder = { this },
    ): Result<SimpleHttpResponse, Throwable>

    suspend fun put(
        url: String,
        body: SimpleBody,
        block: SimpleRequestBuilder.() -> SimpleRequestBuilder = { this },
    ): Result<SimpleHttpResponse, Throwable>

    suspend fun delete(
        url: String,
        block: SimpleRequestBuilder.() -> SimpleRequestBuilder = { this },
    ): Result<SimpleHttpResponse, Throwable>

    suspend fun options(
        url: String,
        block: SimpleRequestBuilder.() -> SimpleRequestBuilder = { this },
    ): Result<SimpleHttpResponse, Throwable>

    suspend fun patch(
        url: String,
        body: SimpleBody,
        block: SimpleRequestBuilder.() -> SimpleRequestBuilder = { this },
    ): Result<SimpleHttpResponse, Throwable>

    suspend fun request(
        block: () -> SimpleRequestBuilder,
    ): Result<SimpleHttpResponse, Throwable>

    fun close()
}

inline fun <T> timed(block: () -> T): Pair<T, Double> {
    val timer = SimpleTimer()
    return Pair(
        block(),
        timer.elapsedSeconds()
    )
}


