package httptest

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelFutureOnCancellation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.hc.client5.http.async.methods.*
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.client5.http.impl.async.HttpAsyncClients
import org.apache.hc.core5.concurrent.FutureCallback
import org.apache.hc.core5.io.CloseMode
import org.apache.hc.core5.reactor.IOReactorConfig
import org.apache.hc.core5.util.Timeout

interface HttpClient {
    suspend fun get(url: String): String
    fun close()
}

object DefaultHttpClient : HttpClient {
    private val client: CloseableHttpAsyncClient by lazy {
        fun createClient(): CloseableHttpAsyncClient {
            val reactorConfig = IOReactorConfig
                .custom()
                .setSoTimeout(Timeout.ofSeconds(5))
                .build()

            return HttpAsyncClients
                .custom()
                .setIOReactorConfig(reactorConfig)
                .build()
                .also { it.start() }
        }

        createClient()
    }

    override suspend fun get(url: String): String {
        val request = SimpleRequestBuilder.get(url)
            .build()

        val response = client.execute(request)
        return response.body.bodyText
    }

    override fun close() {
        client.close(CloseMode.GRACEFUL)
    }
}

private suspend fun CloseableHttpAsyncClient.execute(
    request: SimpleHttpRequest,
): SimpleHttpResponse =
    suspendCancellableCoroutine { cont ->
        val future = execute(
            SimpleRequestProducer.create(request),
            SimpleResponseConsumer.create(),
            object : FutureCallback<SimpleHttpResponse> {
                override fun completed(result: SimpleHttpResponse?) {
                    cont.resumeWith(Result.success(result!!))
                }

                override fun failed(ex: Exception?) {
                    cont.resumeWith(Result.failure(ex!!))
                }

                override fun cancelled() {
                    if (!cont.isCancelled) {
                        cont.resumeWith(Result.failure(CancellationException()))
                    }
                }
            }
        )

        cont.cancelFutureOnCancellation(future)
    }