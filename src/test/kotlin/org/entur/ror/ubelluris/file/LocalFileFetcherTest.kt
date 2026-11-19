package org.entur.ror.ubelluris.file

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path

class LocalFileFetcherTest {
    private val downloadDir = Path.of("downloads")

    @Test
    fun shouldReturnDirectoryOfSalesZipAsSalesPath() {
        val localFileFetcher =
            LocalFileFetcher(
                stopPlaceBlobPath = "stops/stops.zip",
                timetableBlobPaths = emptyMap(),
                salesBlobPath = "sales/samtrafikensales_latest.zip",
                downloadDir = downloadDir,
            )

        assertThat(localFileFetcher.fetch().salesPath).isEqualTo(downloadDir.resolve("sales"))
    }

    @Test
    fun shouldReturnNoSalesPathWithoutSalesBlobPath() {
        val localFileFetcher = LocalFileFetcher("stops/stops.zip", emptyMap(), salesBlobPath = null, downloadDir = downloadDir)

        assertThat(localFileFetcher.fetch().salesPath).isNull()
    }
}
