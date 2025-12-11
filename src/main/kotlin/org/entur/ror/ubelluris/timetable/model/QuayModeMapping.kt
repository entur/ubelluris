package org.entur.ror.ubelluris.timetable.model

import org.entur.ror.ubelluris.model.TransportMode

/**
 * Mapping Quays to their TransportModes
 */
data class QuayModeMapping(
    val quayToModes: Map<String, Set<TransportMode>>,
    val quayToStopPlace: Map<String, String>
)

