package org.entur.ror.ubelluris.timetable.model

import org.entur.ror.ubelluris.model.TransportMode

/**
 * Analysis result for a StopPlace determining how to handle TransportMode insertion
 */
data class StopPlaceAnalysis(
    val stopPlaceId: String,
    val scenario: Scenario,
    val quayModes: Map<String, TransportMode>,
    val existingMode: TransportMode?,
    val existingType: String?,
    val hasParent: Boolean,
    val parentRef: String?
)

/**
 * Classification of how to handle TransportMode insertion for a StopPlace
 */
enum class Scenario {
    /**
     * StopPlace has only one quay - update StopPlace mode directly
     */
    SINGLE_QUAY,

    /**
     * StopPlace has multiple quays, all have the same mode - update StopPlace mode
     */
    UNIFORM_MODE,

    /**
     * StopPlace has multiple quays with different modes - split into multiple StopPlaces
     */
    MIXED_MODE
}
