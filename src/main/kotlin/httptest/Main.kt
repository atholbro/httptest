package httptest

import httptest.http.DefaultHttpClient
import httptest.http.HttpClient
import io.prometheus.client.exporter.HTTPServer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


fun main() {
    val metricsServer = HTTPServer.Builder()
        .withPort(9000)
        .withDaemonThreads(true)
        .build()

    runBlocking {
        val client: HttpClient = DefaultHttpClient

        val exHandler = CoroutineExceptionHandler { ctx, ex ->
            ex.printStackTrace()
        }

        coroutineScope {
            (1..1).forEach { i ->
                launch(exHandler) {
                    val result = client.get("http://google.com:10")
                    println("${i}:\n$result\n\n")
                }
            }
        }

        client.close()
    }
}
