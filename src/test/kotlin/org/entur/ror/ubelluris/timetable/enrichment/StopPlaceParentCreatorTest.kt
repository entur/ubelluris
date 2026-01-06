package org.entur.ror.ubelluris.timetable.enrichment

import org.assertj.core.api.Assertions.assertThat
import org.jdom2.Namespace
import org.jdom2.input.SAXBuilder
import org.junit.jupiter.api.Test
import java.io.StringReader

class StopPlaceParentCreatorTest {

    private val stopPlaceParentCreator = StopPlaceParentCreator()
    private val namespace = Namespace.getNamespace("http://www.netex.org.uk/netex")

    @Test
    fun shouldCreateParentStopPlaceWithAllElements() {
        val xml = """
            <StopPlace xmlns="http://www.netex.org.uk/netex" id="SAM:StopPlace:1000" version="1">
              <keyList>
                <KeyValue>
                  <Key>owner</Key>
                  <Value>001</Value>
                </KeyValue>
                <KeyValue>
                  <Key>data-from</Key>
                  <Value>Source-System</Value>
                </KeyValue>
              </keyList>
              <Name>Test Station</Name>
              <Centroid>
                <Location>
                  <Longitude>13.0</Longitude>
                  <Latitude>59.0</Latitude>
                </Location>
              </Centroid>
            </StopPlace>
        """.trimIndent()

        val originalStopPlace = SAXBuilder().build(StringReader(xml)).rootElement
        val parentId = "SAM:StopPlace:1000_parent"

        val parent = stopPlaceParentCreator.createParentStopPlace(originalStopPlace, namespace, parentId)

        assertThat(parent.getAttributeValue("id")).isEqualTo(parentId)
        assertThat(parent.getAttributeValue("version")).isEqualTo("1")
        assertThat(parent.name).isEqualTo("StopPlace")

        val keyList = parent.getChild("keyList", namespace)
        assertThat(keyList).isNotNull

        val keyValues = keyList!!.getChildren("KeyValue", namespace)
        assertThat(keyValues).hasSize(2)

        val ownerKv = keyValues.find { it.getChildText("Key", namespace) == "owner" }
        assertThat(ownerKv).isNotNull
        assertThat(ownerKv!!.getChildText("Value", namespace)).isEqualTo("001")

        val dataFromKv = keyValues.find { it.getChildText("Key", namespace) == "data-from" }
        assertThat(dataFromKv).isNotNull
        assertThat(dataFromKv!!.getChildText("Value", namespace)).isEqualTo("Source-System")

        assertThat(parent.getChildText("Name", namespace)).isEqualTo("Test Station")

        val centroid = parent.getChild("Centroid", namespace)
        assertThat(centroid).isNotNull
        val location = centroid!!.getChild("Location", namespace)
        assertThat(location).isNotNull
        assertThat(location!!.getChildText("Longitude", namespace)).isEqualTo("13.0")
        assertThat(location.getChildText("Latitude", namespace)).isEqualTo("59.0")
    }

    @Test
    fun shouldCreateParentStopPlaceWithOnlyOwnerKeyValue() {
        val xml = """
            <StopPlace xmlns="http://www.netex.org.uk/netex" id="SAM:StopPlace:1000" version="1">
              <keyList>
                <KeyValue>
                  <Key>owner</Key>
                  <Value>001</Value>
                </KeyValue>
                <KeyValue>
                  <Key>other-key</Key>
                  <Value>Other-Value</Value>
                </KeyValue>
              </keyList>
              <Name>Test Station</Name>
            </StopPlace>
        """.trimIndent()

        val originalStopPlace = SAXBuilder().build(StringReader(xml)).rootElement
        val parentId = "SAM:StopPlace:1000_parent"

        val parent = stopPlaceParentCreator.createParentStopPlace(originalStopPlace, namespace, parentId)

        val keyList = parent.getChild("keyList", namespace)
        assertThat(keyList).isNotNull

        val keyValues = keyList!!.getChildren("KeyValue", namespace)
        assertThat(keyValues).hasSize(1)

        val ownerKv = keyValues.find { it.getChildText("Key", namespace) == "owner" }
        assertThat(ownerKv).isNotNull
        assertThat(ownerKv!!.getChildText("Value", namespace)).isEqualTo("001")

        val dataFromKv = keyValues.find { it.getChildText("Key", namespace) == "data-from" }
        assertThat(dataFromKv).isNull()
    }

    @Test
    fun shouldCreateParentStopPlaceWithOnlyDataFromKeyValue() {
        val xml = """
            <StopPlace xmlns="http://www.netex.org.uk/netex" id="SAM:StopPlace:1000" version="1">
              <keyList>
                <KeyValue>
                  <Key>data-from</Key>
                  <Value>Source-System</Value>
                </KeyValue>
              </keyList>
              <Name>Test Station</Name>
            </StopPlace>
        """.trimIndent()

        val originalStopPlace = SAXBuilder().build(StringReader(xml)).rootElement
        val parentId = "SAM:StopPlace:1000_parent"

        val parent = stopPlaceParentCreator.createParentStopPlace(originalStopPlace, namespace, parentId)

        val keyList = parent.getChild("keyList", namespace)
        assertThat(keyList).isNotNull

        val keyValues = keyList!!.getChildren("KeyValue", namespace)
        assertThat(keyValues).hasSize(1)

        val dataFromKv = keyValues.find { it.getChildText("Key", namespace) == "data-from" }
        assertThat(dataFromKv).isNotNull
        assertThat(dataFromKv!!.getChildText("Value", namespace)).isEqualTo("Source-System")
    }

    @Test
    fun shouldCreateParentStopPlaceWithoutKeyList() {
        val xml = """
            <StopPlace xmlns="http://www.netex.org.uk/netex" id="SAM:StopPlace:1000" version="1">
              <Name>Test Station</Name>
              <Centroid>
                <Location>
                  <Longitude>13.0</Longitude>
                  <Latitude>59.0</Latitude>
                </Location>
              </Centroid>
            </StopPlace>
        """.trimIndent()

        val originalStopPlace = SAXBuilder().build(StringReader(xml)).rootElement
        val parentId = "SAM:StopPlace:1000_parent"

        val parent = stopPlaceParentCreator.createParentStopPlace(originalStopPlace, namespace, parentId)

        assertThat(parent.getAttributeValue("id")).isEqualTo(parentId)
        assertThat(parent.getAttributeValue("version")).isEqualTo("1")

        val keyList = parent.getChild("keyList", namespace)
        assertThat(keyList).isNotNull
        assertThat(keyList!!.getChildren("KeyValue", namespace)).isEmpty()

        assertThat(parent.getChildText("Name", namespace)).isEqualTo("Test Station")
        assertThat(parent.getChild("Centroid", namespace)).isNotNull
    }

    @Test
    fun shouldCreateParentStopPlaceWithoutName() {
        val xml = """
            <StopPlace xmlns="http://www.netex.org.uk/netex" id="SAM:StopPlace:1000" version="1">
              <keyList>
                <KeyValue>
                  <Key>owner</Key>
                  <Value>001</Value>
                </KeyValue>
              </keyList>
              <Centroid>
                <Location>
                  <Longitude>13.0</Longitude>
                  <Latitude>59.0</Latitude>
                </Location>
              </Centroid>
            </StopPlace>
        """.trimIndent()

        val originalStopPlace = SAXBuilder().build(StringReader(xml)).rootElement
        val parentId = "SAM:StopPlace:1000_parent"

        val parent = stopPlaceParentCreator.createParentStopPlace(originalStopPlace, namespace, parentId)

        assertThat(parent.getAttributeValue("id")).isEqualTo(parentId)
        assertThat(parent.getChild("Name", namespace)).isNull()
        assertThat(parent.getChild("Centroid", namespace)).isNotNull
    }

    @Test
    fun shouldCreateParentStopPlaceWithoutCentroid() {
        val xml = """
            <StopPlace xmlns="http://www.netex.org.uk/netex" id="SAM:StopPlace:1000" version="1">
              <keyList>
                <KeyValue>
                  <Key>owner</Key>
                  <Value>001</Value>
                </KeyValue>
              </keyList>
              <Name>Test Station</Name>
            </StopPlace>
        """.trimIndent()

        val originalStopPlace = SAXBuilder().build(StringReader(xml)).rootElement
        val parentId = "SAM:StopPlace:1000_parent"

        val parent = stopPlaceParentCreator.createParentStopPlace(originalStopPlace, namespace, parentId)

        assertThat(parent.getAttributeValue("id")).isEqualTo(parentId)
        assertThat(parent.getChildText("Name", namespace)).isEqualTo("Test Station")
        assertThat(parent.getChild("Centroid", namespace)).isNull()
    }

    @Test
    fun shouldCreateMinimalParentStopPlace() {
        val xml = """
            <StopPlace xmlns="http://www.netex.org.uk/netex" id="SAM:StopPlace:1000" version="1">
            </StopPlace>
        """.trimIndent()

        val originalStopPlace = SAXBuilder().build(StringReader(xml)).rootElement
        val parentId = "SAM:StopPlace:1000_parent"

        val parent = stopPlaceParentCreator.createParentStopPlace(originalStopPlace, namespace, parentId)

        assertThat(parent.getAttributeValue("id")).isEqualTo(parentId)
        assertThat(parent.getAttributeValue("version")).isEqualTo("1")

        val keyList = parent.getChild("keyList", namespace)
        assertThat(keyList).isNotNull
        assertThat(keyList!!.getChildren()).isEmpty()

        assertThat(parent.getChild("Name", namespace)).isNull()
        assertThat(parent.getChild("Centroid", namespace)).isNull()
    }

    @Test
    fun shouldCloneElementsNotModifyOriginals() {
        val xml = """
            <StopPlace xmlns="http://www.netex.org.uk/netex" id="SAM:StopPlace:1000" version="1">
              <Name>Test Station</Name>
              <Centroid>
                <Location>
                  <Longitude>13.0</Longitude>
                  <Latitude>59.0</Latitude>
                </Location>
              </Centroid>
            </StopPlace>
        """.trimIndent()

        val originalStopPlace = SAXBuilder().build(StringReader(xml)).rootElement
        val parentId = "SAM:StopPlace:1000_parent"

        val parent = stopPlaceParentCreator.createParentStopPlace(originalStopPlace, namespace, parentId)
        parent.getChild("Name", namespace)!!.text = "Modified Name"

        assertThat(originalStopPlace.getChildText("Name", namespace)).isEqualTo("Test Station")
        assertThat(parent.getChildText("Name", namespace)).isEqualTo("Modified Name")
    }
}