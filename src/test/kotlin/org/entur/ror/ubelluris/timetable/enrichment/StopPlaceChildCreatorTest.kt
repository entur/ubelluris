package org.entur.ror.ubelluris.timetable.enrichment

import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.model.TransportMode
import org.jdom2.Namespace
import org.jdom2.input.SAXBuilder
import org.junit.jupiter.api.Test
import java.io.StringReader

class StopPlaceChildCreatorTest {

    private val stopPlaceChildCreator = StopPlaceChildCreator()
    private val namespace = Namespace.getNamespace("http://www.netex.org.uk/netex")

    @Test
    fun shouldCreateChildStopPlaceWithAllElements() {
        val xml = """
            <StopPlace xmlns="http://www.netex.org.uk/netex" id="SAM:StopPlace:1000" version="1">
              <keyList>
                <KeyValue>
                  <Key>owner</Key>
                  <Value>001</Value>
                </KeyValue>
              </keyList>
              <Name>Test Station</Name>
              <Centroid>
                <Location>
                  <Longitude>13.0</Longitude>
                  <Latitude>59.0</Latitude>
                </Location>
              </Centroid>
              <quays>
                <Quay id="SAM:Quay:50001">
                  <PublicCode>1</PublicCode>
                </Quay>
                <Quay id="SAM:Quay:50002">
                  <PublicCode>2</PublicCode>
                </Quay>
              </quays>
            </StopPlace>
        """.trimIndent()

        val originalStopPlace = SAXBuilder().build(StringReader(xml)).rootElement
        val childId = "SAM:StopPlace:1000_tram"
        val parentRef = "SAM:StopPlace:1000_parent"

        val child = stopPlaceChildCreator.createChildStopPlace(
            originalStopPlace = originalStopPlace,
            childId = childId,
            mode = TransportMode.TRAM,
            quayIds = listOf("SAM:Quay:50001"),
            namespace = namespace,
            parentRef = parentRef
        )

        assertThat(child.getAttributeValue("id")).isEqualTo(childId)
        assertThat(child.getAttributeValue("version")).isEqualTo("1")
        assertThat(child.name).isEqualTo("StopPlace")

        assertThat(child.getChildText("TransportMode", namespace)).isEqualTo("tram")

        val parentSiteRef = child.getChild("ParentSiteRef", namespace)
        assertThat(parentSiteRef).isNotNull
        assertThat(parentSiteRef!!.getAttributeValue("ref")).isEqualTo(parentRef)
        assertThat(parentSiteRef.getAttributeValue("version")).isEqualTo("1")

        assertThat(child.getChildText("Name", namespace)).isEqualTo("Test Station")

        val quays = child.getChild("quays", namespace)!!.getChildren("Quay", namespace)
        assertThat(quays)
            .hasSize(1)
            .extracting { it.getAttributeValue("id") }
            .containsExactly("SAM:Quay:50001")
    }

    @Test
    fun shouldSetTransportModeCorrectlyForDifferentModes() {
        val xml = """
            <StopPlace xmlns="http://www.netex.org.uk/netex" id="SAM:StopPlace:1000" version="1">
              <Name>Test Station</Name>
              <quays>
                <Quay id="SAM:Quay:50001"/>
              </quays>
            </StopPlace>
        """.trimIndent()

        val originalStopPlace = SAXBuilder().build(StringReader(xml)).rootElement

        val busChild = stopPlaceChildCreator.createChildStopPlace(
            originalStopPlace = originalStopPlace,
            childId = "SAM:StopPlace:1000_bus",
            mode = TransportMode.BUS,
            quayIds = listOf("SAM:Quay:50001"),
            namespace = namespace,
            parentRef = "SAM:StopPlace:1000_parent"
        )

        val tramChild = stopPlaceChildCreator.createChildStopPlace(
            originalStopPlace = originalStopPlace,
            childId = "SAM:StopPlace:1000_tram",
            mode = TransportMode.TRAM,
            quayIds = listOf("SAM:Quay:50001"),
            namespace = namespace,
            parentRef = "SAM:StopPlace:1000_parent"
        )

        val waterChild = stopPlaceChildCreator.createChildStopPlace(
            originalStopPlace = originalStopPlace,
            childId = "SAM:StopPlace:1000_water",
            mode = TransportMode.WATER,
            quayIds = listOf("SAM:Quay:50001"),
            namespace = namespace,
            parentRef = "SAM:StopPlace:1000_parent"
        )

        assertThat(busChild.getChildText("TransportMode", namespace)).isEqualTo("bus")
        assertThat(tramChild.getChildText("TransportMode", namespace)).isEqualTo("tram")
        assertThat(waterChild.getChildText("TransportMode", namespace)).isEqualTo("water")
    }

    @Test
    fun shouldOverrideExistingTransportMode() {
        val xml = """
            <StopPlace xmlns="http://www.netex.org.uk/netex" id="SAM:StopPlace:1000" version="1">
              <TransportMode>bus</TransportMode>
              <quays>
                <Quay id="SAM:Quay:50001"/>
              </quays>
            </StopPlace>
        """.trimIndent()

        val originalStopPlace = SAXBuilder().build(StringReader(xml)).rootElement

        val child = stopPlaceChildCreator.createChildStopPlace(
            originalStopPlace = originalStopPlace,
            childId = "SAM:StopPlace:1000_tram",
            mode = TransportMode.TRAM,
            quayIds = listOf("SAM:Quay:50001"),
            namespace = namespace,
            parentRef = "SAM:StopPlace:1000_parent"
        )

        assertThat(child.getChildText("TransportMode", namespace)).isEqualTo("tram")
    }

    @Test
    fun shouldAddTransportModeWhenNotPresent() {
        val xml = """
            <StopPlace xmlns="http://www.netex.org.uk/netex" id="SAM:StopPlace:1000" version="1">
              <Name>Test Station</Name>
              <quays>
                <Quay id="SAM:Quay:50001"/>
              </quays>
            </StopPlace>
        """.trimIndent()

        val originalStopPlace = SAXBuilder().build(StringReader(xml)).rootElement

        val child = stopPlaceChildCreator.createChildStopPlace(
            originalStopPlace = originalStopPlace,
            childId = "SAM:StopPlace:1000_tram",
            mode = TransportMode.TRAM,
            quayIds = listOf("SAM:Quay:50001"),
            namespace = namespace,
            parentRef = "SAM:StopPlace:1000_parent"
        )

        val transportMode = child.getChild("TransportMode", namespace)
        assertThat(transportMode).isNotNull
        assertThat(transportMode!!.text).isEqualTo("tram")
    }

    @Test
    fun shouldSetParentSiteRefCorrectly() {
        val xml = """
            <StopPlace xmlns="http://www.netex.org.uk/netex" id="SAM:StopPlace:1000" version="1">
              <quays>
                <Quay id="SAM:Quay:50001"/>
              </quays>
            </StopPlace>
        """.trimIndent()

        val originalStopPlace = SAXBuilder().build(StringReader(xml)).rootElement
        val parentRef = "SAM:StopPlace:1000_parent"

        val child = stopPlaceChildCreator.createChildStopPlace(
            originalStopPlace = originalStopPlace,
            childId = "SAM:StopPlace:1000_tram",
            mode = TransportMode.TRAM,
            quayIds = listOf("SAM:Quay:50001"),
            namespace = namespace,
            parentRef = parentRef
        )

        val parentSiteRef = child.getChild("ParentSiteRef", namespace)
        assertThat(parentSiteRef).isNotNull
        assertThat(parentSiteRef!!.getAttributeValue("ref")).isEqualTo(parentRef)
        assertThat(parentSiteRef.getAttributeValue("version")).isEqualTo("1")
    }

    @Test
    fun shouldOverrideExistingParentSiteRef() {
        val xml = """
            <StopPlace xmlns="http://www.netex.org.uk/netex" id="SAM:StopPlace:1000" version="1">
              <ParentSiteRef ref="SAM:StopPlace:OLD_PARENT" version="1"/>
              <quays>
                <Quay id="SAM:Quay:50001"/>
              </quays>
            </StopPlace>
        """.trimIndent()

        val originalStopPlace = SAXBuilder().build(StringReader(xml)).rootElement
        val newParentRef = "SAM:StopPlace:1000_parent"

        val child = stopPlaceChildCreator.createChildStopPlace(
            originalStopPlace = originalStopPlace,
            childId = "SAM:StopPlace:1000_tram",
            mode = TransportMode.TRAM,
            quayIds = listOf("SAM:Quay:50001"),
            namespace = namespace,
            parentRef = newParentRef
        )

        val parentSiteRef = child.getChild("ParentSiteRef", namespace)
        assertThat(parentSiteRef!!.getAttributeValue("ref")).isEqualTo(newParentRef)
    }

    @Test
    fun shouldFilterQuaysCorrectly() {
        val xml = """
            <StopPlace xmlns="http://www.netex.org.uk/netex" id="SAM:StopPlace:1000" version="1">
              <quays>
                <Quay id="SAM:Quay:50001">
                  <PublicCode>1</PublicCode>
                </Quay>
                <Quay id="SAM:Quay:50002">
                  <PublicCode>2</PublicCode>
                </Quay>
                <Quay id="SAM:Quay:50003">
                  <PublicCode>3</PublicCode>
                </Quay>
              </quays>
            </StopPlace>
        """.trimIndent()

        val originalStopPlace = SAXBuilder().build(StringReader(xml)).rootElement

        val child = stopPlaceChildCreator.createChildStopPlace(
            originalStopPlace = originalStopPlace,
            childId = "SAM:StopPlace:1000_tram",
            mode = TransportMode.TRAM,
            quayIds = listOf("SAM:Quay:50001", "SAM:Quay:50003"),
            namespace = namespace,
            parentRef = "SAM:StopPlace:1000_parent"
        )

        val quays = child.getChild("quays", namespace)!!.getChildren("Quay", namespace)
        assertThat(quays)
            .hasSize(2)
            .extracting { it.getAttributeValue("id") }
            .containsExactlyInAnyOrder("SAM:Quay:50001", "SAM:Quay:50003")
    }

    @Test
    fun shouldClearPublicCodeAsterisk() {
        val xml = """
            <StopPlace xmlns="http://www.netex.org.uk/netex" id="SAM:StopPlace:1000" version="1">
              <quays>
                <Quay id="SAM:Quay:50001">
                  <PublicCode>*</PublicCode>
                </Quay>
                <Quay id="SAM:Quay:50002">
                  <PublicCode>2</PublicCode>
                </Quay>
              </quays>
            </StopPlace>
        """.trimIndent()

        val originalStopPlace = SAXBuilder().build(StringReader(xml)).rootElement

        val child = stopPlaceChildCreator.createChildStopPlace(
            originalStopPlace = originalStopPlace,
            childId = "SAM:StopPlace:1000_tram",
            mode = TransportMode.TRAM,
            quayIds = listOf("SAM:Quay:50001", "SAM:Quay:50002"),
            namespace = namespace,
            parentRef = "SAM:StopPlace:1000_parent"
        )

        val quays = child.getChild("quays", namespace)!!.getChildren("Quay", namespace)
        val quay1 = quays.find { it.getAttributeValue("id") == "SAM:Quay:50001" }
        val quay2 = quays.find { it.getAttributeValue("id") == "SAM:Quay:50002" }

        assertThat(quay1!!.getChildText("PublicCode", namespace)).isEmpty()
        assertThat(quay2!!.getChildText("PublicCode", namespace)).isEqualTo("2")
    }

    @Test
    fun shouldHandleStopPlaceWithNoQuays() {
        val xml = """
            <StopPlace xmlns="http://www.netex.org.uk/netex" id="SAM:StopPlace:1000" version="1">
              <Name>Test Station</Name>
            </StopPlace>
        """.trimIndent()

        val originalStopPlace = SAXBuilder().build(StringReader(xml)).rootElement

        val child = stopPlaceChildCreator.createChildStopPlace(
            originalStopPlace = originalStopPlace,
            childId = "SAM:StopPlace:1000_tram",
            mode = TransportMode.TRAM,
            quayIds = listOf(),
            namespace = namespace,
            parentRef = "SAM:StopPlace:1000_parent"
        )

        assertThat(child.getAttributeValue("id")).isEqualTo("SAM:StopPlace:1000_tram")
        assertThat(child.getChildText("TransportMode", namespace)).isEqualTo("tram")
        assertThat(child.getChild("quays", namespace)).isNull()
    }

    @Test
    fun shouldHandleEmptyQuaysContainer() {
        val xml = """
            <StopPlace xmlns="http://www.netex.org.uk/netex" id="SAM:StopPlace:1000" version="1">
              <Name>Test Station</Name>
              <quays>
              </quays>
            </StopPlace>
        """.trimIndent()

        val originalStopPlace = SAXBuilder().build(StringReader(xml)).rootElement

        val child = stopPlaceChildCreator.createChildStopPlace(
            originalStopPlace = originalStopPlace,
            childId = "SAM:StopPlace:1000_tram",
            mode = TransportMode.TRAM,
            quayIds = listOf(),
            namespace = namespace,
            parentRef = "SAM:StopPlace:1000_parent"
        )

        val quays = child.getChild("quays", namespace)
        assertThat(quays).isNotNull
        assertThat(quays!!.getChildren("Quay", namespace)).isEmpty()
    }

    @Test
    fun shouldPreserveOtherStopPlaceElements() {
        val xml = """
            <StopPlace xmlns="http://www.netex.org.uk/netex" id="SAM:StopPlace:1000" version="2">
              <keyList>
                <KeyValue>
                  <Key>owner</Key>
                  <Value>001</Value>
                </KeyValue>
              </keyList>
              <Name>Test Station</Name>
              <Description>A test description</Description>
              <Centroid>
                <Location>
                  <Longitude>13.0</Longitude>
                  <Latitude>59.0</Latitude>
                </Location>
              </Centroid>
              <quays>
                <Quay id="SAM:Quay:50001"/>
              </quays>
            </StopPlace>
        """.trimIndent()

        val originalStopPlace = SAXBuilder().build(StringReader(xml)).rootElement

        val child = stopPlaceChildCreator.createChildStopPlace(
            originalStopPlace = originalStopPlace,
            childId = "SAM:StopPlace:1000_tram",
            mode = TransportMode.TRAM,
            quayIds = listOf("SAM:Quay:50001"),
            namespace = namespace,
            parentRef = "SAM:StopPlace:1000_parent"
        )

        assertThat(child.getChildText("Name", namespace)).isEqualTo("Test Station")
        assertThat(child.getChildText("Description", namespace)).isEqualTo("A test description")
        assertThat(child.getChild("Centroid", namespace)).isNotNull
        assertThat(child.getChild("keyList", namespace)).isNotNull

        val keyValues = child.getChild("keyList", namespace)!!.getChildren("KeyValue", namespace)
        val ownerKv = keyValues.find { it.getChildText("Key", namespace) == "owner" }
        assertThat(ownerKv!!.getChildText("Value", namespace)).isEqualTo("001")
    }

    @Test
    fun shouldSetStopPlaceTypeBasedOnTransportMode() {
        val xml = """
            <StopPlace xmlns="http://www.netex.org.uk/netex" id="SAM:StopPlace:1000" version="1">
              <TransportMode>bus</TransportMode>
              <StopPlaceType>onstreetBus</StopPlaceType>
              <quays>
                <Quay id="SAM:Quay:50001"/>
              </quays>
            </StopPlace>
        """.trimIndent()

        val originalStopPlace = SAXBuilder().build(StringReader(xml)).rootElement

        val tramChild = stopPlaceChildCreator.createChildStopPlace(
            originalStopPlace = originalStopPlace,
            childId = "SAM:StopPlace:1000_tram",
            mode = TransportMode.TRAM,
            quayIds = listOf("SAM:Quay:50001"),
            namespace = namespace,
            parentRef = "SAM:StopPlace:1000_parent"
        )

        val waterChild = stopPlaceChildCreator.createChildStopPlace(
            originalStopPlace = originalStopPlace,
            childId = "SAM:StopPlace:1000_water",
            mode = TransportMode.WATER,
            quayIds = listOf("SAM:Quay:50001"),
            namespace = namespace,
            parentRef = "SAM:StopPlace:1000_parent"
        )

        val busChild = stopPlaceChildCreator.createChildStopPlace(
            originalStopPlace = originalStopPlace,
            childId = "SAM:StopPlace:1000_bus",
            mode = TransportMode.BUS,
            quayIds = listOf("SAM:Quay:50001"),
            namespace = namespace,
            parentRef = "SAM:StopPlace:1000_parent"
        )

        assertThat(tramChild.getChildText("StopPlaceType", namespace)).isEqualTo("onstreetTram")
        assertThat(waterChild.getChildText("StopPlaceType", namespace)).isEqualTo("ferryStop")
        assertThat(busChild.getChildText("StopPlaceType", namespace)).isEqualTo("onstreetBus")
    }

    @Test
    fun shouldNotModifyOriginalStopPlace() {
        val xml = """
            <StopPlace xmlns="http://www.netex.org.uk/netex" id="SAM:StopPlace:1000" version="1">
              <Name>Original Name</Name>
              <quays>
                <Quay id="SAM:Quay:50001"/>
                <Quay id="SAM:Quay:50002"/>
              </quays>
            </StopPlace>
        """.trimIndent()

        val originalStopPlace = SAXBuilder().build(StringReader(xml)).rootElement
        val originalQuayCount = originalStopPlace.getChild("quays", namespace)!!
            .getChildren("Quay", namespace).size

        val child = stopPlaceChildCreator.createChildStopPlace(
            originalStopPlace = originalStopPlace,
            childId = "SAM:StopPlace:1000_tram",
            mode = TransportMode.TRAM,
            quayIds = listOf("SAM:Quay:50001"),
            namespace = namespace,
            parentRef = "SAM:StopPlace:1000_parent"
        )

        assertThat(originalStopPlace.getAttributeValue("id")).isEqualTo("SAM:StopPlace:1000")
        assertThat(originalStopPlace.getChild("TransportMode", namespace)).isNull()
        assertThat(originalStopPlace.getChild("ParentSiteRef", namespace)).isNull()

        val originalQuays = originalStopPlace.getChild("quays", namespace)!!
            .getChildren("Quay", namespace)
        assertThat(originalQuays).hasSize(originalQuayCount)

        assertThat(child.getAttributeValue("id")).isEqualTo("SAM:StopPlace:1000_tram")
        assertThat(child.getChildText("TransportMode", namespace)).isEqualTo("tram")

        val childParentSiteRef = child.getChild("ParentSiteRef", namespace)
        assertThat(childParentSiteRef).isNotNull
        assertThat(childParentSiteRef!!.getAttributeValue("ref")).isEqualTo("SAM:StopPlace:1000_parent")

        val childQuays = child.getChild("quays", namespace)!!.getChildren("Quay", namespace)
        assertThat(childQuays)
            .hasSize(1)
            .extracting { it.getAttributeValue("id") }
            .containsExactly("SAM:Quay:50001")
    }
}
