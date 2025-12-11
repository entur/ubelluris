package org.entur.ror.ubelluris.timetable.fetch

import org.entur.ror.ubelluris.timetable.model.TimetableData

interface TimetableFetcher {
    fun fetch(providers: List<String>): Map<String, TimetableData>
}
