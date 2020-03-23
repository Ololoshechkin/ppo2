import com.mongodb.rx.client.MongoClients
import com.typesafe.config.ConfigFactory
import io.reactivex.netty.protocol.http.server.HttpServer
import org.apache.http.impl.client.HttpClients
import java.io.File

fun main() {
    val config = ConfigFactory.parseFile(File("src/main/resources/application.conf"))

    val dao = ReactiveDatabase(
        MongoClients
            .create("${config.databaseCfg.schema}://${config.databaseCfg.host}:${config.databaseCfg.port}")
            .getDatabase(config.databaseCfg.databaseName),
        UpToDateCurrencyRates(
            config.conversionCfg,
            HTTPClientImpl(HttpClients.createDefault())
        )
    )
    HttpServer
        .newServer(config.apiCfg.port)
        .start { request, response ->
            response.writeString(makeRequest(request).perform(dao).map { "$it\n" })
        }
        .awaitShutdown()
}
