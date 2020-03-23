import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import config.CurrencyConversionConfig
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

typealias ConversionMap = HashMap<Pair<Currency, Currency>, Price>

fun ConversionMap.add(fromCurrency: Currency, toCurrency: Currency, rate: Double) =
    put(fromCurrency to toCurrency, rate of toCurrency)

class CurrencyConverterImpl(private val conversion: Map<Pair<Currency, Currency>, Price>) : CurrencyConverter {
    override fun convert(oldPrice: Price, newCurrency: Currency): Price {
        val oldCurrencyPrice = conversion[oldPrice.currency to newCurrency]
            ?: throw IllegalStateException(
                "Expected conversion map to be exaustive but found ${oldPrice.currency}, " +
                        "$newCurrency pair that is not present"
            )
        return oldCurrencyPrice * oldPrice.ammount
    }
}

class UpToDateCurrencyRates(
    config: CurrencyConversionConfig,
    private val client: HTTPClient
) {
    private val converterRef = AtomicReference<CurrencyConverter>(getConversion(config, client))

    init {
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate({
            val ok = converterRef.compareAndSet(converter, getConversion(config, client))
            if (!ok) throw java.lang.IllegalStateException("Unexpected mutation of current converter")
        }, 1, 1, TimeUnit.HOURS)
    }

    val converter: CurrencyConverter
        get() = converterRef.get()

    private val parser = Klaxon()

    private fun getConversion(
        config: CurrencyConversionConfig,
        client: HTTPClient
    ): CurrencyConverter = client.use {
        val resp = it.getResponse(
            url = "${config.schema}://${config.host}/${config.path}" +
                    "?q=USD_EUR,USD_RUB&compact=ultra&apiKey=${config.key}"
        )
        val jsonResponse = parser.parse<CurrencyConversionResponse>(resp)
            ?: throw IllegalStateException("$resp is not a currency conversion map")

        val resultMap = ConversionMap()

        resultMap.add(Currency.USD, Currency.USD, 1.0)
        resultMap.add(Currency.RUB, Currency.RUB, 1.0)
        resultMap.add(Currency.EUR, Currency.EUR, 1.0)

        resultMap.add(Currency.USD, Currency.EUR, jsonResponse.usdToEur)
        resultMap.add(Currency.USD, Currency.RUB, jsonResponse.usdToRub)

        resultMap.add(Currency.EUR, Currency.USD, 1.0 / jsonResponse.usdToEur)
        resultMap.add(Currency.RUB, Currency.USD, 1.0 / jsonResponse.usdToRub)

        resultMap.add(Currency.EUR, Currency.RUB, jsonResponse.usdToRub / jsonResponse.usdToEur)
        resultMap.add(Currency.RUB, Currency.EUR, jsonResponse.usdToEur / jsonResponse.usdToRub)

        CurrencyConverterImpl(resultMap.toMap())
    }
}


private data class CurrencyConversionResponse(
    @Json(name = "USD_EUR")
    val usdToEur: Double,
    @Json(name = "USD_RUB")
    val usdToRub: Double
)