package dev.meteo.weather

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList

/** Minimal local HTTP server; lets the Open-Meteo client be tested without the network. */
class LocalHttpServer(
    private val body: String,
    private val status: Int = 200,
) : AutoCloseable {

    private val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)

    /** Raw query strings of every request, in order. */
    val queries = CopyOnWriteArrayList<String>()

    /** Per-request header values, in order. */
    val userAgents = CopyOnWriteArrayList<String>()
    val accepts = CopyOnWriteArrayList<String>()

    val baseUrl: String get() = "http://127.0.0.1:${server.address.port}"

    val requestCount: Int get() = queries.size

    init {
        server.createContext("/") { exchange ->
            queries += exchange.requestURI.rawQuery ?: ""
            userAgents += exchange.requestHeaders.getFirst("User-Agent") ?: ""
            accepts += exchange.requestHeaders.getFirst("Accept") ?: ""
            val bytes = body.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.executor = null
        server.start()
    }

    override fun close() {
        server.stop(0)
    }
}
