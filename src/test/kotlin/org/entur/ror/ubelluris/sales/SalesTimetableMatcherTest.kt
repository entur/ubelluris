package org.entur.ror.ubelluris.sales

import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.model.DayTypeDateAssignment
import org.entur.ror.ubelluris.model.SalesData
import org.entur.ror.ubelluris.model.SalesServiceJourney
import org.entur.ror.ubelluris.model.SharedCalendarData
import org.entur.ror.ubelluris.sales.SalesTimetableMatcher.MatchOutcome
import org.entur.ror.ubelluris.sales.SalesTimetableMatcher.SalesValues
import org.entur.ror.ubelluris.sales.SalesTimetableMatcher.ServiceJourneyMatch
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SalesTimetableMatcherTest {
    private val salesData =
        SalesData(
            trainNrServiceJourneysMap =
                mapOf(
                    "100" to
                        listOf(
                            salesServiceJourney("SE:030:ServiceJourney:100_a", "100", "SE:030:DayType:10nov", productCode = "LT"),
                            salesServiceJourney("SE:030:ServiceJourney:100_b", "100", "SE:030:DayType:11nov", productCode = "LT"),
                        ),
                    "200" to
                        listOf(
                            salesServiceJourney("SE:030:ServiceJourney:200_a", "200", "SE:030:DayType:10nov", productCode = "LT"),
                            salesServiceJourney("SE:030:ServiceJourney:200_b", "200", "SE:030:DayType:10nov", productCode = "RE"),
                        ),
                    "300" to
                        listOf(
                            salesServiceJourney(
                                "SE:030:ServiceJourney:300_a",
                                "300",
                                "SE:030:DayType:10nov",
                                productCode = "LT",
                                salesAuthorityNumber = "275",
                            ),
                            salesServiceJourney(
                                "SE:030:ServiceJourney:300_b",
                                "300",
                                "SE:030:DayType:10nov",
                                productCode = "IC",
                                salesAuthorityNumber = "74",
                            ),
                        ),
                ),
            calendarData =
                dateOnlyCalendar(
                    "SE:030:DayType:10nov" to listOf("2026-11-10"),
                    "SE:030:DayType:11nov" to listOf("2026-11-11"),
                ),
        )

    private val timetableCalendar =
        dateOnlyCalendar(
            "SE:100:DayType:10nov" to listOf("2026-11-10"),
            "SE:100:DayType:10and11nov" to listOf("2026-11-10", "2026-11-11"),
            "SE:100:DayType:1dec" to listOf("2026-12-01"),
        )

    private val matcher = SalesTimetableMatcher(salesData, timetableCalendar)

    @Test
    fun shouldMatchWithSalesValuesWhenOneSalesServiceJourneyOverlaps() {
        assertThat(matcher.match(railServiceJourney("SJ:1", listOf("100"), "SE:100:DayType:10nov")))
            .isEqualTo(ServiceJourneyMatch("SJ:1", MatchOutcome.MATCHED, SalesValues("LT", "279")))
    }

    @Test
    fun shouldMatchWhenSeveralOverlappingSalesServiceJourneysAgree() {
        assertThat(matcher.match(railServiceJourney("SJ:1", listOf("100"), "SE:100:DayType:10and11nov")))
            .isEqualTo(ServiceJourneyMatch("SJ:1", MatchOutcome.MATCHED, SalesValues("LT", "279")))
    }

    @Test
    fun shouldNotMatchWhenOverlappingSalesServiceJourneysDisagree() {
        assertThat(matcher.match(railServiceJourney("SJ:1", listOf("200"), "SE:100:DayType:10nov")))
            .isEqualTo(ServiceJourneyMatch("SJ:1", MatchOutcome.AMBIGUOUS_OPERATING_DATE_MATCH))
    }

    @Test
    fun shouldMatchWithValuesOfHighestPrioritySalesAuthorityNumberWhenOverlappingSalesServiceJourneysDisagree() {
        val prioritizingMatcher = SalesTimetableMatcher(salesData, timetableCalendar, listOf("999", "74", "275"))

        assertThat(prioritizingMatcher.match(railServiceJourney("SJ:1", listOf("300"), "SE:100:DayType:10nov")))
            .isEqualTo(ServiceJourneyMatch("SJ:1", MatchOutcome.MATCHED, SalesValues("IC", "74")))
    }

    @Test
    fun shouldKeepProductCodeOfPrioritizedSalesAuthorityNumberWhateverThePriorityOrder() {
        val matcherPrioritizing275 = SalesTimetableMatcher(salesData, timetableCalendar, listOf("275", "74"))
        val matcherPrioritizing74 = SalesTimetableMatcher(salesData, timetableCalendar, listOf("74", "275"))

        assertThat(matcherPrioritizing275.match(railServiceJourney("SJ:1", listOf("300"), "SE:100:DayType:10nov")).salesValues)
            .isEqualTo(SalesValues("LT", "275"))
        assertThat(matcherPrioritizing74.match(railServiceJourney("SJ:1", listOf("300"), "SE:100:DayType:10nov")).salesValues)
            .isEqualTo(SalesValues("IC", "74"))
    }

    @Test
    fun shouldNotMatchWhenNoDisagreeingSalesAuthorityNumberHasPriority() {
        val prioritizingMatcher = SalesTimetableMatcher(salesData, timetableCalendar, listOf("999"))

        assertThat(prioritizingMatcher.match(railServiceJourney("SJ:1", listOf("300"), "SE:100:DayType:10nov")).outcome)
            .isEqualTo(MatchOutcome.AMBIGUOUS_OPERATING_DATE_MATCH)
    }

    @Test
    fun shouldNotMatchWhenHighestPrioritySalesAuthorityNumberHasDifferentProductCodes() {
        val prioritizingMatcher = SalesTimetableMatcher(salesData, timetableCalendar, listOf("279"))

        assertThat(prioritizingMatcher.match(railServiceJourney("SJ:1", listOf("200"), "SE:100:DayType:10nov")).outcome)
            .isEqualTo(MatchOutcome.AMBIGUOUS_OPERATING_DATE_MATCH)
    }

    @Test
    fun shouldNotMatchWhenOperatingDatesDoNotOverlap() {
        assertThat(matcher.match(railServiceJourney("SJ:1", listOf("100"), "SE:100:DayType:1dec")).outcome)
            .isEqualTo(MatchOutcome.NO_OPERATING_DATE_OVERLAP)
    }

    @Test
    fun shouldNotMatchWhenTimetableServiceJourneyHasNoOperatingDates() {
        assertThat(matcher.match(railServiceJourney("SJ:1", listOf("100"), "SE:100:DayType:unknown")).outcome)
            .isEqualTo(MatchOutcome.WITHOUT_OPERATING_DATES)
    }

    @Test
    fun shouldNotMatchWhenTrainNrIsMissingRepeatedOrUnknown() {
        assertThat(matcher.match(railServiceJourney("SJ:1", emptyList(), "SE:100:DayType:10nov")).outcome)
            .isEqualTo(MatchOutcome.WITHOUT_TRAIN_NR)
        assertThat(matcher.match(railServiceJourney("SJ:2", listOf("100", "200"), "SE:100:DayType:10nov")).outcome)
            .isEqualTo(MatchOutcome.MULTIPLE_TRAIN_NRS)
        assertThat(matcher.match(railServiceJourney("SJ:3", listOf("999"), "SE:100:DayType:10nov")).outcome)
            .isEqualTo(MatchOutcome.TRAIN_NR_NOT_IN_SALES)
    }

    @Test
    fun shouldCountEachOutcomeInSummary() {
        val matches =
            listOf(
                ServiceJourneyMatch("SJ:1", MatchOutcome.MATCHED, SalesValues("LT", "279")),
                ServiceJourneyMatch("SJ:2", MatchOutcome.MATCHED, SalesValues("LT", "279")),
                ServiceJourneyMatch("SJ:3", MatchOutcome.AMBIGUOUS_OPERATING_DATE_MATCH),
                ServiceJourneyMatch("SJ:4", MatchOutcome.MULTIPLE_TRAIN_NRS),
            )

        assertThat(SalesTimetableMatcher.MatchSummary.of(matches)).isEqualTo(
            SalesTimetableMatcher.MatchSummary(railServiceJourneys = 4, matched = 2, ambiguousOperatingDateMatch = 1, multipleTrainNrs = 1),
        )
    }

    private fun railServiceJourney(
        serviceJourneyId: String,
        trainNrs: List<String>,
        dayTypeRef: String,
    ) = TimetableRailServiceJourney(serviceJourneyId, trainNrs, listOf(dayTypeRef))

    private fun dateOnlyCalendar(vararg datesByDayTypeId: Pair<String, List<String>>) =
        SharedCalendarData(
            dayTypes = emptyMap(),
            operatingPeriods = emptyMap(),
            dayTypeAssignments = emptyMap(),
            dayTypeDateAssignments =
                datesByDayTypeId.associate { (dayTypeId, dates) ->
                    dayTypeId to dates.map { DayTypeDateAssignment(LocalDate.parse(it), isAvailable = true) }
                },
        )

    private fun salesServiceJourney(
        serviceJourneyId: String,
        trainNr: String,
        dayTypeRef: String,
        productCode: String,
        salesAuthorityNumber: String = "279",
    ) = SalesServiceJourney(
        serviceJourneyId = serviceJourneyId,
        productCode = productCode,
        salesAuthorityNumber = salesAuthorityNumber,
        dayTypeRefs = listOf(dayTypeRef),
        trainNr = trainNr,
    )
}
