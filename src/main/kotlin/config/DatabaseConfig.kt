package config

import com.typesafe.config.Config

interface DatabaseConfig {
    val schema: String
    val host: String
    val port: Int
    val databaseName: String
}

class DatabaseConfigImpl(conf: Config) : DatabaseConfig {
    override val schema: String = conf.getString("schema")
    override val host: String = conf.getString("host")
    override val port: Int = conf.getInt("port")
    override val databaseName: String = conf.getString("databaseName")
}
