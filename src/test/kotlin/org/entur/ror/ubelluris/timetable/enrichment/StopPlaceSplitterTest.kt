package org.entur.ror.ubelluris.timetable.enrichment

import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.model.TransportMode
import org.entur.ror.ubelluris.timetable.model.Scenario
import org.entur.ror.ubelluris.timetable.model.StopPlaceAnalysis
import org.jdom2.filter.Filters
import org.jdom2.input.SAXBuilder
import org.junit.jupiter.api.Test
import java.io.StringReader

class StopPlaceSplitterTest {

    private val stopPlaceSplitter = StopPlaceSplitter()

    @Test
    fun shouldSplitMixModeStopPlace() {
        val xml = """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
              <stopPlaces>
                <StopPlace id="SAM:StopPlace:1000">
                  <quays>
                    <Quay id="SAM:Quay:50001">
                      <PublicCode>*</PublicCode>
                    </Quay>
                    <Quay id="SAM:Quay:50002"/>
                  </quays>
                </StopPlace>
              </stopPlaces>
            </PublicationDelivery>
        """.trimIndent()

        val document = SAXBuilder().build(StringReader(xml))
        val analysis = StopPlaceAnalysis(
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

        stopPlaceSplitter.split(document, listOf(analysis))

        val root = document.rootElement
        val ns = root.namespace
        val stopPlaces = root.getChild("stopPlaces", ns)!!
            .getChildren("StopPlace", ns)

        assertThat(stopPlaces).hasSize(4)

        val parent = stopPlaces.find { it.getAttributeValue("id")!!.endsWith("_parent") }
        val tramChild = stopPlaces.find { it.getAttributeValue("id")!!.endsWith("_tram") }
        val busChild = stopPlaces.find { it.getAttributeValue("id")!!.endsWith("_bus") }

        assertThat(parent).isNotNull
        assertThat(tramChild).isNotNull
        assertThat(busChild).isNotNull

        // children have correct TransportMode
        assertThat(tramChild!!.getChildText("TransportMode", ns)).isEqualTo("tram")
        assertThat(busChild!!.getChildText("TransportMode", ns)).isEqualTo("bus")

        // children reference parent
        assertThat(tramChild.getChild("ParentSiteRef", ns)?.getAttributeValue("ref"))
            .isEqualTo(parent!!.getAttributeValue("id"))

        assertThat(busChild.getChild("ParentSiteRef", ns)?.getAttributeValue("ref"))
            .isEqualTo(parent.getAttributeValue("id"))

        val tramQuays = tramChild.getChild("quays", ns)!!
            .getChildren("Quay", ns)
        val busQuays = busChild.getChild("quays", ns)!!
            .getChildren("Quay", ns)

        assertThat(tramQuays)
            .hasSize(1)
            .extracting { it.getAttributeValue("id") }
            .containsOnly("SAM:Quay:50001")

        assertThat(busQuays)
            .hasSize(1)
            .extracting { it.getAttributeValue("id") }
            .containsOnly("SAM:Quay:50002")
    }

    @Test
    fun shouldReuseExistingParentWhenStopPlaceAlreadyHasParent() {
        val xml = """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
              <stopPlaces>
                <StopPlace id="SAM:StopPlace:PARENT">
                  <Name>Existing Parent</Name>
                </StopPlace>

                <StopPlace id="SAM:StopPlace:1000">
                  <ParentSiteRef ref="SAM:StopPlace:PARENT"/>
                  <quays>
                    <Quay id="SAM:Quay:50001"/>
                    <Quay id="SAM:Quay:50002"/>
                  </quays>
                </StopPlace>
              </stopPlaces>
            </PublicationDelivery>
        """.trimIndent()

        val document = SAXBuilder().build(StringReader(xml))

        val analysis = StopPlaceAnalysis(
            stopPlaceId = "SAM:StopPlace:1000",
            scenario = Scenario.MIXED_MODE,
            quayModes = mapOf(
                "SAM:Quay:50001" to TransportMode.TRAM,
                "SAM:Quay:50002" to TransportMode.BUS
            ),
            existingMode = null,
            existingType = null,
            hasParent = true,
            parentRef = "SAM:StopPlace:PARENT"
        )

        stopPlaceSplitter.split(document, listOf(analysis))

        val root = document.rootElement
        val ns = root.namespace
        val stopPlaces = root.getChild("stopPlaces", ns)!!
            .getChildren("StopPlace", ns)

        // children created
        val tramChild = stopPlaces.find { it.getAttributeValue("id")!!.endsWith("_tram") }
        val busChild = stopPlaces.find { it.getAttributeValue("id")!!.endsWith("_bus") }

        assertThat(tramChild).isNotNull
        assertThat(busChild).isNotNull

        // no generated parent created
        assertThat(stopPlaces.any { it.getAttributeValue("id")!!.endsWith("_parent") })
            .isFalse

        assertThat(tramChild!!.getChild("ParentSiteRef", ns)?.getAttributeValue("ref"))
            .isEqualTo("SAM:StopPlace:PARENT")

        assertThat(busChild!!.getChild("ParentSiteRef", ns)?.getAttributeValue("ref"))
            .isEqualTo("SAM:StopPlace:PARENT")
    }

    @Test
    fun shouldRemoveOriginalQuaysAfterSplit() {
        val xml = """
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

        val document = SAXBuilder().build(StringReader(xml))

        val analysis = StopPlaceAnalysis(
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

        stopPlaceSplitter.split(document, listOf(analysis))

        val root = document.rootElement
        val ns = root.namespace

        val originalStopPlace = root
            .getChild("stopPlaces", ns)!!
            .getChildren("StopPlace", ns)
            .first { it.getAttributeValue("id") == "SAM:StopPlace:1000" }

        val remainingQuays = originalStopPlace
            .getChild("quays", ns)!!
            .getChildren("Quay", ns)

        assertThat(remainingQuays).isEmpty()
    }

    @Test
    fun shouldReturnEmptyResultWhenNoStopPlacesContainerExists() {
        val xml = """
        <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
          <somethingElse>
            <StopPlace id="SAM:StopPlace:1000"/>
          </somethingElse>
        </PublicationDelivery>
    """.trimIndent()

        val document = SAXBuilder().build(StringReader(xml))

        val analysis = StopPlaceAnalysis(
            stopPlaceId = "SAM:StopPlace:1000",
            scenario = Scenario.MIXED_MODE,
            quayModes = mapOf(
                "SAM:Quay:50001" to TransportMode.TRAM
            ),
            existingMode = null,
            existingType = null,
            hasParent = false,
            parentRef = null
        )

        stopPlaceSplitter.split(document, listOf(analysis))

        val root = document.rootElement
        assertThat(root.getDescendants(Filters.element("StopPlace", root.namespace)).toList())
            .hasSize(1)
    }

    @Test
    fun shouldSkipAnalysisWhenStopPlaceIsNotFound() {
        val xml = """
        <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
          <stopPlaces>
            <StopPlace id="SAM:StopPlace:EXISTING">
              <quays>
                <Quay id="SAM:Quay:50001"/>
              </quays>
            </StopPlace>
          </stopPlaces>
        </PublicationDelivery>
    """.trimIndent()

        val document = SAXBuilder().build(StringReader(xml))

        val analysis = StopPlaceAnalysis(
            stopPlaceId = "SAM:StopPlace:MISSING",
            scenario = Scenario.MIXED_MODE,
            quayModes = mapOf(
                "SAM:Quay:50001" to TransportMode.TRAM
            ),
            existingMode = null,
            existingType = null,
            hasParent = false,
            parentRef = null
        )

        stopPlaceSplitter.split(document, listOf(analysis))

        val root = document.rootElement
        val ns = root.namespace
        val stopPlaces = root.getChild("stopPlaces", ns)!!
            .getChildren("StopPlace", ns)

        assertThat(stopPlaces)
            .hasSize(1)
            .extracting { it.getAttributeValue("id") }
            .containsExactly("SAM:StopPlace:EXISTING")
    }

}
