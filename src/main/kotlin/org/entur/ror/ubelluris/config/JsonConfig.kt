package org.entur.ror.ubelluris.config

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream

class JsonConfig {

    companion object {
        fun loadCliConfig(configFile: InputStream): CliConfig {
            val jsonParser = Json {
                isLenient = true
                ignoreUnknownKeys = true
            }
            return jsonParser.decodeFromStream<CliConfig>(configFile)
        }

    }
}