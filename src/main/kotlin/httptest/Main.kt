package httptest

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


fun main() = runBlocking {
    val client: HttpClient = DefaultHttpClient

    coroutineScope {
        (1..10).forEach { i ->
            launch {
                val result = client.get("http://google.com")
                println("${i}:\n$result\n\n")
            }
        }
    }

    client.close()
}
