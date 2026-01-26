package org.entur.ror.ubelluris.timetable.enrichment

import org.entur.ror.ubelluris.model.TransportMode
import org.entur.ror.ubelluris.timetable.model.Scenario
import org.entur.ror.ubelluris.timetable.model.StopPlaceAnalysis
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class TransportModeInserterTest {
    private val stopPlaceSplitter = StopPlaceSplitter()

    private val transportModeInserter = TransportModeInserter(stopPlaceSplitter)

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun shouldUpdateStopPlaceDirectlyIfSingleQuayMode() {
        val xmlFile = tempDir.resolve("stop_places.xml")

        Files.writeString(
            xmlFile,
            """
        <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
          <stopPlaces>
            <StopPlace id="SAM:StopPlace:1000">
              <quays>
                <Quay id="SAM:Quay:50001">
                    <PublicCode>*</PublicCode>
                </Quay>
              </quays>
            </StopPlace>
          </stopPlaces>
        </PublicationDelivery>
        """.trimIndent()
        )

        val analyses = listOf(
            StopPlaceAnalysis(
                stopPlaceId = "SAM:StopPlace:1000",
                scenario = Scenario.SINGLE_QUAY,
                quayModes = mapOf("SAM:Quay:50001" to TransportMode.TRAM),
                existingMode = null,
                existingType = null,
                hasParent = false,
                parentRef = null
            )
        )

        transportModeInserter.insert(xmlFile, analyses)

        val updatedXml = Files.readString(xmlFile)
        assert(updatedXml.contains("<TransportMode>tram</TransportMode>"))
        assert(updatedXml.contains("<PublicCode />"))
    }

    @Test
    fun shouldUpdateStopPlaceDirectlyIfUniformQuayMode() {
        val xmlFile = tempDir.resolve("stop_places.xml")

        Files.writeString(
            xmlFile,
            """
        <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
          <stopPlaces>
            <StopPlace id="SAM:StopPlace:2000">
              <quays>
                <Quay id="SAM:Quay:60001">
                </Quay>
                <Quay id="SAM:Quay:60002">
                </Quay>
              </quays>
            </StopPlace>
          </stopPlaces>
        </PublicationDelivery>
        """.trimIndent()
        )

        val analyses = listOf(
            StopPlaceAnalysis(
                stopPlaceId = "SAM:StopPlace:2000",
                scenario = Scenario.UNIFORM_MODE,
                quayModes = mapOf(
                    "SAM:Quay:60001" to TransportMode.WATER,
                    "SAM:Quay:60002" to TransportMode.WATER
                ),
                existingMode = null,
                existingType = null,
                hasParent = false,
                parentRef = null
            )
        )

        transportModeInserter.insert(xmlFile, analyses)

        val updatedXml = Files.readString(xmlFile)
        assert(updatedXml.contains("<TransportMode>water</TransportMode>"))
    }

    @Test
    fun shouldSplitStopPlaceIfMixedQuayMode() {
        val xmlFile = tempDir.resolve("stop_places.xml")

        Files.writeString(
            xmlFile,
            """
        <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
          <stopPlaces>
            <StopPlace id="SAM:StopPlace:1000">
              <quays>
                <Quay id="SAM:Quay:50001">
                  <PublicCode>*</PublicCode>
                </Quay>
                <Quay id="SAM:Quay:50002">
                </Quay>
              </quays>
            </StopPlace>
          </stopPlaces>
        </PublicationDelivery>
        """.trimIndent()
        )

        val analyses = listOf(
            StopPlaceAnalysis(
                stopPlaceId = "SAM:StopPlace:1000",
                scenario = Scenario.MIXED_MODE,
                quayModes = mapOf(
                    "SAM:Quay:50001" to TransportMode.TRAM,
                    "SAM:Quay:50002" to TransportMode.BUS
                ),
                existingMode = null,
                existingType = null,
                hasParent = false,
                parentRef = null
            )
        )

        transportModeInserter.insert(xmlFile, analyses)

        val updatedXml = Files.readString(xmlFile)

        assert(updatedXml.contains("_1000_parent"))
        assert(updatedXml.contains("_1000_tram"))
        assert(updatedXml.contains("_1000_bus"))

        assert(updatedXml.contains("<TransportMode>tram</TransportMode>"))
        assert(updatedXml.contains("<TransportMode>bus</TransportMode>"))

        assert(updatedXml.contains("SAM:Quay:50001"))
        assert(updatedXml.contains("SAM:Quay:50002"))

        assert(updatedXml.contains("<ParentSiteRef"))
    }

    @Test
    fun shouldOverwriteExistingTransportMode() {
        val xmlFile = tempDir.resolve("stop_places.xml")

        Files.writeString(
            xmlFile,
            """
        <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
          <stopPlaces>
            <StopPlace id="SAM:StopPlace:3000">
              <TransportMode>bus</TransportMode>
              <quays>
                <Quay id="SAM:Quay:70001"/>
              </quays>
            </StopPlace>
          </stopPlaces>
        </PublicationDelivery>
        """.trimIndent()
        )

        val analyses = listOf(
            StopPlaceAnalysis(
                stopPlaceId = "SAM:StopPlace:3000",
                scenario = Scenario.SINGLE_QUAY,
                quayModes = mapOf("SAM:Quay:70001" to TransportMode.TRAM),
                existingMode = TransportMode.BUS,
                existingType = null,
                hasParent = false,
                parentRef = null
            )
        )

        transportModeInserter.insert(xmlFile, analyses)

        val updatedXml = Files.readString(xmlFile)
        assert(updatedXml.contains("<TransportMode>tram</TransportMode>"))
        assert(!updatedXml.contains("<TransportMode>bus</TransportMode>"))
    }

    @Test
    fun shouldHandleMixedQuayModeWithExistingParent() {
        val xmlFile = tempDir.resolve("stop_places.xml")

        Files.writeString(
            xmlFile,
            """
        <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
          <stopPlaces>
            <StopPlace id="SAM:StopPlace:PARENT">
              <Name>Parent Stop</Name>
            </StopPlace>

            <StopPlace id="SAM:StopPlace:4000">
              <ParentSiteRef ref="SAM:StopPlace:PARENT"/>
              <quays>
                <Quay id="SAM:Quay:80001"/>
                <Quay id="SAM:Quay:80002"/>
              </quays>
            </StopPlace>
          </stopPlaces>
        </PublicationDelivery>
        """.trimIndent()
        )

        val analyses = listOf(
            StopPlaceAnalysis(
                stopPlaceId = "SAM:StopPlace:4000",
                scenario = Scenario.MIXED_MODE,
                quayModes = mapOf(
                    "SAM:Quay:80001" to TransportMode.TRAM,
                    "SAM:Quay:80002" to TransportMode.BUS
                ),
                existingMode = null,
                existingType = null,
                hasParent = true,
                parentRef = "SAM:StopPlace:PARENT"
            )
        )

        transportModeInserter.insert(xmlFile, analyses)

        val updatedXml = Files.readString(xmlFile)

        assert(updatedXml.contains("_4000_tram"))
        assert(updatedXml.contains("_4000_bus"))

        assert(!updatedXml.contains("_4000_parent"))
        assert(updatedXml.contains("""ParentSiteRef ref="SAM:StopPlace:PARENT""""))
    }

}