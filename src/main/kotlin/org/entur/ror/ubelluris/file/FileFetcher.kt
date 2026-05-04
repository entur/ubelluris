package org.entur.ror.ubelluris.file

import java.nio.file.Path

data class FileFetchResult(
    val stopPlacePath: Path,
    val timetablePaths: Map<String, Path>,
)

interface FileFetcher {
    fun fetch(): FileFetchResult
}
