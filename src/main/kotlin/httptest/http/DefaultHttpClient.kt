package httptest.http

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOr
import com.github.michaelbull.result.map
import com.github.michaelbull.result.runCatching
import httptest.util.incWhileRunning
import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import io.prometheus.client.Histogram
import org.apache.hc.client5.http.async.methods.SimpleBody
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.client5.http.impl.async.HttpAsyncClients
import org.apache.hc.core5.io.CloseMode
import org.apache.hc.core5.reactor.IOReactorConfig
import org.apache.hc.core5.util.Timeout
import java.util.concurrent.TimeUnit

object DefaultHttpClient : HttpClient {
    private val client: CloseableHttpAsyncClient by lazy {
        fun createClient(): CloseableHttpAsyncClient {
            val reactorConfig = IOReactorConfig
                .custom()
                .setSoTimeout(Timeout.ofSeconds(5))
                .build()

            val requestConfig = RequestConfig
                .custom()
                .setConnectTimeout(15, TimeUnit.SECONDS)
                .setResponseTimeout(90, TimeUnit.SECONDS)
                .build()

            return HttpAsyncClients
                .custom()
                .setDefaultRequestConfig(requestConfig)
                .setIOReactorConfig(reactorConfig)
                .build()
                .also { it.start() }
        }

        createClient()
    }

    private object Metrics {
        val requestCounter = Counter.build()
            .name("http_client_requests")
            .help("Total outgoing HTTP requests.")
            .labelNames("verb", "url")
            .register()
        val pendingRequests = Gauge.build()
            .name("http_client_pending_requests")
            .help("Currently active HTTP requests.")
            .labelNames("verb", "url")
            .register()
        val requestLatency = Histogram.build()
            .name("http_client_request_latency")
            .help("Histogram of HTTP request/response latency.")
            .labelNames("verb", "url", "status")
            .register()

    }

    override suspend fun get(
        url: String,
        block: SimpleRequestBuilder.() -> SimpleRequestBuilder
    ): Result<SimpleHttpResponse, Throwable> = request {
        block(SimpleRequestBuilder.get(url))
    }

    override suspend fun head(
        url: String,
        block: SimpleRequestBuilder.() -> SimpleRequestBuilder
    ): Result<SimpleHttpResponse, Throwable> = request {
        block(SimpleRequestBuilder.head(url))
    }

    override suspend fun post(
        url: String,
        body: SimpleBody,
        block: SimpleRequestBuilder.() -> SimpleRequestBuilder
    ): Result<SimpleHttpResponse, Throwable> = request {
        block(SimpleRequestBuilder.post(url).setBody(body))
    }

    override suspend fun put(
        url: String,
        body: SimpleBody,
        block: SimpleRequestBuilder.() -> SimpleRequestBuilder
    ): Result<SimpleHttpResponse, Throwable> = request {
        block(SimpleRequestBuilder.put(url).setBody(body))
    }

    override suspend fun delete(
        url: String,
        block: SimpleRequestBuilder.() -> SimpleRequestBuilder
    ): Result<SimpleHttpResponse, Throwable> = request {
        block(SimpleRequestBuilder.delete(url))
    }

    override suspend fun options(
        url: String,
        block: SimpleRequestBuilder.() -> SimpleRequestBuilder
    ): Result<SimpleHttpResponse, Throwable> = request {
        block(SimpleRequestBuilder.options(url))
    }

    override suspend fun patch(
        url: String,
        body: SimpleBody,
        block: SimpleRequestBuilder.() -> SimpleRequestBuilder
    ): Result<SimpleHttpResponse, Throwable> = request {
        block(SimpleRequestBuilder.patch(url).setBody(body))
    }

    override suspend fun request(
        block: () -> SimpleRequestBuilder
    ): Result<SimpleHttpResponse, Throwable> {
        val request = block().build()

        Metrics.requestCounter
            .labels(request.method, request.uri.toString())
            .inc()

        return Metrics.pendingRequests
            .labels(request.method, request.uri.toString())
            .incWhileRunning {
                val (response, time) = timed {
                    runCatching {
                        client.execute(request)
                    }
                }

                Metrics.requestLatency
                    .labels(
                        request.method,
                        request.uri.toString(),
                        response.map { it.code }.getOr(0).toString(),
                    )
                    .observe(time)

                response
            }
    }

    override fun close() {
        client.close(CloseMode.GRACEFUL)
    }
}
