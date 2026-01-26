package org.entur.ror.ubelluris.sax.plugins

data class StopPlacePurgingParsingContext(
    var publicCode: String? = null,
    var currentQuayId: String? = null,
    var quayHasPublicCode: Boolean = false
)