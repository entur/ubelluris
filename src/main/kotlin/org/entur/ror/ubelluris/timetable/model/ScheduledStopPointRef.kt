package org.entur.ror.ubelluris.timetable.model

import org.entur.ror.ubelluris.model.TransportMode

/**
 * Represents a reference to a ScheduledStopPoint extracted from timetable data
 */
data class ScheduledStopPointRef(
    val provider: String,
    val originalRef: String,
    val transportMode: TransportMode
) {
    fun toCodespaceFormat(): String? {
        val parts = originalRef.split(":")
        if (parts.size >= 4) {
            val codespace = parts[1].trimStart('0').ifEmpty { "0" }
            val id = parts[3]
            return "$codespace:$id"
        }
        return null
    }
}
