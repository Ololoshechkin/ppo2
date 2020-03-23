import java.lang.IllegalStateException

enum class Currency {
    RUB, EUR, USD;
}

fun String.toCurrency() = this.toUpperCase().let {
    when (it) {
        in Currency.values().map(Currency::toString) -> Currency.valueOf(it)
        else -> null
    }
}

fun String.toCurrencyOrThrow() = this.toCurrency() ?: throw IllegalStateException("$this is not a currency")

class Price(val ammount: Double, val currency: Currency) {
    operator fun times(value: Double) = Price(ammount * value, currency)
}

infix fun Double.of(currency: Currency) = Price(this, currency)

interface CurrencyConverter {
    fun convert(price: Price, newCurrency: Currency): Price
}