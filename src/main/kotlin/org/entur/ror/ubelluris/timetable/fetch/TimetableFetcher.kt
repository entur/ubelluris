package org.entur.ror.ubelluris.timetable.fetch

import org.entur.ror.ubelluris.timetable.model.TimetableData

/**
 * Interface for fetching timetable data
 */
interface TimetableFetcher {
    /**
     * Fetches timetable data for the specified providers
     *
     * @param providers List of provider names (e.g., ["vt", "halland", "skane"])
     * @return Map of provider name to TimetableData
     */
    fun fetch(providers: List<String>): Map<String, TimetableData>
}
