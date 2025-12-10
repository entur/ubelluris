package org.entur.ror.ubelluris.timetable.model

import org.entur.ror.ubelluris.model.TransportMode

/**
 * Log entry for TransportMode insertion actions
 */
data class ModeInsertionLog(
    val stopPlaceId: String,
    val action: Action,
    val oldMode: TransportMode?,
    val newMode: TransportMode?,
    val childStopPlaces: List<String>? = null
)

enum class Action {
    UPDATED_SINGLE_QUAY,
    UPDATED_UNIFORM_MODE,
    SPLIT_MIXED_MODE
}
