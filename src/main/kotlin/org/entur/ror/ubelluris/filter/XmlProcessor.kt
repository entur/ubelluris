package org.entur.ror.ubelluris.filter

import java.nio.file.Path

interface XmlProcessor {
    fun process(inputFile: Path): Path
}