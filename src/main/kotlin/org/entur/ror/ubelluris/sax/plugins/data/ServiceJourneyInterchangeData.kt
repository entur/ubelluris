package org.entur.ror.ubelluris.sax.plugins.data

/**
 * Represents the content of a ServiceJourneyInterchange element for comparison.
 * Two interchanges are considered identical if all these fields match.
 */
data class ServiceJourneyInterchangeData(
    val id: String,
    val version: String?,
    val priority: String?,
    val guaranteed: String?,
    val advertised: String?,
    val fromPointRef: String?,
    val toPointRef: String?,
    val fromJourneyRef: String?,
    val toJourneyRef: String?,
)
