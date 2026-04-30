package org.entur.ror.ubelluris.timetable.model

import java.nio.file.Path

data class TimetableProccessingResult(
    var quayModeMapping: QuayModeMapping,
    var providerResultsPaths: Map<String, Path>,
)