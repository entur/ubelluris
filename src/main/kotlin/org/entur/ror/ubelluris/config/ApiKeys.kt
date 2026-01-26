package org.entur.ror.ubelluris.config

data class ApiKeys(
    val stopsDataApiKey: String,
    val timetableDataApiKey: String
) {
    companion object {
        fun fromEnvironment(): ApiKeys {
            val stopsKey = System.getenv("STOPS_DATA_API_KEY")
                ?: throw IllegalStateException("STOPS_DATA_API_KEY environment variable not set")
            val timetableKey = System.getenv("TIMETABLE_DATA_API_KEY")
                ?: throw IllegalStateException("TIMETABLE_DATA_API_KEY environment variable not set")
            return ApiKeys(stopsKey, timetableKey)
        }
    }
}