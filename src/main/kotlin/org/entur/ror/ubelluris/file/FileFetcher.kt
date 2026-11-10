package org.entur.ror.ubelluris.file

import java.nio.file.Path

data class FileFetchResult(
    val stopPlacePath: Path,
    val timetablePaths: Map<String, Path>,
    val salesPath: Path?, // null when sales data is not fetched
)

interface FileFetcher {
    fun fetch(): FileFetchResult
}
