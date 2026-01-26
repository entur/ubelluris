package org.entur.ror.ubelluris.sax.plugins

data class StopPlacePurgingParsingContext(
    var currentQuayId: String? = null,
    var quayHasPublicCode: Boolean = false
)