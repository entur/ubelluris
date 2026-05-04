package org.entur.ror.ubelluris.file

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class LocalFilePublisherTest {
    private val storagePath = Path.of("2026", "01", "01")

    @TempDir
    lateinit var tempDir: Path

    private val publisher by lazy {
        LocalFilePublisher(storagePath, resultsDir = tempDir.resolve("results"))
    }

    @Test
    fun shouldMoveFilesToResultsDir() {
        val stopPlaceFile = tempDir.resolve(Path.of("stops", "stops.xml"))
        Files.createDirectories(stopPlaceFile.parent)
        Files.writeString(stopPlaceFile, "<StopPlaces/>")

        val providers = listOf("RUT", "ATB", "FLT")
        val timetablePaths =
            providers.associate { provider ->
                val dir = tempDir.resolve(storagePath).resolve("timetable").resolve(provider)
                Files.createDirectories(dir)
                Files.writeString(dir.resolve("${provider}_line_001.xml"), "<Line />")
                provider to dir
            }

        val result = publisher.publish(stopPlaceFile, timetablePaths)

        val expectedFilePlacement = result.resolve(FilePublisher.STOPS_DIR).resolve(stopPlaceFile.fileName)
        assertThat(expectedFilePlacement).hasContent("<StopPlaces/>")

        val timetableDir = result.resolve(storagePath).resolve("timetable")
        providers.forEach { provider ->
            val expectedProviderFilePlacement = timetableDir.resolve(provider).resolve("${provider}_line_001.xml")
            assertThat(expectedProviderFilePlacement).hasContent("<Line />")
        }
    }

    @Test
    fun shouldReturnResultsDir() {
        val stopPlaceFile = tempDir.resolve("stops.xml")
        Files.writeString(stopPlaceFile, "<StopPlaces/>")

        val result = publisher.publish(stopPlaceFile, emptyMap())

        assertThat(result).isEqualTo(tempDir.resolve("results"))
    }
}
