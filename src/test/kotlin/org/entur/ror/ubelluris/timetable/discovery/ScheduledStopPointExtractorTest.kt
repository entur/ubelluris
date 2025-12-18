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
        val provider1 = "provider1"
        val provider2 = "provider2"

        val xmlFile1 = tempDir.resolve("provider1_line.xml")
        val xmlFile2 = tempDir.resolve("provider2_line.xml")

        Files.writeString(
            xmlFile1,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <Line>
                    <TransportMode>tram</TransportMode>
                </Line>
                <JourneyPattern>
                    <ScheduledStopPointRef ref="SE:001:ScheduledStopPoint:1"/>
                </JourneyPattern>
            </PublicationDelivery>
        """.trimIndent()
        )

        Files.writeString(
            xmlFile2,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <Line>
                    <TransportMode>bus</TransportMode>
                </Line>
                <JourneyPattern>
                    <ScheduledStopPointRef ref="SE:002:ScheduledStopPoint:10"/>
                    <ScheduledStopPointRef ref="SE:002:ScheduledStopPoint:11"/>
                </JourneyPattern>
            </PublicationDelivery>
        """.trimIndent()
        )

        val timetableDataMap = mapOf(
            provider1 to TimetableData(
                provider = provider1,
                modeHelperFiles = listOf(xmlFile1),
                allFiles = listOf(xmlFile1)
            ),
            provider2 to TimetableData(
                provider = provider2,
                modeHelperFiles = listOf(xmlFile2),
                allFiles = listOf(xmlFile2)
            )
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
                tuple(provider1, "SE:001:ScheduledStopPoint:1", TransportMode.TRAM),
                tuple(provider2, "SE:002:ScheduledStopPoint:10", TransportMode.BUS),
                tuple(provider2, "SE:002:ScheduledStopPoint:11", TransportMode.BUS)
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

    @Test
    fun shouldHandleMixOfValidAndInvalidFiles() {
        val provider = "provider1"

        val invalidXmlFile = tempDir.resolve("invalid.xml")
        Files.writeString(
            invalidXmlFile,
            "<PublicationDelivery><Line><TransportMode>tram</TransportMode></Line>"
        )

        val validXmlFile = tempDir.resolve("valid.xml")
        Files.writeString(
            validXmlFile,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <Line>
                    <TransportMode>tram</TransportMode>
                </Line>
                <JourneyPattern>
                    <ScheduledStopPointRef ref="SE:001:ScheduledStopPoint:1"/>
                </JourneyPattern>
            </PublicationDelivery>
        """.trimIndent()
        )

        val timetableData = TimetableData(
            provider = provider,
            modeHelperFiles = listOf(invalidXmlFile, validXmlFile),
            allFiles = listOf(invalidXmlFile, validXmlFile)
        )

        val result = scheduledStopPointExtractor.extract(
            mapOf(provider to timetableData)
        )

        assertThat(result)
            .hasSize(1)
            .extracting(
                ScheduledStopPointRef::provider,
                ScheduledStopPointRef::originalRef,
                ScheduledStopPointRef::transportMode
            )
            .containsExactly(
                tuple(provider, "SE:001:ScheduledStopPoint:1", TransportMode.TRAM)
            )
    }

}