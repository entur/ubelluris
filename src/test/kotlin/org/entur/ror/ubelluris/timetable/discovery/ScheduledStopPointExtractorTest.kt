package org.entur.ror.ubelluris.timetable.discovery

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.entur.ror.ubelluris.model.TransportMode
import org.entur.ror.ubelluris.timetable.model.ScheduledStopPointRef
import org.entur.ror.ubelluris.timetable.model.TimetableData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ScheduledStopPointExtractorTest {

    private val scheduledStopPointExtractor = ScheduledStopPointExtractor()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun shouldExtractScheduledStopPointsFromTimetableData() {
        val provider = "provider1"
        val xmlFile = tempDir.resolve("line_001.xml")

        Files.writeString(
            xmlFile,
            """
                <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                    <Line>
                        <TransportMode>tram</TransportMode>
                    </Line>

                    <JourneyPattern>
                        <ScheduledStopPointRef ref="SE:001:ScheduledStopPoint:1"/>
                        <ScheduledStopPointRef ref="SE:001:ScheduledStopPoint:2"/>
                    </JourneyPattern>

                    <JourneyPattern>
                        <ScheduledStopPointRef ref="SE:001:ScheduledStopPoint:3"/>
                    </JourneyPattern>
                </PublicationDelivery>
            """.trimIndent()
        )

        val timetableData = TimetableData(
            provider = provider,
            modeHelperFiles = listOf(xmlFile),
            allFiles = listOf(xmlFile)
        )

        val timetableDataMap = mapOf(
            provider to timetableData
        )

        val result = scheduledStopPointExtractor.extract(timetableDataMap)

        assertThat(result)
            .hasSize(3)
            .extracting(
                ScheduledStopPointRef::provider,
                ScheduledStopPointRef::originalRef,
                ScheduledStopPointRef::transportMode
            )
            .containsExactlyInAnyOrder(
                tuple(provider, "SE:001:ScheduledStopPoint:1", TransportMode.TRAM),
                tuple(provider, "SE:001:ScheduledStopPoint:2", TransportMode.TRAM),
                tuple(provider, "SE:001:ScheduledStopPoint:3", TransportMode.TRAM)
            )
    }

    @Test
    fun shouldReturnEmptyListIfNoTransportModeInTimetableData() {
        val provider = "provider1"
        val xmlFile = tempDir.resolve("line_001.xml")

        Files.writeString(
            xmlFile,
            """
                <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                    <Line />
                    <JourneyPattern>
                        <ScheduledStopPointRef ref="SE:001:ScheduledStopPoint:1"/>
                        <ScheduledStopPointRef ref="SE:001:ScheduledStopPoint:2"/>
                    </JourneyPattern>

                    <JourneyPattern>
                        <ScheduledStopPointRef ref="SE:001:ScheduledStopPoint:3"/>
                    </JourneyPattern>
                </PublicationDelivery>
            """.trimIndent()
        )

        val timetableData = TimetableData(
            provider = provider,
            modeHelperFiles = listOf(xmlFile),
            allFiles = listOf(xmlFile)
        )

        val timetableDataMap = mapOf(
            provider to timetableData
        )

        val result = scheduledStopPointExtractor.extract(timetableDataMap)

        assertThat(result).isEmpty()
    }
}