package org.entur.ror.ubelluris.sax.enrichment

import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.model.Scenario
import org.entur.ror.ubelluris.model.StopPlaceAnalysis
import org.entur.ror.ubelluris.model.TransportMode
import org.entur.ror.ubelluris.processor.StopPlaceTypeNormalizer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class TransportModeToStopPlaceTypeEndToEndTest {
    private val transportModeInserter = TransportModeInserter(StopPlaceSplitter())
    private val stopPlaceTypeNormalizer = StopPlaceTypeNormalizer()

    @Test
    fun shouldNormalizeStopPlaceTypeWhenTransportModeBecomesWater(
        @TempDir tempDir: Path,
    ) {
        val xmlFile = tempDir.resolve("stop_places.xml")

        Files.writeString(
            xmlFile,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
              <stopPlaces>
                <StopPlace id="SAM:StopPlace:9000">
                  <TransportMode>bus</TransportMode>
                  <StopPlaceType>onstreetBus</StopPlaceType>
                  <quays>
                    <Quay id="SAM:Quay:90001"/>
                    <Quay id="SAM:Quay:90002"/>
                  </quays>
                </StopPlace>
              </stopPlaces>
            </PublicationDelivery>
            """.trimIndent(),
        )

        val analyses =
            listOf(
                StopPlaceAnalysis(
                    stopPlaceId = "SAM:StopPlace:9000",
                    scenario = Scenario.UNIFORM_MODE,
                    quayModes =
                        mapOf(
                            "SAM:Quay:90001" to TransportMode.WATER,
                            "SAM:Quay:90002" to TransportMode.WATER,
                        ),
                    existingMode = TransportMode.BUS,
                    existingType = "onstreetBus",
                    hasParent = false,
                    parentRef = null,
                ),
            )

        transportModeInserter.insert(xmlFile, analyses)

        val afterInsert = Files.readString(xmlFile)
        assertThat(afterInsert).contains("<TransportMode>water</TransportMode>")
        assertThat(afterInsert).doesNotContain("<TransportMode>bus</TransportMode>")
        assertThat(afterInsert).contains("<StopPlaceType>onstreetBus</StopPlaceType>")

        stopPlaceTypeNormalizer.process(xmlFile.toFile())

        val afterNormalize = Files.readString(xmlFile)
        assertThat(afterNormalize).contains("<TransportMode>water</TransportMode>")

        assertThat(afterNormalize).contains("<StopPlaceType>ferryStop</StopPlaceType>")
        assertThat(afterNormalize).doesNotContain("<StopPlaceType>onstreetBus</StopPlaceType>")
    }

    @Test
    fun shouldNormalizeStopPlaceTypeWhenTransportModeBecomesTram(
        @TempDir tempDir: Path,
    ) {
        val xmlFile = tempDir.resolve("stop_places.xml")

        Files.writeString(
            xmlFile,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
              <stopPlaces>
                <StopPlace id="SAM:StopPlace:9100">
                  <TransportMode>bus</TransportMode>
                  <StopPlaceType>onstreetBus</StopPlaceType>
                  <quays>
                    <Quay id="SAM:Quay:91001"/>
                    <Quay id="SAM:Quay:91002"/>
                  </quays>
                </StopPlace>
              </stopPlaces>
            </PublicationDelivery>
            """.trimIndent(),
        )

        val analyses =
            listOf(
                StopPlaceAnalysis(
                    stopPlaceId = "SAM:StopPlace:9100",
                    scenario = Scenario.UNIFORM_MODE,
                    quayModes =
                        mapOf(
                            "SAM:Quay:91001" to TransportMode.TRAM,
                            "SAM:Quay:91002" to TransportMode.TRAM,
                        ),
                    existingMode = TransportMode.BUS,
                    existingType = "onstreetBus",
                    hasParent = false,
                    parentRef = null,
                ),
            )

        transportModeInserter.insert(xmlFile, analyses)

        val afterInsert = Files.readString(xmlFile)
        assertThat(afterInsert).contains("<TransportMode>tram</TransportMode>")
        assertThat(afterInsert).doesNotContain("<TransportMode>bus</TransportMode>")
        assertThat(afterInsert).contains("<StopPlaceType>onstreetBus</StopPlaceType>")

        stopPlaceTypeNormalizer.process(xmlFile.toFile())

        val afterNormalize = Files.readString(xmlFile)
        assertThat(afterNormalize).contains("<TransportMode>tram</TransportMode>")

        assertThat(afterNormalize).contains("<StopPlaceType>onstreetTram</StopPlaceType>")
        assertThat(afterNormalize).doesNotContain("<StopPlaceType>onstreetBus</StopPlaceType>")
    }
}
