package reactive

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.util.EntityUtils
import java.io.Closeable

interface HTTPClient : Closeable {
    fun getResponse(url: String): String
}


class HTTPClientImpl(private val closeableHttpClient: CloseableHttpClient) : HTTPClient {
    override fun getResponse(url: String): String {
        return closeableHttpClient.execute(HttpGet(url)).use {
            EntityUtils.toString(it.entity)
        }
    }

    override fun close() {
        closeableHttpClient.close()
    }
}