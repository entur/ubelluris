package org.entur.ror.ubelluris.timetable.model

import java.nio.file.Path

/**
 * Represents fetched and filtered timetable data for a provider
 */
data class TimetableData(
    val provider: String,
    val modeHelperFiles: List<Path>,
    val allFiles: List<Path>
)
