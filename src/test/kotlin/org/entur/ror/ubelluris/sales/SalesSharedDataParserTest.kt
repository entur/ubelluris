package org.entur.ror.ubelluris.sales

import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.model.DayTypeDateAssignment
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class SalesSharedDataParserTest {
    @TempDir
    lateinit var tempDir: Path

    private val parser = SalesSharedDataParser()

    @Test
    fun shouldMapDayTypeToDaysOfWeek() {
        val result = parser.parse(writeSharedDataFile())

        assertThat(result.dayTypes).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "SE:030:DayType:weekdays" to
                    listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                "SE:030:DayType:empty" to emptyList(),
            ),
        )
    }

    @Test
    fun shouldMapOperatingPeriodToFromAndToDate() {
        val result = parser.parse(writeSharedDataFile())

        assertThat(result.operatingPeriods).containsExactlyEntriesOf(
            mapOf(
                "SE:030:OperatingPeriod:1" to
                    Pair(LocalDateTime.parse("2026-11-02T00:00:00"), LocalDateTime.parse("2026-12-12T00:00:00")),
            ),
        )
    }

    @Test
    fun shouldMapDayTypeToOperatingPeriod() {
        val result = parser.parse(writeSharedDataFile())

        assertThat(result.dayTypeAssignments).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "SE:030:DayType:weekdays" to "SE:030:OperatingPeriod:1",
                "SE:030:DayType:empty" to "SE:030:OperatingPeriod:1",
            ),
        )
    }

    @Test
    fun shouldMapDayTypeToIncludedAndExcludedDates() {
        val result = parser.parse(writeSharedDataFile())

        assertThat(result.dayTypeDateAssignments).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "SE:030:DayType:weekdays" to listOf(DayTypeDateAssignment(LocalDate.parse("2026-12-24"), isAvailable = false)),
                "SE:030:DayType:empty" to
                    listOf(
                        DayTypeDateAssignment(LocalDate.parse("2026-12-28"), isAvailable = true),
                        DayTypeDateAssignment(LocalDate.parse("2026-12-29"), isAvailable = true),
                    ),
            ),
        )
    }

    private fun writeSharedDataFile(): Path {
        val sharedDataPath = tempDir.resolve("_shared_data.xml")
        Files.writeString(
            sharedDataPath,
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
              <dataObjects>
                <CompositeFrame id="SE:030:CompositeFrame:1">
                  <frames>
                    <ServiceCalendarFrame id="SE:030:ServiceCalendarFrame:1">
                      <dayTypes>
                        <DayType id="SE:030:DayType:weekdays">
                          <properties>
                            <PropertyOfDay><DaysOfWeek>Monday Tuesday Wednesday Thursday Friday</DaysOfWeek></PropertyOfDay>
                          </properties>
                        </DayType>
                        <DayType id="SE:030:DayType:empty">
                          <properties>
                            <PropertyOfDay><DaysOfWeek/></PropertyOfDay>
                          </properties>
                        </DayType>
                      </dayTypes>
                      <operatingPeriods>
                        <OperatingPeriod id="SE:030:OperatingPeriod:1">
                          <FromDate>2026-11-02T00:00:00</FromDate>
                          <ToDate>2026-12-12T00:00:00</ToDate>
                        </OperatingPeriod>
                      </operatingPeriods>
                      <dayTypeAssignments>
                        <DayTypeAssignment order="1" id="SE:030:DayTypeAssignment:weekdays">
                          <OperatingPeriodRef ref="SE:030:OperatingPeriod:1"/>
                          <DayTypeRef ref="SE:030:DayType:weekdays"/>
                        </DayTypeAssignment>
                        <DayTypeAssignment order="2" id="SE:030:DayTypeAssignment:weekdays_261224">
                          <Date>2026-12-24</Date>
                          <DayTypeRef ref="SE:030:DayType:weekdays"/>
                          <isAvailable>false</isAvailable>
                        </DayTypeAssignment>
                        <DayTypeAssignment order="1" id="SE:030:DayTypeAssignment:empty">
                          <OperatingPeriodRef ref="SE:030:OperatingPeriod:1"/>
                          <DayTypeRef ref="SE:030:DayType:empty"/>
                        </DayTypeAssignment>
                        <DayTypeAssignment order="2" id="SE:030:DayTypeAssignment:empty_261228">
                          <Date>2026-12-28</Date>
                          <DayTypeRef ref="SE:030:DayType:empty"/>
                        </DayTypeAssignment>
                        <DayTypeAssignment order="3" id="SE:030:DayTypeAssignment:empty_261229">
                          <Date>2026-12-29</Date>
                          <DayTypeRef ref="SE:030:DayType:empty"/>
                        </DayTypeAssignment>
                      </dayTypeAssignments>
                    </ServiceCalendarFrame>
                  </frames>
                </CompositeFrame>
              </dataObjects>
            </PublicationDelivery>
            """.trimIndent(),
        )
        return sharedDataPath
    }
}
