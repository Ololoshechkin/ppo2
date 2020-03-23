import com.typesafe.config.Config

val Config.port
    get() = this.getInt("port")

val Config.schema
    get() = this.getString("schema")

val Config.host: String
    get() = this.getString("host")

val Config.databaseName: String
    get() = this.getString("databaseName")

val Config.path: String
    get() = this.getString("path")

val Config.key: String
    get() = this.getString("key")

val Config.conversionCfg: Config
    get() = this.getConfig("conversionConfig")

val Config.databaseCfg: Config
    get() = this.getConfig("databaseConfig")

val Config.apiCfg: Config
    get() = this.getConfig("apiConfig")