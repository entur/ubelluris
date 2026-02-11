package org.entur.ror.ubelluris.timetable

import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.model.TransportMode
import org.entur.ror.ubelluris.timetable.config.TimetableConfig
import org.entur.ror.ubelluris.timetable.fetch.TimetableFetcher
import org.entur.ror.ubelluris.timetable.model.TimetableData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.nio.file.Files
import java.nio.file.Path

class TimetableProcessorTest {

    @TempDir
    lateinit var tempDir: Path

    private val config = TimetableConfig(
        providers = listOf("provider1"),
        modeFilter = setOf(TransportMode.TRAM, TransportMode.WATER),
        blacklist = emptyMap(),
    )
    private val timetableFetcher: TimetableFetcher = mock()
    private val timetableProcessor = TimetableProcessor(timetableFetcher, config)

    @Test
    fun shouldProcessTimetableAndInsertTransportMode() {
        val stopsFile = tempDir.resolve("stop_places.xml")
        val modeHelperFile = tempDir.resolve("mode_helper_tram.xml")

        Files.writeString(
            stopsFile,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <stopPlaces>
                    <StopPlace id="SAM:StopPlace:1000">
                        <quays>
                            <Quay id="SAM:Quay:50001">
                                <keyList>
                                    <KeyValue>
                                        <Key>local-stoppoint-gid</Key>
                                        <Value>1:001</Value>
                                    </KeyValue>
                                </keyList>
                            </Quay>
                        </quays>
                    </StopPlace>
                </stopPlaces>
            </PublicationDelivery>
            """.trimIndent()
        )

        Files.writeString(
            modeHelperFile,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <Line>
                    <TransportMode>tram</TransportMode>
                </Line>
                <JourneyPattern>
                    <ScheduledStopPointRef ref="SE:001:ScheduledStopPoint:001"/>
                </JourneyPattern>
            </PublicationDelivery>
            """.trimIndent()
        )

        val timetableData = mapOf(
            "provider1" to TimetableData(
                provider = "provider1",
                modeHelperFiles = listOf(modeHelperFile),
                allFiles = listOf(modeHelperFile)
            )
        )

        whenever(timetableFetcher.fetch(any())).thenReturn(timetableData)

        val result = timetableProcessor.process(stopsFile)

        val resultContent = Files.readString(result)
        assertThat(resultContent).contains("<TransportMode>tram</TransportMode>")
    }

    @Test
    fun shouldProcessMultipleStopPlacesWithDifferentModes() {
        val stopsFile = tempDir.resolve("stop_places.xml")
        val tramHelperFile = tempDir.resolve("mode_helper_tram.xml")
        val waterHelperFile = tempDir.resolve("mode_helper_water.xml")

        Files.writeString(
            stopsFile,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <stopPlaces>
                    <StopPlace id="SAM:StopPlace:1000">
                        <quays>
                            <Quay id="SAM:Quay:50001">
                                <keyList>
                                    <KeyValue>
                                        <Key>local-stoppoint-gid</Key>
                                        <Value>1:001</Value>
                                    </KeyValue>
                                </keyList>
                            </Quay>
                        </quays>
                    </StopPlace>
                    <StopPlace id="SAM:StopPlace:2000">
                        <quays>
                            <Quay id="SAM:Quay:60001">
                                <keyList>
                                    <KeyValue>
                                        <Key>local-stoppoint-gid</Key>
                                        <Value>1:002</Value>
                                    </KeyValue>
                                </keyList>
                            </Quay>
                        </quays>
                    </StopPlace>
                </stopPlaces>
            </PublicationDelivery>
            """.trimIndent()
        )

        Files.writeString(
            tramHelperFile,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <Line>
                    <TransportMode>tram</TransportMode>
                </Line>
                <JourneyPattern>
                    <ScheduledStopPointRef ref="SE:001:ScheduledStopPoint:001"/>
                </JourneyPattern>
            </PublicationDelivery>
            """.trimIndent()
        )

        Files.writeString(
            waterHelperFile,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <Line>
                    <TransportMode>water</TransportMode>
                </Line>
                <JourneyPattern>
                    <ScheduledStopPointRef ref="SE:001:ScheduledStopPoint:002"/>
                </JourneyPattern>
            </PublicationDelivery>
            """.trimIndent()
        )

        val timetableData = mapOf(
            "provider1" to TimetableData(
                provider = "provider1",
                modeHelperFiles = listOf(tramHelperFile, waterHelperFile),
                allFiles = listOf(tramHelperFile, waterHelperFile)
            )
        )

        whenever(timetableFetcher.fetch(any())).thenReturn(timetableData)

        val result = timetableProcessor.process(stopsFile)

        val resultContent = Files.readString(result)
        assertThat(resultContent).contains("<TransportMode>tram</TransportMode>")
        assertThat(resultContent).contains("<TransportMode>water</TransportMode>")
    }

    @Test
    fun shouldHandleStopPlaceWithNoMatchingQuays() {
        val stopsFile = tempDir.resolve("stop_places.xml")
        val modeHelperFile = tempDir.resolve("mode_helper_tram.xml")

        Files.writeString(
            stopsFile,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <stopPlaces>
                    <StopPlace id="SAM:StopPlace:1000">
                        <quays>
                            <Quay id="SAM:Quay:50001">
                                <keyList>
                                    <KeyValue>
                                        <Key>local-stoppoint-gid</Key>
                                        <Value>1:999</Value>
                                    </KeyValue>
                                </keyList>
                            </Quay>
                        </quays>
                    </StopPlace>
                </stopPlaces>
            </PublicationDelivery>
            """.trimIndent()
        )

        Files.writeString(
            modeHelperFile,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <Line>
                    <TransportMode>tram</TransportMode>
                </Line>
                <JourneyPattern>
                    <ScheduledStopPointRef ref="SE:001:ScheduledStopPoint:001"/>
                </JourneyPattern>
            </PublicationDelivery>
            """.trimIndent()
        )

        val timetableData = mapOf(
            "provider1" to TimetableData(
                provider = "provider1",
                modeHelperFiles = listOf(modeHelperFile),
                allFiles = listOf(modeHelperFile)
            )
        )

        whenever(timetableFetcher.fetch(any())).thenReturn(timetableData)

        val result = timetableProcessor.process(stopsFile)

        assertThat(result).isEqualTo(stopsFile)
    }

    @Test
    fun shouldProcessUniformModeStopPlace() {
        val stopsFile = tempDir.resolve("stop_places.xml")
        val modeHelperFile = tempDir.resolve("mode_helper_tram.xml")

        Files.writeString(
            stopsFile,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <stopPlaces>
                    <StopPlace id="SAM:StopPlace:1000">
                        <quays>
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
                                        <Value>1:002</Value>
                                    </KeyValue>
                                </keyList>
                            </Quay>
                        </quays>
                    </StopPlace>
                </stopPlaces>
            </PublicationDelivery>
            """.trimIndent()
        )

        Files.writeString(
            modeHelperFile,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <Line>
                    <TransportMode>tram</TransportMode>
                </Line>
                <JourneyPattern>
                    <ScheduledStopPointRef ref="SE:001:ScheduledStopPoint:001"/>
                    <ScheduledStopPointRef ref="SE:001:ScheduledStopPoint:002"/>
                </JourneyPattern>
            </PublicationDelivery>
            """.trimIndent()
        )

        val timetableData = mapOf(
            "provider1" to TimetableData(
                provider = "provider1",
                modeHelperFiles = listOf(modeHelperFile),
                allFiles = listOf(modeHelperFile)
            )
        )

        whenever(timetableFetcher.fetch(any())).thenReturn(timetableData)

        val result = timetableProcessor.process(stopsFile)

        val resultContent = Files.readString(result)
        assertThat(resultContent).contains("<TransportMode>tram</TransportMode>")
    }
}
