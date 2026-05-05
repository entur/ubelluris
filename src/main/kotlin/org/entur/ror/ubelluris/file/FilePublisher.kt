package org.entur.ror.ubelluris.file

import java.nio.file.Path

interface FilePublisher {
    fun publish(
        stopPlacePath: Path,
        timetablePaths: Map<String, Path>,
    ): Path

    companion object {
        const val STOPS_DIR = "stops"
        const val TIMETABLE_DIR = "timetable"
    }
}
