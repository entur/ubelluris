package org.entur.ror.ubelluris.timetable.model

import org.entur.ror.ubelluris.model.TransportMode

/**
 * Result of VT-1 discovery phase: mapping of Quays to their TransportModes
 */
data class QuayModeMapping(
    val quayToModes: Map<String, Set<TransportMode>>,
    val quayToStopPlace: Map<String, String>,
    val issues: List<MatchIssue>
)

/**
 * Represents an issue encountered during quay-to-mode matching
 */
data class MatchIssue(
    val type: IssueType,
    val message: String,
    val scheduledStopPointRef: String? = null,
    val quayIds: List<String>? = null
)

enum class IssueType {
    MISSING_MATCH,      // Timetable ref not found in stops
    AMBIGUOUS_MATCH,    // Multiple quays with same gid
    INVALID_FORMAT      // Malformed reference
}
