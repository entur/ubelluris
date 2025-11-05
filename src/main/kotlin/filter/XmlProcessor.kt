package filter

import java.nio.file.Path

interface XmlProcessor {
    fun process(inputFile: Path): Path
}