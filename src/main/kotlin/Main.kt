import com.mongodb.rx.client.MongoClients
import com.typesafe.config.ConfigFactory
import config.ApplicationConfigImpl
import io.reactivex.netty.protocol.http.server.HttpServer
import org.apache.http.impl.client.HttpClients
import java.io.File

fun main() {
    val config = ConfigFactory.parseFile(File("src/main/resources/application.conf"))
    val applicationConfig = ApplicationConfigImpl(config)
    val databaseConfig = applicationConfig.databaseConfig

    val dao = ReactiveDatabase(
        MongoClients
            .create("${databaseConfig.schema}://${databaseConfig.host}:${databaseConfig.port}")
            .getDatabase(databaseConfig.databaseName),
        UpToDateCurrencyRates(
            applicationConfig.currencyConversionConfig,
            HTTPClientImpl(HttpClients.createDefault())
        )
    )
    HttpServer
        .newServer(applicationConfig.apiConfig.port)
        .start { request, response ->
            response.writeString(makeRequest(request).perform(dao).map { "$it\n" })
        }
        .awaitShutdown()
}
