package org.entur.ror.ubelluris.sax.handlers

import org.assertj.core.api.Assertions.assertThat
import org.entur.netex.tools.lib.NetexProcessor
import org.entur.ror.ubelluris.config.CliConfig
import org.entur.ror.ubelluris.filter.TimetableFilterConfig
import org.entur.ror.ubelluris.model.TransportMode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class EmptyMembersFilterHandlerTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun shouldRemoveEmptyMembersInGroupOfStopPlaces() {
        val cliConfig =
            CliConfig(
                sourceCodespace = "SE",
                targetCodespace = "SE",
                transportModes = listOf(TransportMode.TRAM),
            )
        val filterConfig = TimetableFilterConfig(cliConfig).build()

        val inputDir = Files.createDirectories(tempDir.resolve("input")).toFile()
        val outputDir = Files.createDirectories(tempDir.resolve("output")).toFile()

        val inputBytes =
            requireNotNull(javaClass.getResourceAsStream("/timetable/_stops-empty-members.xml")) {
                "Test resource not found: /timetable/_stops-empty-members.xml"
            }.readBytes()
        File(inputDir, "_stops.xml").writeBytes(inputBytes)

        NetexProcessor(
            filterConfig = filterConfig,
        ).run(inputDir, outputDir)

        val outputFile = outputDir.listFiles()?.firstOrNull()
        requireNotNull(outputFile) { "No output file was created" }

        val result = outputFile.readText()

        assertThat(result).doesNotContainPattern("<members\\s*/>")
        assertThat(result).doesNotContainPattern("<members>\\s*</members>")
        assertThat(Regex("<members>").findAll(result).count()).isEqualTo(2)
        assertThat(result).containsPattern("<members>\\s*<StopPlaceRef ref=\"SE:001:StopPlace:2\" version=\"1\"")
        assertThat(result).containsPattern("<members>\\s*<TaxiRankRef ref=\"SE:001:TaxiRank:3\" version=\"1\"")
        assertThat(result).contains("SE:001:GroupOfStopPlaces:1")
        assertThat(result).contains("SE:001:GroupOfStopPlaces:2")
        assertThat(result).contains("SE:001:GroupOfStopPlaces:4")
    }
}
