package org.entur.ror.ubelluris.sales

import org.rutebanken.netex.model.DayOfWeekEnumeration
import org.rutebanken.netex.model.DayType
import org.rutebanken.netex.model.DayTypeAssignment
import org.rutebanken.netex.model.DayTypeAssignmentsInFrame_RelStructure
import org.rutebanken.netex.model.DayTypeRefStructure
import org.rutebanken.netex.model.DayTypesInFrame_RelStructure
import org.rutebanken.netex.model.OperatingDay
import org.rutebanken.netex.model.OperatingDaysInFrame_RelStructure
import org.rutebanken.netex.model.OperatingPeriod
import org.rutebanken.netex.model.OperatingPeriodRefStructure
import org.rutebanken.netex.model.OperatingPeriodsInFrame_RelStructure
import org.rutebanken.netex.model.PropertiesOfDay_RelStructure
import org.rutebanken.netex.model.PropertyOfDay
import org.rutebanken.netex.model.ServiceCalendarFrame
import java.math.BigInteger
import java.nio.file.Path
import java.time.LocalDateTime

private const val MONDAYS_DAY_TYPE_ID = "SE:100:DayType:mondays"
private const val DATES_ONLY_DAY_TYPE_ID = "SE:100:DayType:datesOnly"
private const val OPERATING_PERIOD_ID = "SE:100:OperatingPeriod:1"
const val EXISTING_OPERATING_DAY_ID = "SE:100:OperatingDay:existing"

/**
 * Writes a timetable _shared_data.xml with this calendar to [directory]:
 * - SE:100:DayType:mondays: Mondays in OperatingPeriod 2026-11-02 to 2026-11-16 (exclusive), except 2026-11-09
 * - SE:100:DayType:datesOnly: only 2026-11-10
 * With [existingOperatingDay], the frame also has operatingDays with OperatingDay [EXISTING_OPERATING_DAY_ID].
 */
fun writeTimetableSharedDataFile(
    directory: Path,
    existingOperatingDay: Boolean = false,
) {
    val serviceCalendarFrame =
        ServiceCalendarFrame()
            .withId("SE:100:ServiceCalendarFrame:1")
            .withVersion(TEST_NETEX_VERSION)
            .withDayTypes(
                DayTypesInFrame_RelStructure().withDayType_(
                    netexFactory.createDayType(dayType(MONDAYS_DAY_TYPE_ID, DayOfWeekEnumeration.MONDAY)),
                    netexFactory.createDayType(dayType(DATES_ONLY_DAY_TYPE_ID)),
                ),
            ).withOperatingPeriods(
                OperatingPeriodsInFrame_RelStructure().withOperatingPeriodOrUicOperatingPeriod(
                    OperatingPeriod()
                        .withId(OPERATING_PERIOD_ID)
                        .withVersion(TEST_NETEX_VERSION)
                        .withFromDate(LocalDateTime.parse("2026-11-02T00:00:00"))
                        .withToDate(LocalDateTime.parse("2026-11-16T00:00:00")),
                ),
            ).withDayTypeAssignments(
                DayTypeAssignmentsInFrame_RelStructure().withDayTypeAssignment(
                    dayTypeAssignment("SE:100:DayTypeAssignment:mondays", order = 1, MONDAYS_DAY_TYPE_ID)
                        .withOperatingPeriodRef(
                            netexFactory.createOperatingPeriodRef(OperatingPeriodRefStructure().withRef(OPERATING_PERIOD_ID)),
                        ),
                    dayTypeAssignment("SE:100:DayTypeAssignment:mondays_261109", order = 2, MONDAYS_DAY_TYPE_ID)
                        .withDate(LocalDateTime.parse("2026-11-09T00:00:00"))
                        .withIsAvailable(false),
                    dayTypeAssignment("SE:100:DayTypeAssignment:datesOnly_261110", order = 1, DATES_ONLY_DAY_TYPE_ID)
                        .withDate(LocalDateTime.parse("2026-11-10T00:00:00")),
                ),
            )
    if (existingOperatingDay) {
        serviceCalendarFrame.withOperatingDays(
            OperatingDaysInFrame_RelStructure().withOperatingDay(
                OperatingDay()
                    .withId(EXISTING_OPERATING_DAY_ID)
                    .withVersion(TEST_NETEX_VERSION)
                    .withCalendarDate(LocalDateTime.parse("2026-12-01T00:00:00")),
            ),
        )
    }

    writePublicationDelivery(
        directory.resolve("_shared_data.xml"),
        "SE:100:CompositeFrame:1",
        netexFactory.createServiceCalendarFrame(serviceCalendarFrame),
    )
}

/** A DayType that operates on [daysOfWeek]. Without days of week it operates only on its date assignments. */
private fun dayType(
    id: String,
    vararg daysOfWeek: DayOfWeekEnumeration,
): DayType =
    DayType()
        .withId(id)
        .withVersion(TEST_NETEX_VERSION)
        .withProperties(PropertiesOfDay_RelStructure().withPropertyOfDay(PropertyOfDay().withDaysOfWeek(*daysOfWeek)))

private fun dayTypeAssignment(
    id: String,
    order: Int,
    dayTypeId: String,
): DayTypeAssignment =
    DayTypeAssignment()
        .withId(id)
        .withVersion(TEST_NETEX_VERSION)
        .withOrder(BigInteger.valueOf(order.toLong()))
        .withDayTypeRef(netexFactory.createDayTypeRef(DayTypeRefStructure().withRef(dayTypeId)))
