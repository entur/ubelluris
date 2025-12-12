package org.entur.ror.ubelluris.timetable.fetch

import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.model.TransportMode
import org.entur.ror.ubelluris.timetable.config.TimetableConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class HttpTimetableFetcherTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun shouldFetchRelevantTimetableDataForMultipleProviders() {
        val cacheDir = tempDir.resolve("cache")
        val helperDir = tempDir.resolve("helper")

        val config = TimetableConfig(
            apiUrl = "http://unused",
            apiKey = "unused",
            providers = listOf("provider1", "provider2"),
            modeFilter = setOf(TransportMode.TRAM, TransportMode.WATER),
            blacklist = emptyMap(),
            cacheDir = cacheDir,
            helperDir = helperDir
        )

        Files.createDirectories(cacheDir)

        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

        createZip(
            cacheDir.resolve("${today}_provider1.zip"),
            mapOf(
                "line_p1_001.xml" to """
                <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                    <Line>
                        <TransportMode>tram</TransportMode>
                    </Line>
                </PublicationDelivery>
            """.trimIndent(),
                "line_p1_002.xml" to """
                <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                    <Line>
                        <TransportMode>bus</TransportMode>
                    </Line>
                </PublicationDelivery>
            """.trimIndent()
            )
        )

        createZip(
            cacheDir.resolve("${today}_provider2.zip"),
            mapOf(
                "line_p2_001.xml" to """
                <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                    <Line>
                        <TransportMode>tram</TransportMode>
                    </Line>
                </PublicationDelivery>
            """.trimIndent(),
                "line_p2_002.xml" to """
                <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                    <Line>
                        <TransportMode>water</TransportMode>
                    </Line>
                </PublicationDelivery>
            """.trimIndent()
            )
        )

        val fetcher = HttpTimetableFetcher(config)

        val result = fetcher.fetch(listOf("provider1", "provider2"))

        val provider1Data = result["provider1"]!!
        assertThat(provider1Data.allFiles).hasSize(2)
        assertThat(provider1Data.modeHelperFiles).hasSize(1)
        assertThat(provider1Data.modeHelperFiles.first().fileName.toString())
            .isEqualTo("line_p1_001.xml")

        val provider2Data = result["provider2"]!!
        assertThat(provider2Data.allFiles).hasSize(2)
        assertThat(provider2Data.modeHelperFiles).hasSize(2)
        assertThat(provider2Data.modeHelperFiles)
            .extracting { it.fileName.toString() }
            .containsExactlyInAnyOrder("line_p2_001.xml", "line_p2_002.xml")
    }

    private fun createZip(
        zipPath: Path,
        files: Map<String, String>
    ) {
        ZipOutputStream(Files.newOutputStream(zipPath)).use { zip ->
            files.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
    }

}