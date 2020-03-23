package config

import com.typesafe.config.Config

interface CurrencyConversionConfig {
    val host: String
    val path: String
    val schema: String
    val key: String
}

class CurrencyConversionConfigImpl(conf: Config) : CurrencyConversionConfig {
    override val host: String = conf.getString("host")
    override val path: String = conf.getString("path")
    override val schema: String = conf.getString("schema")
    override val key: String = conf.getString("key")
}
