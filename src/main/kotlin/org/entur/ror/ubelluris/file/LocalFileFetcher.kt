package org.entur.ror.ubelluris.file

import java.nio.file.Path

class LocalFileFetcher(
    private val stopPlaceBlobPath: String,
    private val timetableBlobPaths: Map<String, String>,
    private val downloadDir: Path = GcsFileFetcher.DEFAULT_DOWNLOAD_DIR,
) : FileFetcher {
    override fun fetch(): FileFetchResult =
        FileFetchResult(
            stopPlacePath = downloadDir.resolve(stopPlaceBlobPath),
            timetablePaths = timetableBlobPaths.mapValues { (_, blobPath) -> downloadDir.resolve(blobPath) },
        )
}
