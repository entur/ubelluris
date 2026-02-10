package org.entur.ror.ubelluris.timetable.config

import org.entur.ror.ubelluris.model.TransportMode
import java.nio.file.Path
import java.nio.file.Paths

data class TimetableConfig(
    val providers: List<String>,
    val modeFilter: Set<TransportMode>,
    val blacklist: Map<String, List<String>>,
    val helperDir: Path = Paths.get("transport-mode-helpers")
)
