package org.entur.ror.ubelluris.config

import kotlinx.serialization.Serializable

@Serializable
data class CliConfig(
    var stopsDataUrl: String,
    var timetableDataUrl: String,
    var sourceCodespace: String,
    var targetCodespace: String,
    var timetableProviders: List<String>,
    val illegalPublicCodes: List<String> = listOf("*", "-")
) {

}