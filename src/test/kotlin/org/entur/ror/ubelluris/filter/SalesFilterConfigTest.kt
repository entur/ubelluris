package org.entur.ror.ubelluris.filter

import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.model.DayTypeDateAssignment
import org.entur.ror.ubelluris.sales.OperatingDatesResolver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class SalesFilterConfigTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun shouldMapDayTypeToDaysOfWeek() {
        val result = SalesFilterConfig().run(writeSharedDataFile().toFile()).calendarData

        assertThat(result.dayTypes).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "SE:030:DayType:weekdays" to
                    listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                "SE:030:DayType:empty" to emptyList(),
            ),
        )
    }

    @Test
    fun shouldUseDaysOfAllPropertyOfDayOfDayType() {
        val calendar = SalesFilterConfig().run(writePropertyOfDaySharedDataFile().toFile()).calendarData

        assertThat(calendar.dayTypes["SE:030:DayType:weekend"]).containsExactly(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        assertThat(OperatingDatesResolver(calendar).resolve(listOf("SE:030:DayType:weekend")))
            .containsExactlyInAnyOrderElementsOf(listOf("2026-11-07", "2026-11-08", "2026-11-14", "2026-11-15").map(LocalDate::parse))
    }

    @Test
    fun shouldNotRunOnAnyDayWhenPropertyOfDayIsEmpty() {
        val calendar = SalesFilterConfig().run(writePropertyOfDaySharedDataFile().toFile()).calendarData

        assertThat(calendar.dayTypes["SE:030:DayType:notRunning"]).isEmpty()
        assertThat(OperatingDatesResolver(calendar).resolve(listOf("SE:030:DayType:notRunning"))).isEmpty()
    }

    @Test
    fun shouldMapOperatingPeriodToFromAndToDate() {
        val result = SalesFilterConfig().run(writeSharedDataFile().toFile()).calendarData

        assertThat(result.operatingPeriods).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "SE:030:OperatingPeriod:1" to
                    Pair(LocalDateTime.parse("2026-11-02T00:00:00"), LocalDateTime.parse("2026-12-12T00:00:00")),
                "SE:030:OperatingPeriod:2" to
                    Pair(LocalDateTime.parse("2027-01-04T00:00:00"), LocalDateTime.parse("2027-02-01T00:00:00")),
            ),
        )
    }

    @Test
    fun shouldMapDayTypeToAllItsOperatingPeriods() {
        val result = SalesFilterConfig().run(writeSharedDataFile().toFile()).calendarData

        assertThat(result.operatingPeriodIdsByDayTypeId).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "SE:030:DayType:weekdays" to listOf("SE:030:OperatingPeriod:1", "SE:030:OperatingPeriod:2"),
                "SE:030:DayType:empty" to listOf("SE:030:OperatingPeriod:1"),
            ),
        )
    }

    @Test
    fun shouldMapDayTypeToIncludedAndExcludedDates() {
        val result = SalesFilterConfig().run(writeSharedDataFile().toFile()).calendarData

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

    /** A DayType with two PropertyOfDay, and a DayType with an empty PropertyOfDay. Both use a period from 2026-11-02 to 2026-11-16. */
    private fun writePropertyOfDaySharedDataFile(): Path {
        Files.writeString(
            tempDir.resolve("_shared_data.xml"),
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
              <dataObjects>
                <CompositeFrame id="SE:030:CompositeFrame:1">
                  <frames>
                    <ServiceCalendarFrame id="SE:030:ServiceCalendarFrame:1">
                      <dayTypes>
                        <DayType id="SE:030:DayType:weekend">
                          <properties>
                            <PropertyOfDay><DaysOfWeek>Saturday</DaysOfWeek></PropertyOfDay>
                            <PropertyOfDay><DaysOfWeek>Sunday</DaysOfWeek></PropertyOfDay>
                          </properties>
                        </DayType>
                        <DayType id="SE:030:DayType:notRunning">
                          <properties>
                            <PropertyOfDay/>
                          </properties>
                        </DayType>
                      </dayTypes>
                      <operatingPeriods>
                        <OperatingPeriod id="SE:030:OperatingPeriod:1">
                          <FromDate>2026-11-02T00:00:00</FromDate>
                          <ToDate>2026-11-16T00:00:00</ToDate>
                        </OperatingPeriod>
                      </operatingPeriods>
                      <dayTypeAssignments>
                        <DayTypeAssignment order="1" id="SE:030:DayTypeAssignment:weekend">
                          <OperatingPeriodRef ref="SE:030:OperatingPeriod:1"/>
                          <DayTypeRef ref="SE:030:DayType:weekend"/>
                        </DayTypeAssignment>
                        <DayTypeAssignment order="1" id="SE:030:DayTypeAssignment:notRunning">
                          <OperatingPeriodRef ref="SE:030:OperatingPeriod:1"/>
                          <DayTypeRef ref="SE:030:DayType:notRunning"/>
                        </DayTypeAssignment>
                      </dayTypeAssignments>
                    </ServiceCalendarFrame>
                  </frames>
                </CompositeFrame>
              </dataObjects>
            </PublicationDelivery>
            """.trimIndent(),
        )
        return tempDir
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
                        <OperatingPeriod id="SE:030:OperatingPeriod:2">
                          <FromDate>2027-01-04T00:00:00</FromDate>
                          <ToDate>2027-02-01T00:00:00</ToDate>
                        </OperatingPeriod>
                      </operatingPeriods>
                      <dayTypeAssignments>
                        <DayTypeAssignment order="1" id="SE:030:DayTypeAssignment:weekdays">
                          <OperatingPeriodRef ref="SE:030:OperatingPeriod:1"/>
                          <DayTypeRef ref="SE:030:DayType:weekdays"/>
                        </DayTypeAssignment>
                        <DayTypeAssignment order="3" id="SE:030:DayTypeAssignment:weekdays_2">
                          <OperatingPeriodRef ref="SE:030:OperatingPeriod:2"/>
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
        return tempDir
    }
}
