package org.entur.ror.ubelluris.timetable.model

import org.entur.ror.ubelluris.model.TransportMode
import java.nio.file.Path

data class TimetableData(
    val provider: String,
    val providerDir: Path = Path.of(""),
    val modeHelperFiles: List<Path>,
    val allFiles: List<Path>,
    val quayModes: Map<String, Set<TransportMode>> = emptyMap()
)
