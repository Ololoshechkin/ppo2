package reactive

import com.typesafe.config.Config

val Config.port
    get() = this.getInt("reactive.getPort")

val Config.schema
    get() = this.getString("reactive.getSchema")

val Config.host: String
    get() = this.getString("reactive.getHost")

val Config.databaseName: String
    get() = this.getString("reactive.getDatabaseName")

val Config.path: String
    get() = this.getString("path")

val Config.key: String
    get() = this.getString("reactive.getKey")

val Config.conversionCfg: Config
    get() = this.getConfig("conversionConfig")

val Config.databaseCfg: Config
    get() = this.getConfig("databaseConfig")

val Config.apiCfg: Config
    get() = this.getConfig("apiConfig")