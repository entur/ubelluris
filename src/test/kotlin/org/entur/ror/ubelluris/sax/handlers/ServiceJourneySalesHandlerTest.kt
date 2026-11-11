package org.entur.ror.ubelluris.sax.handlers

import org.assertj.core.api.Assertions.assertThat
import org.entur.netex.tools.lib.NetexProcessor
import org.entur.ror.ubelluris.config.CliConfig
import org.entur.ror.ubelluris.filter.TimetableFilterConfig
import org.entur.ror.ubelluris.model.DayTypeDateAssignment
import org.entur.ror.ubelluris.model.SalesData
import org.entur.ror.ubelluris.model.SalesServiceJourney
import org.entur.ror.ubelluris.model.SharedCalendarData
import org.entur.ror.ubelluris.model.TransportMode
import org.entur.ror.ubelluris.sales.SalesTimetableMatcher
import org.entur.ror.ubelluris.sales.writeLineFile
import org.entur.ror.ubelluris.sales.writeTimetableSharedDataFile
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.filter.Filters
import org.jdom2.input.SAXBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate

/**
 * Runs the real TimetableFilterConfig on a timetable with the calendar of [writeTimetableSharedDataFile].
 * Timetable DayType "mondays" operates only on 2026-11-02 (2026-11-09 is excluded, and ToDate 2026-11-16 is exclusive).
 */
class ServiceJourneySalesHandlerTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var inputDir: Path
    private lateinit var outputDir: Path
    private val namespace = Namespace.getNamespace("http://www.netex.org.uk/netex")

    // Sales train 100 runs on Monday 2026-11-02, which is the date of timetable DayType "mondays"
    private val salesData =
        SalesData(
            trainNrServiceJourneysMap =
                mapOf(
                    "100" to
                        listOf(
                            SalesServiceJourney(
                                serviceJourneyId = "SE:030:ServiceJourney:sales100",
                                productCode = "RE",
                                salesAuthorityNumber = "74",
                                dayTypeRefs = listOf("SE:030:DayType:2nov"),
                                trainNr = "100",
                            ),
                        ),
                ),
            calendarData =
                SharedCalendarData(
                    dayTypes = emptyMap(),
                    operatingPeriods = emptyMap(),
                    dayTypeAssignments = emptyMap(),
                    dayTypeDateAssignments =
                        mapOf("SE:030:DayType:2nov" to listOf(DayTypeDateAssignment(LocalDate.parse("2026-11-02"), isAvailable = true))),
                ),
        )

    private val timetableFilterConfig =
        TimetableFilterConfig(
            CliConfig(sourceCodespace = "SE", targetCodespace = "SE", transportModes = listOf(TransportMode.BUS)),
            salesData,
        )

    @BeforeEach
    fun createDirectories() {
        inputDir = Files.createDirectories(tempDir.resolve("input"))
        outputDir = Files.createDirectories(tempDir.resolve("output"))
        writeTimetableSharedDataFile(inputDir)
    }

    @Test
    fun shouldWriteSalesKeyListAsFirstChildOfMatchedServiceJourney() {
        writeTimetableLineFile("line_matched.xml", "SE:100:Line:1", trainNr = "100", dayTypeRef = "SE:100:DayType:mondays")

        filter()

        val serviceJourney = readServiceJourney("line_matched.xml")
        assertThat(serviceJourney.children.first().name).isEqualTo("keyList")
        assertThat(keyValues(serviceJourney)).containsExactly("productCode" to "RE", "salesAuthorityNumber" to "74")
        assertThat(timetableFilterConfig.salesEnrichmentPlan.matchSummary)
            .isEqualTo(SalesTimetableMatcher.MatchSummary(railServiceJourneys = 1, matched = 1))
    }

    @Test
    fun shouldReplaceDayTypesOfMatchedServiceJourneyWithDatedServiceJourneyPerOperatingDate() {
        writeTimetableLineFile("line_matched.xml", "SE:100:Line:1", trainNr = "100", dayTypeRef = "SE:100:DayType:mondays")

        filter()

        val serviceJourney = readServiceJourney("line_matched.xml")
        assertThat(serviceJourney.getChild("dayTypes", namespace)).isNull()

        val vehicleJourneys = serviceJourney.parentElement
        assertThat(vehicleJourneys.children.map { it.name }).containsExactly("ServiceJourney", "DatedServiceJourney")
        val datedServiceJourney = vehicleJourneys.getChild("DatedServiceJourney", namespace)
        assertThat(datedServiceJourney.getAttributeValue("id")).isEqualTo("SE:100:DatedServiceJourney:line_matched.xml_2026-11-02")
        assertThat(datedServiceJourney.getAttributeValue("version")).isEqualTo("1")
        assertThat(datedServiceJourney.children.map { it.name }).containsExactly("ServiceJourneyRef", "OperatingDayRef")
        assertThat(datedServiceJourney.getChild("ServiceJourneyRef", namespace).getAttributeValue("ref"))
            .isEqualTo("SE:100:ServiceJourney:line_matched.xml")
        assertThat(datedServiceJourney.getChild("OperatingDayRef", namespace).getAttributeValue("ref"))
            .isEqualTo("SE:100:OperatingDay:2026-11-02")
    }

    @Test
    fun shouldWriteOperatingDaysAndRemoveCalendarDataOnlyUsedByConvertedServiceJourneys() {
        writeTimetableLineFile("line_matched.xml", "SE:100:Line:1", trainNr = "100", dayTypeRef = "SE:100:DayType:mondays")

        filter()

        val serviceCalendarFrame = readSharedServiceCalendarFrame()
        assertThat(serviceCalendarFrame.children.map { it.name }).containsExactly("dayTypes", "operatingDays", "dayTypeAssignments")
        val operatingDay = serviceCalendarFrame.getChild("operatingDays", namespace).getChildren("OperatingDay", namespace).single()
        assertThat(operatingDay.getAttributeValue("id")).isEqualTo("SE:100:OperatingDay:2026-11-02")
        assertThat(operatingDay.getChildText("CalendarDate", namespace)).isEqualTo("2026-11-02")

        // DayType "datesOnly" was not used by the converted ServiceJourney, so it and its assignment stay
        assertThat(ids(serviceCalendarFrame, "DayType")).containsExactly("SE:100:DayType:datesOnly")
        assertThat(ids(serviceCalendarFrame, "DayTypeAssignment")).containsExactly("SE:100:DayTypeAssignment:datesOnly_261110")
        assertThat(ids(serviceCalendarFrame, "OperatingPeriod")).isEmpty()
    }

    @Test
    fun shouldNotChangeServiceJourneyWithoutMatchOrItsCalendarData() {
        writeTimetableLineFile("line_unmatched.xml", "SE:100:Line:1", trainNr = "100", dayTypeRef = "SE:100:DayType:datesOnly")

        filter()

        val serviceJourney = readServiceJourney("line_unmatched.xml")
        assertThat(serviceJourney.getChild("keyList", namespace)).isNull()
        assertThat(serviceJourney.getChild("dayTypes", namespace)).isNotNull()
        assertThat(serviceJourney.parentElement.getChildren("DatedServiceJourney", namespace)).isEmpty()

        val serviceCalendarFrame = readSharedServiceCalendarFrame()
        assertThat(serviceCalendarFrame.getChild("operatingDays", namespace)).isNull()
        assertThat(ids(serviceCalendarFrame, "DayType")).containsExactlyInAnyOrder("SE:100:DayType:mondays", "SE:100:DayType:datesOnly")
        assertThat(timetableFilterConfig.salesEnrichmentPlan.matchSummary)
            .isEqualTo(SalesTimetableMatcher.MatchSummary(railServiceJourneys = 1, noOperatingDateOverlap = 1))
    }

    @Test
    fun shouldNotMatchAnyServiceJourneyWithoutSalesData() {
        writeTimetableLineFile("line_matched.xml", "SE:100:Line:1", trainNr = "100", dayTypeRef = "SE:100:DayType:mondays")
        val unpatchedFilterConfig =
            TimetableFilterConfig(
                CliConfig(sourceCodespace = "SE", targetCodespace = "SE", transportModes = listOf(TransportMode.BUS)),
                salesData.forCarriers(emptySet()),
            )

        filter(unpatchedFilterConfig)

        val serviceJourney = readServiceJourney("line_matched.xml")
        assertThat(serviceJourney.getChild("keyList", namespace)).isNull()
        assertThat(serviceJourney.getChild("dayTypes", namespace)).isNotNull()
        assertThat(unpatchedFilterConfig.salesEnrichmentPlan.matchSummary).isEqualTo(SalesTimetableMatcher.MatchSummary())
    }

    @Test
    fun shouldAddKeyValuesToEndOfExistingKeyList() {
        writeTimetableLineFile(
            "line_with_key_list.xml",
            "SE:100:Line:1",
            trainNr = "100",
            dayTypeRef = "SE:100:DayType:mondays",
            serviceJourneyKeyList = true,
        )

        filter()

        val serviceJourney = readServiceJourney("line_with_key_list.xml")
        assertThat(serviceJourney.getChildren("keyList", namespace)).hasSize(1)
        assertThat(keyValues(serviceJourney)).containsExactly("fingerprint" to "abc", "productCode" to "RE", "salesAuthorityNumber" to "74")
    }

    @Test
    fun shouldSkipSalesKeyThatIsAlreadyInKeyList() {
        writeTimetableLineFile(
            "line_with_product_code.xml",
            "SE:100:Line:1",
            trainNr = "100",
            dayTypeRef = "SE:100:DayType:mondays",
            serviceJourneyKeyList = true,
            existingProductCode = "OLD",
        )

        filter()

        assertThat(keyValues(readServiceJourney("line_with_product_code.xml")))
            .containsExactly("productCode" to "OLD", "fingerprint" to "abc", "salesAuthorityNumber" to "74")
    }

    @Test
    fun shouldWriteNewKeyListAfterElementsThatComeBeforeIt() {
        writeTimetableLineFile(
            "line_with_valid_between.xml",
            "SE:100:Line:1",
            trainNr = "100",
            dayTypeRef = "SE:100:DayType:mondays",
            serviceJourneyValidBetween = true,
        )

        filter()

        val serviceJourney = readServiceJourney("line_with_valid_between.xml")
        assertThat(serviceJourney.children.map { it.name }.take(2)).containsExactly("ValidBetween", "keyList")
        assertThat(keyValues(serviceJourney)).containsExactly("productCode" to "RE", "salesAuthorityNumber" to "74")
    }

    @Test
    fun shouldWriteProductCodeOfPrioritizedSalesAuthorityNumberWhenSalesValuesDisagree() {
        // Sales train 100 has a second sales ServiceJourney on the same date, with other sales values
        val disagreeingSalesData =
            salesData.copy(
                trainNrServiceJourneysMap =
                    mapOf(
                        "100" to
                            salesData.trainNrServiceJourneysMap.getValue("100") +
                            SalesServiceJourney(
                                serviceJourneyId = "SE:030:ServiceJourney:sales100_lt",
                                productCode = "LT",
                                salesAuthorityNumber = "275",
                                dayTypeRefs = listOf("SE:030:DayType:2nov"),
                                trainNr = "100",
                            ),
                    ),
            )
        val prioritizingFilterConfig =
            TimetableFilterConfig(
                CliConfig(
                    sourceCodespace = "SE",
                    targetCodespace = "SE",
                    transportModes = listOf(TransportMode.BUS),
                    salesAuthorityNumberPriority = listOf("275", "74"),
                ),
                disagreeingSalesData,
            )
        writeTimetableLineFile("line_matched.xml", "SE:100:Line:1", trainNr = "100", dayTypeRef = "SE:100:DayType:mondays")

        filter(prioritizingFilterConfig)

        assertThat(keyValues(readServiceJourney("line_matched.xml")))
            .containsExactly("productCode" to "LT", "salesAuthorityNumber" to "275")
    }

    private fun writeTimetableLineFile(
        fileName: String,
        lineId: String,
        trainNr: String,
        dayTypeRef: String,
        serviceJourneyKeyList: Boolean = false,
        serviceJourneyValidBetween: Boolean = false,
        // timetable ServiceJourneys have no productCode of their own by default
        existingProductCode: String? = null,
    ) = writeLineFile(
        inputDir,
        fileName,
        lineId = lineId,
        transportMode = "rail",
        trainNr = trainNr,
        serviceJourneyId = "SE:100:ServiceJourney:$fileName",
        dayTypeRefs = listOf(dayTypeRef),
        serviceJourneyKeyList = serviceJourneyKeyList,
        serviceJourneyValidBetween = serviceJourneyValidBetween,
        productCode = existingProductCode,
    )

    private fun filter(filterConfig: TimetableFilterConfig = timetableFilterConfig) {
        NetexProcessor(filterConfig = filterConfig.build()).run(inputDir.toFile(), outputDir.toFile())
    }

    private fun readServiceJourney(fileName: String): Element = readElements(fileName, "ServiceJourney").single()

    private fun readSharedServiceCalendarFrame(): Element = readElements("_shared_data.xml", "ServiceCalendarFrame").single()

    private fun readElements(
        fileName: String,
        elementName: String,
    ): List<Element> =
        SAXBuilder()
            .build(outputDir.resolve(fileName).toFile())
            .rootElement
            .getDescendants(Filters.element(elementName, namespace))
            .toList()

    private fun ids(
        parent: Element,
        elementName: String,
    ): List<String> = parent.getDescendants(Filters.element(elementName, namespace)).map { it.getAttributeValue("id") }

    // A list, not a map, so that a duplicate Key is visible
    private fun keyValues(serviceJourney: Element): List<Pair<String, String>> =
        serviceJourney
            .getChild("keyList", namespace)
            .getChildren("KeyValue", namespace)
            .map { it.getChildText("Key", namespace) to it.getChildText("Value", namespace) }
}
