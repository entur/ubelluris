package org.entur.ror.ubelluris.sax.plugins.data

/**
 * Identifies a StopPointInJourneyPattern. NeTEx requires id + version + order to be unique within a document;
 * the JourneyPattern id is included as an extra safeguard.
 */
data class StopPointInJourneyPatternKey(
    val journeyPatternId: String,
    val id: String,
    val version: String?,
    val order: String?,
)

data class StopPointInJourneyPatternData(
    val key: StopPointInJourneyPatternKey,
    val sortOrder: Int,
)
