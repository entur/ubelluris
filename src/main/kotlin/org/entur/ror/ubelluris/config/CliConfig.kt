package org.entur.ror.ubelluris.config

import kotlinx.serialization.Serializable
import org.entur.ror.ubelluris.model.TransportMode

@Serializable
data class CliConfig(
    var sourceCodespace: String,
    var targetCodespace: String,
    var timetableProviders: List<String> = listOf("vt", "halland", "skane"),
    var transportModes: List<TransportMode> = listOf(TransportMode.TRAM, TransportMode.WATER),
    val illegalPublicCodes: List<String> = listOf("*", "-")
)
