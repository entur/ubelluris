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
    /**
     * Converts the original ref (e.g., "SE:014:ScheduledStopPoint:123456")
     * to codespace format (e.g., "014:123456")
     */
    fun toCodespaceFormat(): String? {
        val parts = originalRef.split(":")
        if (parts.size >= 4) {
            val codespace = parts[1]  // "014"
            val id = parts[3]         // "123456"
            return "$codespace:$id"
        }
        return null
    }
}
