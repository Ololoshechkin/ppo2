package config

import com.typesafe.config.Config

interface ApplicationConfig {
    val apiConfig: ApiConfig
    val databaseConfig: DatabaseConfig
    val currencyConversionConfig: CurrencyConversionConfig
}

class ApplicationConfigImpl(conf: Config) : ApplicationConfig {
    override val apiConfig: ApiConfig = ApiConfigImpl(conf.getConfig("api"))
    override val databaseConfig: DatabaseConfig = DatabaseConfigImpl(conf.getConfig("database"))
    override val currencyConversionConfig: CurrencyConversionConfig =
        CurrencyConversionConfigImpl(conf.getConfig("currencyConversionApi"))
}
