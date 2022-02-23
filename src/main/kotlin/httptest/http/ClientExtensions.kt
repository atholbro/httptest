package httptest.http

import kotlinx.coroutines.cancelFutureOnCancellation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.core5.concurrent.FutureCallback
import java.util.concurrent.CancellationException
import kotlin.coroutines.resumeWithException

internal suspend fun CloseableHttpAsyncClient.execute(
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
                    cont.resumeWithException(ex!!)
                }

                override fun cancelled() {
                    if (!cont.isCancelled) {
                        cont.resumeWithException(CancellationException())
                    }
                }
            }
        )

        cont.cancelFutureOnCancellation(future)
    }
