package org.entur.ror.ubelluris.utils

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class NetexEntityCollectorTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun shouldCollectOnlyNamedEntities() {
        val entities = NetexEntityCollector(setOf("ServiceJourney")).collect(writeXml())

        assertThat(entities).extracting<String> { it.id }.containsExactly("SJ:1")
    }

    @Test
    fun shouldCollectRefsAndLeafTextByRelativePath() {
        val serviceJourney = NetexEntityCollector(setOf("ServiceJourney")).collect(writeXml()).single()

        assertThat(serviceJourney.requireValue("TransportMode")).isEqualTo("rail")
        assertThat(serviceJourney.requireValue("JourneyPatternRef")).isEqualTo("JP:1")
        assertThat(serviceJourney.values("dayTypes/DayTypeRef")).containsExactly("DT:A", "DT:B")
        assertThat(serviceJourney.hasValue("TransportSubmode")).isFalse()
        assertThat(serviceJourney.requireValue("TransportSubmode/RailSubmode")).isEqualTo("local")
    }

    @Test
    fun shouldCollectOnlyDirectKeyListKeyValues() {
        val serviceJourney = NetexEntityCollector(setOf("ServiceJourney")).collect(writeXml()).single()

        assertThat(serviceJourney.requireKeyValue("productCode")).isEqualTo("LT")
        assertThatThrownBy { serviceJourney.requireKeyValue("attributes") }
            .hasMessageContaining("Missing KeyValue 'attributes'")
    }

    @Test
    fun shouldFailWithEntityIdWhenValueIsMissing() {
        val serviceJourney = NetexEntityCollector(setOf("ServiceJourney")).collect(writeXml()).single()

        assertThatThrownBy { serviceJourney.requireValue("OperatorRef") }
            .hasMessageContaining("Missing <OperatorRef> in <ServiceJourney id=\"SJ:1\">")
    }

    private fun writeXml(): Path {
        val xmlPath = tempDir.resolve("line.xml")
        Files.writeString(
            xmlPath,
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
              <dataObjects>
                <ServiceJourney id="SJ:1">
                  <keyList>
                    <KeyValue><Key>productCode</Key><Value>LT</Value></KeyValue>
                  </keyList>
                  <TransportMode>rail</TransportMode>
                  <TransportSubmode><RailSubmode>local</RailSubmode></TransportSubmode>
                  <dayTypes>
                    <DayTypeRef ref="DT:A"/>
                    <DayTypeRef ref="DT:B"/>
                  </dayTypes>
                  <JourneyPatternRef ref="JP:1"/>
                  <facilities>
                    <ServiceFacilitySet id="SFS:1">
                      <keyList>
                        <KeyValue><Key>attributes</Key><Value>EVFSFS</Value></KeyValue>
                      </keyList>
                    </ServiceFacilitySet>
                  </facilities>
                </ServiceJourney>
                <Line id="Line:1"/>
              </dataObjects>
            </PublicationDelivery>
            """.trimIndent(),
        )
        return xmlPath
    }
}
