package org.entur.ror.ubelluris.config

import kotlinx.serialization.Serializable
import org.entur.ror.ubelluris.model.TransportMode

@Serializable
data class CliConfig(
    var stopsDataUrl: String,
    var timetableDataUrl: String,
    var sourceCodespace: String,
    var targetCodespace: String,
    var timetableProviders: List<String>,
    var transportModes: List<TransportMode>,
    val illegalPublicCodes: List<String> = listOf("*", "-")
) {

}