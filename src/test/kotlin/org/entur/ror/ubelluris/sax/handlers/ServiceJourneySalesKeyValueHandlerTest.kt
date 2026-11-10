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

class ServiceJourneySalesKeyValueHandlerTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var inputDir: Path
    private lateinit var outputDir: Path
    private val namespace = Namespace.getNamespace("http://www.netex.org.uk/netex")

    // Sales train 100 runs on Monday 2026-11-02, which is a date of timetable DayType "mondays"
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
        assertThat(timetableFilterConfig.serviceJourneySalesKeyValueHandler.matchSummary)
            .isEqualTo(SalesTimetableMatcher.MatchSummary(railServiceJourneys = 1, matched = 1))
    }

    @Test
    fun shouldNotChangeServiceJourneyWithoutMatch() {
        writeTimetableLineFile("line_unmatched.xml", "SE:100:Line:1", trainNr = "100", dayTypeRef = "SE:100:DayType:datesOnly")

        filter()

        assertThat(readServiceJourney("line_unmatched.xml").getChild("keyList", namespace)).isNull()
        assertThat(timetableFilterConfig.serviceJourneySalesKeyValueHandler.matchSummary)
            .isEqualTo(SalesTimetableMatcher.MatchSummary(railServiceJourneys = 1, noOperatingDateOverlap = 1))
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
        assertThat(serviceJourney.children.map { it.name }.take(3)).containsExactly("ValidBetween", "keyList", "dayTypes")
        assertThat(keyValues(serviceJourney)).containsExactly("productCode" to "RE", "salesAuthorityNumber" to "74")
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

    private fun filter() {
        NetexProcessor(filterConfig = timetableFilterConfig.build()).run(inputDir.toFile(), outputDir.toFile())
    }

    private fun readServiceJourney(fileName: String): Element =
        SAXBuilder()
            .build(outputDir.resolve(fileName).toFile())
            .rootElement
            .getDescendants(Filters.element("ServiceJourney", namespace))
            .single()

    // A list, not a map, so that a duplicate Key is visible
    private fun keyValues(serviceJourney: Element): List<Pair<String, String>> =
        serviceJourney
            .getChild("keyList", namespace)
            .getChildren("KeyValue", namespace)
            .map { it.getChildText("Key", namespace) to it.getChildText("Value", namespace) }
}
