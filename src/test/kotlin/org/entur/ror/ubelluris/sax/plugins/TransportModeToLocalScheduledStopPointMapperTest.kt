package org.entur.ror.ubelluris.sax.plugins

import org.assertj.core.api.Assertions
import org.entur.netex.tools.lib.NetexProcessor
import org.entur.netex.tools.lib.config.FilterConfigBuilder
import org.entur.ror.ubelluris.model.TransportMode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class TransportModeToLocalScheduledStopPointMapperTest {
    @TempDir
    lateinit var tempDir: Path

    private fun runMapper(
        transportModes: List<TransportMode>,
        vararg resourceFileNames: String,
    ): Map<String, Set<TransportMode>> {
        val plugin = TransportModeToLocalScheduledStopPointMapper(transportModes)
        val filterConfig =
            FilterConfigBuilder()
                .withPlugins(listOf(plugin))
                .build()

        val inputDir = Files.createDirectories(tempDir.resolve("input")).toFile()
        val outputDir = Files.createDirectories(tempDir.resolve("output")).toFile()

        resourceFileNames.forEachIndexed { index, fileName ->
            val bytes =
                requireNotNull(javaClass.getResourceAsStream("/timetable/$fileName")) {
                    "Test resource not found: /timetable/$fileName"
                }.readBytes()
            File(inputDir, "${index}_$fileName").writeBytes(bytes)
        }

        NetexProcessor(
            filterConfig = filterConfig,
        ).run(inputDir, outputDir)

        return plugin.getCollectedData()
    }

    @Test
    fun shouldMapTramStopsToLocalIds() {
        val result = runMapper(listOf(TransportMode.TRAM, TransportMode.WATER), "sample-timetable.xml")

        Assertions.assertThat(result["1:101"]).containsExactly(TransportMode.TRAM)
        Assertions.assertThat(result["1:102"]).containsExactly(TransportMode.TRAM)
    }

    @Test
    fun shouldMapWaterStopsToLocalIds() {
        val result = runMapper(listOf(TransportMode.TRAM, TransportMode.WATER), "sample-timetable.xml")

        Assertions.assertThat(result["1:201"]).containsExactly(TransportMode.WATER)
    }

    @Test
    fun shouldIncludeOnlyConfiguredModes() {
        val result = runMapper(listOf(TransportMode.BUS), "sample-timetable.xml")

        Assertions.assertThat(result["1:301"]).containsExactly(TransportMode.BUS)
        Assertions.assertThat(result).doesNotContainKey("1:101")
        Assertions.assertThat(result).doesNotContainKey("1:201")
    }

    @Test
    fun shouldAccumulateResultsAcrossMultipleFiles() {
        val result =
            runMapper(
                listOf(TransportMode.TRAM, TransportMode.WATER),
                "sample-timetable.xml",
                "sample-timetable.xml",
            )

        Assertions.assertThat(result["1:101"]).containsExactly(TransportMode.TRAM)
        Assertions.assertThat(result["1:201"]).containsExactly(TransportMode.WATER)
    }
}
