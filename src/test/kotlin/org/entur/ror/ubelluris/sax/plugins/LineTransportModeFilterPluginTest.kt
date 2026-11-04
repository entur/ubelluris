package org.entur.ror.ubelluris.sax.plugins

import org.assertj.core.api.Assertions.assertThat
import org.entur.netex.tools.lib.NetexProcessor
import org.entur.netex.tools.lib.config.FilterConfig
import org.entur.ror.ubelluris.config.CliConfig
import org.entur.ror.ubelluris.filter.TimetableFilterConfig
import org.entur.ror.ubelluris.model.TransportMode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class LineTransportModeFilterPluginTest {
    @TempDir
    lateinit var tempDir: Path

    private val netex =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <PublicationDelivery xmlns="http://www.netex.org.uk/netex" version="1.0">
            <PublicationTimestamp>2026-08-24T10:00:00</PublicationTimestamp>
            <dataObjects>
                <CompositeFrame id="TEST:CompositeFrame:1" version="1">
                    <frames>
                        <ServiceFrame id="TEST:ServiceFrame:1" version="1">
                            <routes>
                                <Route id="TEST:Route:TAXI" version="1">
                                    <LineRef ref="TEST:Line:TAXI"/>
                                </Route>
                                <Route id="TEST:Route:BUS" version="1">
                                    <LineRef ref="TEST:Line:BUS"/>
                                </Route>
                            </routes>
                            <lines>
                                <Line id="TEST:Line:TAXI" version="1">
                                    <Name>Taxi line</Name>
                                    <TransportMode>taxi</TransportMode>
                                    <PublicCode>T1</PublicCode>
                                </Line>
                                <Line id="TEST:Line:BUS" version="1">
                                    <Name>Bus line</Name>
                                    <TransportMode>bus</TransportMode>
                                    <PublicCode>B1</PublicCode>
                                </Line>
                                <Line id="TEST:Line:WATER" version="1">
                                    <Name>Water line</Name>
                                    <TransportMode>water</TransportMode>
                                    <PublicCode>W1</PublicCode>
                                </Line>
                                <Line id="TEST:Line:NO_MODE" version="1">
                                    <Name>Line without mode</Name>
                                    <PublicCode>N1</PublicCode>
                                </Line>
                            </lines>
                        </ServiceFrame>
                    </frames>
                </CompositeFrame>
            </dataObjects>
        </PublicationDelivery>
        """.trimIndent()

    private fun run(filterConfig: FilterConfig): String {
        val inputDir = Files.createDirectories(tempDir.resolve("input")).toFile()
        val outputDir = Files.createDirectories(tempDir.resolve("output")).toFile()
        File(inputDir, "lines.xml").writeText(netex)

        NetexProcessor(filterConfig = filterConfig).run(inputDir, outputDir)

        return requireNotNull(outputDir.listFiles()?.firstOrNull()) { "No output file was created" }.readText()
    }

    @Test
    fun shouldRemoveTaxiLineAndItsRoutesFromOutput() {
        val timetableFilterConfig =
            TimetableFilterConfig(
                CliConfig(
                    sourceCodespace = "TEST",
                    targetCodespace = "TEST",
                    transportModes = listOf(TransportMode.TRAM),
                ),
            )

        val result = run(timetableFilterConfig.build())

        assertThat(timetableFilterConfig.lineFilteringRepository.lineIdsToRemove).containsExactly("TEST:Line:TAXI")

        assertThat(result).doesNotContain("TEST:Line:TAXI")
        assertThat(result).doesNotContain("TEST:Route:TAXI")
        assertThat(result).contains("TEST:Line:BUS")
        assertThat(result).contains("TEST:Route:BUS")
        assertThat(result).contains("TEST:Line:WATER")
        assertThat(result).contains("TEST:Line:NO_MODE")
    }
}
