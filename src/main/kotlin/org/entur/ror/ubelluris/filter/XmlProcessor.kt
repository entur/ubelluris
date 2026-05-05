package org.entur.ror.ubelluris.filter

import org.entur.ror.ubelluris.model.TimetableData
import java.nio.file.Path

interface XmlProcessor {
    fun process(inputFile: Path, timetableData: Map<String, TimetableData>): Path
}