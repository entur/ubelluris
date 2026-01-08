package org.entur.ror.ubelluris.timetable.extraction

import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.model.TransportMode
import org.entur.ror.ubelluris.timetable.model.ScheduledStopPointRef
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class QuayModeMatcherTest {

    private val quayModeMatcher = QuayModeMatcher()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun shouldMatchScheduledStopPointRefsAcrossMultipleStopPlaces() {
        val scheduledStopPointRefs = listOf(
            ScheduledStopPointRef(
                provider = "provider",
                originalRef = "SE:001:ScheduledStopPoint:001",
                transportMode = TransportMode.WATER
            ),
            ScheduledStopPointRef(
                provider = "provider",
                originalRef = "SE:001:ScheduledStopPoint:011",
                transportMode = TransportMode.WATER
            ),
            ScheduledStopPointRef(
                provider = "provider",
                originalRef = "SE:001:ScheduledStopPoint:002",
                transportMode = TransportMode.TRAM
            ),
            ScheduledStopPointRef(
                provider = "provider",
                originalRef = "SE:001:ScheduledStopPoint:003",
                transportMode = TransportMode.BUS
            )
        )

        val xmlFile = tempDir.resolve("stop_places.xml")

        Files.writeString(
            xmlFile,
            """
                <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <stopPlaces>
                  <StopPlace id="SAM:StopPlace:1000">
                    <Quay id="SAM:Quay:50001">
                      <keyList>
                        <KeyValue>
                          <Key>local-stoppoint-gid</Key>
                          <Value>1:001</Value>
                        </KeyValue>
                      </keyList>
                    </Quay>
                    <Quay id="SAM:Quay:50002">
                      <keyList>
                        <KeyValue>
                          <Key>local-stoppoint-gid</Key>
                          <Value>1:011</Value>
                        </KeyValue>
                      </keyList>
                    </Quay>
                  </StopPlace>

                  <StopPlace id="SAM:StopPlace:2000">
                    <Quay id="SAM:Quay:60001">
                      <keyList>
                        <KeyValue>
                          <Key>local-stoppoint-gid</Key>
                          <Value>1:002</Value>
                        </KeyValue>
                      </keyList>
                    </Quay>

                    <Quay id="SAM:Quay:60002">
                      <keyList>
                        <KeyValue>
                          <Key>local-stoppoint-gid</Key>
                          <Value>1:003</Value>
                        </KeyValue>
                      </keyList>
                    </Quay>
                  </StopPlace>
                  </stopPlaces>
                </PublicationDelivery>
            """.trimIndent()
        )

        val result = quayModeMatcher.match(xmlFile, scheduledStopPointRefs)

        assertThat(result.quayToModes).hasSize(4)
        assertThat(result.quayToModes["SAM:Quay:50001"]).containsExactly(TransportMode.WATER)
        assertThat(result.quayToModes["SAM:Quay:50002"]).containsExactly(TransportMode.WATER)
        assertThat(result.quayToModes["SAM:Quay:60001"]).containsExactly(TransportMode.TRAM)
        assertThat(result.quayToModes["SAM:Quay:60002"]).containsExactly(TransportMode.BUS)

        assertThat(result.quayToStopPlace).hasSize(4)
        assertThat(result.quayToStopPlace)
            .containsEntry("SAM:Quay:50001", "SAM:StopPlace:1000")
            .containsEntry("SAM:Quay:50002", "SAM:StopPlace:1000")
            .containsEntry("SAM:Quay:60001", "SAM:StopPlace:2000")
            .containsEntry("SAM:Quay:60002", "SAM:StopPlace:2000")
    }

    @Test
    fun shouldNotMatchWhenInvalidRefFormat() {
        val scheduledStopPointRefs = listOf(
            ScheduledStopPointRef(
                provider = "provider",
                originalRef = "INVALID",
                transportMode = TransportMode.TRAM
            )
        )

        val xmlFile = tempDir.resolve("stop_places.xml")

        Files.writeString(
            xmlFile,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
              <StopPlace id="SAM:StopPlace:1234">
                <Quay id="SAM:Quay:50571">
                  <keyList>
                    <KeyValue>
                      <Key>local-stoppoint-gid</Key>
                      <Value>1:001</Value>
                    </KeyValue>
                  </keyList>
                </Quay>
              </StopPlace>
            </PublicationDelivery>
        """.trimIndent()
        )

        val result = quayModeMatcher.match(xmlFile, scheduledStopPointRefs)

        assertThat(result.quayToModes).isEmpty()
        assertThat(result.quayToStopPlace).isEmpty()
    }

    @Test
    fun shouldHandleQuayWithMultipleGids() {
        val scheduledStopPointRefs = listOf(
            ScheduledStopPointRef(
                provider = "provider",
                originalRef = "SE:001:ScheduledStopPoint:001",
                transportMode = TransportMode.TRAM
            ),
            ScheduledStopPointRef(
                provider = "provider",
                originalRef = "SE:001:ScheduledStopPoint:002",
                transportMode = TransportMode.BUS
            )
        )

        val xmlFile = tempDir.resolve("stop_places.xml")

        Files.writeString(
            xmlFile,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
              <StopPlace id="SAM:StopPlace:1234">
                <Quay id="SAM:Quay:50571">
                  <keyList>
                    <KeyValue>
                      <Key>local-stoppoint-gid</Key>
                      <Value>1:001|1:002</Value>
                    </KeyValue>
                  </keyList>
                </Quay>
              </StopPlace>
            </PublicationDelivery>
        """.trimIndent()
        )

        val result = quayModeMatcher.match(xmlFile, scheduledStopPointRefs)

        assertThat(result.quayToModes).hasSize(1)

        assertThat(result.quayToModes["SAM:Quay:50571"])
            .containsExactlyInAnyOrder(
                TransportMode.TRAM,
                TransportMode.BUS
            )

        assertThat(result.quayToStopPlace)
            .hasSize(1)
            .containsEntry("SAM:Quay:50571", "SAM:StopPlace:1234")
    }


    @Test
    fun shouldHandleQuayWithNoMatch() {
        val scheduledStopPointRefs = listOf(
            ScheduledStopPointRef(
                provider = "provider",
                originalRef = "SE:001:ScheduledStopPoint:999",
                transportMode = TransportMode.TRAM
            )
        )

        val xmlFile = tempDir.resolve("stop_places.xml")

        Files.writeString(
            xmlFile,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
              <StopPlace id="SAM:StopPlace:1234">
                <Quay id="SAM:Quay:50571">
                  <keyList>
                    <KeyValue>
                      <Key>local-stoppoint-gid</Key>
                      <Value>1:001</Value>
                    </KeyValue>
                  </keyList>
                </Quay>
              </StopPlace>
            </PublicationDelivery>
        """.trimIndent()
        )

        val result = quayModeMatcher.match(xmlFile, scheduledStopPointRefs)

        assertThat(result.quayToModes).isEmpty()
        assertThat(result.quayToStopPlace).isEmpty()
    }

    @Test
    fun shouldHandleQuayWithoutKeyValues() {
        val scheduledStopPointRefs = listOf(
            ScheduledStopPointRef(
                provider = "provider",
                originalRef = "SE:001:ScheduledStopPoint:001",
                transportMode = TransportMode.TRAM
            )
        )

        val xmlFile = tempDir.resolve("stop_places.xml")

        Files.writeString(
            xmlFile,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
              <StopPlace id="SAM:StopPlace:1234">
                <Quay id="SAM:Quay:50571">
                  <!-- MISSING -->
                </Quay>
              </StopPlace>
            </PublicationDelivery>
        """.trimIndent()
        )

        val result = quayModeMatcher.match(xmlFile, scheduledStopPointRefs)

        assertThat(result.quayToModes).isEmpty()
        assertThat(result.quayToStopPlace).isEmpty()
    }

}