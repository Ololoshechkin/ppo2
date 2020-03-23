package reactive

import com.typesafe.config.Config
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

typealias ConversionMap = HashMap<Pair<Currency, Currency>, Price>

fun ConversionMap.add(fromCurrency: Currency, toCurrency: Currency, rate: Double) =
    put(fromCurrency to toCurrency, rate of toCurrency)

class CurrencyConverterImpl(private val conversion: Map<Pair<Currency, Currency>, Price>) :
    CurrencyConverter {
    override fun convert(oldPrice: Price, newCurrency: Currency): Price {
        val oldCurrencyPrice = conversion[oldPrice.currency to newCurrency]
            ?: throw IllegalStateException(
                "Expected conversion map to be exaustive but found ${oldPrice.currency}, " +
                        "$newCurrency pair that is not present"
            )
        return oldCurrencyPrice * oldPrice.ammount
    }
}

@Serializable
data class Conversion(
    val USD_EUR: Double = 1.0,
    val USD_RUB: Double = 1.0,
    val USD_USD: Double = 1.0,
    val EUR_EUR: Double = 1.0,
    val EUR_RUB: Double = 1.0,
    val EUR_USD: Double = 1.0,
    val RUB_EUR: Double = 1.0,
    val RUB_RUB: Double = 1.0,
    val RUB_USD: Double = 1.0
) {
    fun toMap() = hashMapOf(
        "USD_EUR" to USD_EUR,
        "USD_RUB" to USD_RUB,
        "USD_USD" to USD_USD,
        "EUR_EUR" to EUR_EUR,
        "EUR_RUB" to EUR_RUB,
        "EUR_USD" to EUR_USD,
        "RUB_EUR" to RUB_EUR,
        "RUB_RUB" to RUB_RUB,
        "RUB_USD" to RUB_USD
    )
}

interface CurrencyRates {
    val converter: CurrencyConverter
}

class UpToDateCurrencyRates(
    config: Config,
    private val client: HTTPClient
) : CurrencyRates {
    private val converterRef = AtomicReference<CurrencyConverter>(getConversion(config, client))

    init {
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate({
            val ok = converterRef.compareAndSet(converter, getConversion(config, client))
            if (!ok) throw java.lang.IllegalStateException("Unexpected mutation of current converter")
        }, 1, 1, TimeUnit.HOURS)
    }

    override val converter: CurrencyConverter
        get() = converterRef.get()

    private fun getConversion(
        config: Config,
        client: HTTPClient
    ): CurrencyConverter = client.use {
        val resultMap = ConversionMap()

        Currency.values().forEach { fromCurrency ->
            Currency.values().forEach { toCurrency ->
                val conv = "${fromCurrency}_${toCurrency}"

                val resp = it.getResponse(
                    url = "${config.schema}://${config.host}/${config.path}" +
                            "?q=$conv" +
                            "&compact=ultra" +
                            "&apiKey=${config.key}"
                )
                val conversion = Json.parse(Conversion.serializer(), resp)

                resultMap.add(
                    fromCurrency,
                    toCurrency,
                    conversion.toMap()[conv]
                        ?: throw java.lang.IllegalStateException("Expected $conv to be in conversion map")
                )
            }
        }

        CurrencyConverterImpl(resultMap.toMap())
    }
}