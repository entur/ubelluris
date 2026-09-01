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

class VersionRefNormalizerHandlerTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun shouldRewriteVersionRefToVersionForLocalEntities() {
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
            requireNotNull(javaClass.getResourceAsStream("/timetable/incorrect-versionref.xml")) {
                "Test resource not found: /timetable/incorrect-versionref.xml"
            }.readBytes()
        File(inputDir, "incorrect-versionref.xml").writeBytes(inputBytes)

        NetexProcessor(
            filterConfig = filterConfig,
        ).run(inputDir, outputDir)

        val outputFile = outputDir.listFiles()?.firstOrNull()
        requireNotNull(outputFile) { "No output file was created" }

        val result = outputFile.readText()

        // TrainNumberRef should now use 'version' instead of 'versionRef' for local entity
        assertThat(result).contains("<TrainNumberRef ref=\"SE:014:TrainNumber:9011014160200000_171\" version=\"any\"")
        assertThat(result).doesNotContain("versionRef=\"any\"")

        // The TrainNumber entity should still exist in output
        assertThat(result).contains("SE:014:TrainNumber:9011014160200000_171")
        assertThat(result).contains("<ForAdvertisement>171</ForAdvertisement>")

        // The ServiceJourney should still exist
        assertThat(result).contains("SE:014:ServiceJourney:9011091029900000")
    }
}
