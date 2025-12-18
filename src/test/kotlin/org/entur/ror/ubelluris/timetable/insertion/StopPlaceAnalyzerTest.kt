package org.entur.ror.ubelluris.timetable.insertion

import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.model.TransportMode
import org.entur.ror.ubelluris.timetable.model.QuayModeMapping
import org.entur.ror.ubelluris.timetable.model.Scenario
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class StopPlaceAnalyzerTest {

    private val stopPlaceAnalyzer = StopPlaceAnalyzer()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun shouldDetectStopPlacesWithSingleQuayMode() {
        val xmlFile = tempDir.resolve("stop_places.xml")

        Files.writeString(
            xmlFile,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
              <stopPlaces>
                <StopPlace id="SAM:StopPlace:1000">
                  <quays>
                    <Quay id="SAM:Quay:50001"/>
                  </quays>
                </StopPlace>
              </stopPlaces>
            </PublicationDelivery>
        """.trimIndent()
        )

        val quayModeMapping = QuayModeMapping(
            quayToModes = mapOf(
                "SAM:Quay:50001" to setOf(TransportMode.TRAM),
            ),
            quayToStopPlace = mapOf(
                "SAM:Quay:50001" to "SAM:StopPlace:1000",
            )
        )

        val result = stopPlaceAnalyzer.analyze(xmlFile, quayModeMapping)

        assertThat(result).hasSize(1)

        val stopPlace1000 = result.first { it.stopPlaceId == "SAM:StopPlace:1000" }
        assertThat(stopPlace1000.scenario).isEqualTo(Scenario.SINGLE_QUAY)
        assertThat(stopPlace1000.quayModes)
            .hasSize(1)
            .containsEntry("SAM:Quay:50001", TransportMode.TRAM)
    }

    @Test
    fun shouldDetectStopPlacesWithUniformQuayMode() {
        val xmlFile = tempDir.resolve("stop_places.xml")

        Files.writeString(
            xmlFile,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
              <stopPlaces>
                <StopPlace id="SAM:StopPlace:2000">
                  <quays>
                    <Quay id="SAM:Quay:60001"/>
                    <Quay id="SAM:Quay:60002"/>
                  </quays>
                </StopPlace>
              </stopPlaces>
            </PublicationDelivery>
        """.trimIndent()
        )

        val quayModeMapping = QuayModeMapping(
            quayToModes = mapOf(
                "SAM:Quay:60001" to setOf(TransportMode.WATER),
                "SAM:Quay:60002" to setOf(TransportMode.WATER)
            ),
            quayToStopPlace = mapOf(
                "SAM:Quay:60001" to "SAM:StopPlace:2000",
                "SAM:Quay:60002" to "SAM:StopPlace:2000"
            )
        )

        val result = stopPlaceAnalyzer.analyze(xmlFile, quayModeMapping)

        assertThat(result).hasSize(1)
        val stopPlace2000 = result.first { it.stopPlaceId == "SAM:StopPlace:2000" }
        assertThat(stopPlace2000.scenario).isEqualTo(Scenario.UNIFORM_MODE)
        assertThat(stopPlace2000.quayModes)
            .hasSize(2)
            .containsEntry("SAM:Quay:60001", TransportMode.WATER)
            .containsEntry("SAM:Quay:60002", TransportMode.WATER)
    }


    @Test
    fun shouldDetectStopPlacesWithMixedQuayMode() {
        val xmlFile = tempDir.resolve("stop_places.xml")

        Files.writeString(
            xmlFile,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
              <stopPlaces>
                <StopPlace id="SAM:StopPlace:1000">
                  <quays>
                    <Quay id="SAM:Quay:50001"/>
                    <Quay id="SAM:Quay:50002"/>
                  </quays>
                </StopPlace>
              </stopPlaces>
            </PublicationDelivery>
        """.trimIndent()
        )

        val quayModeMapping = QuayModeMapping(
            quayToModes = mapOf(
                "SAM:Quay:50001" to setOf(TransportMode.TRAM),
                "SAM:Quay:50002" to setOf(TransportMode.BUS)
            ),
            quayToStopPlace = mapOf(
                "SAM:Quay:50001" to "SAM:StopPlace:1000",
                "SAM:Quay:50002" to "SAM:StopPlace:1000"
            )
        )

        val result = stopPlaceAnalyzer.analyze(xmlFile, quayModeMapping)

        assertThat(result).hasSize(1)

        val stopPlace1000 = result.first { it.stopPlaceId == "SAM:StopPlace:1000" }
        assertThat(stopPlace1000.scenario).isEqualTo(Scenario.MIXED_MODE)
        assertThat(stopPlace1000.quayModes)
            .hasSize(2)
            .containsEntry("SAM:Quay:50001", TransportMode.TRAM)
            .containsEntry("SAM:Quay:50002", TransportMode.BUS)
    }

    @Test
    fun shouldHandleStopPlaceWithParentSiteRef() {
        val xmlFile = tempDir.resolve("stop_places.xml")

        Files.writeString(
            xmlFile,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
              <stopPlaces>
                <StopPlace id="SAM:StopPlace:1000">
                  <ParentSiteRef ref="SAM:Site:999"/>
                  <quays>
                    <Quay id="SAM:Quay:50001"/>
                  </quays>
                </StopPlace>
              </stopPlaces>
            </PublicationDelivery>
        """.trimIndent()
        )

        val quayModeMapping = QuayModeMapping(
            quayToModes = mapOf(
                "SAM:Quay:50001" to setOf(TransportMode.TRAM)
            ),
            quayToStopPlace = mapOf(
                "SAM:Quay:50001" to "SAM:StopPlace:1000"
            )
        )

        val result = stopPlaceAnalyzer.analyze(xmlFile, quayModeMapping)

        assertThat(result).hasSize(1)

        val analysis = result.first()
        assertThat(analysis.stopPlaceId).isEqualTo("SAM:StopPlace:1000")
        assertThat(analysis.hasParent).isEqualTo(true)
        assertThat(analysis.parentRef).isEqualTo("SAM:Site:999")
        assertThat(analysis.scenario).isEqualTo(Scenario.SINGLE_QUAY)
    }

    @Test
    fun shouldHandleStopPlaceWithNoMatchingQuayModes() {
        val xmlFile = tempDir.resolve("stop_places.xml")

        Files.writeString(
            xmlFile,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
              <stopPlaces>
                <StopPlace id="SAM:StopPlace:1000">
                  <quays>
                    <Quay id="SAM:Quay:50001"/>
                    <Quay id="SAM:Quay:50002"/>
                  </quays>
                </StopPlace>
              </stopPlaces>
            </PublicationDelivery>
        """.trimIndent()
        )

        val quayModeMapping = QuayModeMapping(
            quayToModes = emptyMap(),
            quayToStopPlace = emptyMap()
        )

        val result = stopPlaceAnalyzer.analyze(xmlFile, quayModeMapping)

        assertThat(result).isEmpty()
    }

    @Test
    fun shouldHandleStopPlaceWithMissingQuayModes() {
        val xmlFile = tempDir.resolve("stop_places.xml")

        Files.writeString(
            xmlFile,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
              <stopPlaces>
                <StopPlace id="SAM:StopPlace:1000">
                  <quays>
                    <Quay id="SAM:Quay:50001"/>
                    <Quay id="SAM:Quay:50002"/>
                  </quays>
                </StopPlace>
              </stopPlaces>
            </PublicationDelivery>
        """.trimIndent()
        )

        val quayModeMapping = QuayModeMapping(
            quayToModes = mapOf(
                "SAM:Quay:50001" to setOf(TransportMode.TRAM)
            ),
            quayToStopPlace = mapOf(
                "SAM:Quay:50001" to "SAM:StopPlace:1000",
                "SAM:Quay:50002" to "SAM:StopPlace:1000"
            )
        )

        val result = stopPlaceAnalyzer.analyze(xmlFile, quayModeMapping)

        assertThat(result).hasSize(1)

        val stopPlace1000 = result.first()
        assertThat(stopPlace1000.scenario).isEqualTo(Scenario.MIXED_MODE)

        assertThat(stopPlace1000.quayModes)
            .hasSize(1)
            .containsEntry("SAM:Quay:50001", TransportMode.TRAM)
    }

    @Test
    fun shouldHandleStopPlaceWithExistingTransportMode() {
        val xmlFile = tempDir.resolve("stop_places.xml")

        Files.writeString(
            xmlFile,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
              <stopPlaces>
                <StopPlace id="SAM:StopPlace:1000">
                  <TransportMode>bus</TransportMode>
                  <quays>
                    <Quay id="SAM:Quay:50001"/>
                  </quays>
                </StopPlace>
              </stopPlaces>
            </PublicationDelivery>
        """.trimIndent()
        )

        val quayModeMapping = QuayModeMapping(
            quayToModes = mapOf(
                "SAM:Quay:50001" to setOf(TransportMode.TRAM)
            ),
            quayToStopPlace = mapOf(
                "SAM:Quay:50001" to "SAM:StopPlace:1000"
            )
        )

        val result = stopPlaceAnalyzer.analyze(xmlFile, quayModeMapping)

        assertThat(result).hasSize(1)

        val analysis = result.first()
        assertThat(analysis.stopPlaceId).isEqualTo("SAM:StopPlace:1000")
        assertThat(analysis.existingMode).isEqualTo(TransportMode.BUS)
        assertThat(analysis.scenario).isEqualTo(Scenario.SINGLE_QUAY)
        assertThat(analysis.quayModes)
            .hasSize(1)
            .containsEntry("SAM:Quay:50001", TransportMode.TRAM)
    }

    @Test
    fun shouldSkipStopPlacesWithMissingIdAttribute() {
        val xmlFile = tempDir.resolve("stop_places.xml")

        Files.writeString(
            xmlFile,
            """
        <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
          <stopPlaces>
            <StopPlace>
              <quays>
                <Quay id="SAM:Quay:50001"/>
              </quays>
            </StopPlace>
          </stopPlaces>
        </PublicationDelivery>
        """.trimIndent()
        )

        val quayModeMapping = QuayModeMapping(
            quayToModes = mapOf(
                "SAM:Quay:50001" to setOf(TransportMode.TRAM)
            ),
            quayToStopPlace = mapOf(
                "SAM:Quay:50001" to "SAM:StopPlace:1000"
            )
        )

        val result = stopPlaceAnalyzer.analyze(xmlFile, quayModeMapping)

        assertThat(result).isEmpty()
    }

}