package org.entur.ror.ubelluris.timetable.config

import org.entur.ror.ubelluris.model.TransportMode
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Configuration for timetable fetching and processing
 */
data class TimetableConfig(
    val apiUrl: String,
    val apiKey: String,
    val providers: List<String>,
    val modeFilter: Set<TransportMode>,
    val blacklist: Map<String, List<String>>,
    val cacheDir: Path = Paths.get("downloads"),
    val helperDir: Path = Paths.get("transport-mode-helpers")
) {
}
