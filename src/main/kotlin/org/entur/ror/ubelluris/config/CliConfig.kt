package org.entur.ror.ubelluris.config

import kotlinx.serialization.Serializable

@Serializable
data class CliConfig(
    var stopsDataUrl: String,
    val stopsDataApiKey: String,
    var timetableDataUrl: String,
    var timetableDataApiKey: String,
    var timetableProviders: List<String>
) {

}