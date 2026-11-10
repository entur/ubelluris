package org.entur.ror.ubelluris.sales

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.entur.ror.ubelluris.config.CliConfig
import org.entur.ror.ubelluris.config.TimetableFilterFeatures
import org.entur.ror.ubelluris.model.SalesData
import org.entur.ror.ubelluris.model.SalesServiceJourney
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SalesDataServiceTest {
    @TempDir
    lateinit var salesDir: Path

    private val salesDataService = SalesDataService(cliConfig())

    @Test
    fun shouldNotCollectSalesDataWhenFeatureIsDisabled() {
        writeLineFile(salesDir, "line_813_3294.xml", lineId = "SE:030:Line:1", transportMode = "rail", trainNr = "3294")
        val salesDataServiceWithoutSales = SalesDataService(cliConfig(useSalesData = false))

        val result = salesDataServiceWithoutSales.process(salesDir)

        assertThat(result).isEqualTo(SalesData.EMPTY)
    }

    @Test
    fun shouldReturnEmptySalesDataWithoutSalesDirWhenFeatureIsDisabled() {
        val salesDataServiceWithoutSales = SalesDataService(cliConfig(useSalesData = false))

        assertThat(salesDataServiceWithoutSales.process(null)).isEqualTo(SalesData.EMPTY)
    }

    @Test
    fun shouldFailWithoutSalesDirWhenFeatureIsEnabled() {
        assertThatThrownBy { salesDataService.process(null) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun shouldMapRailServiceJourneyFromLineFile() {
        writeLineFile(salesDir, "line_813_3294.xml", lineId = "SE:030:Line:1", transportMode = "rail", trainNr = "3294")

        val result = salesDataService.processLineFiles(salesDir)

        assertThat(result).containsExactlyEntriesOf(
            mapOf(
                "3294" to
                    listOf(
                        SalesServiceJourney(
                            serviceJourneyId = "SE:030:ServiceJourney:3294",
                            productCode = "LT",
                            salesAuthorityNumber = "279",
                            dayTypeRefs = listOf("SE:030:DayType:A", "SE:030:DayType:B"),
                            trainNr = "3294",
                        ),
                    ),
            ),
        )
    }

    @Test
    fun shouldSkipServiceJourneyWhenLineHasNoSalesAuthorityNumber() {
        writeLineFile(
            salesDir,
            "line_a.xml",
            lineId = "SE:030:Line:1",
            transportMode = "rail",
            trainNr = "100",
            salesAuthorityNumber = null,
        )

        val result = salesDataService.processLineFiles(salesDir)

        assertThat(result).isEmpty()
    }

    @Test
    fun shouldSkipServiceJourneysOnNonRailLines() {
        writeLineFile(salesDir, "line_380_651.xml", lineId = "SE:030:Line:1", transportMode = "bus", trainNr = null)

        val result = salesDataService.processLineFiles(salesDir)

        assertThat(result).isEmpty()
    }

    @Test
    fun shouldSkipReplacementBusServiceJourneysOnRailLines() {
        writeLineFile(
            salesDir,
            "line_380_300.xml",
            lineId = "SE:030:Line:1",
            transportMode = "rail",
            trainNr = null,
            serviceJourneyTransportMode = "bus",
        )

        val result = salesDataService.processLineFiles(salesDir)

        assertThat(result).isEmpty()
    }

    @Test
    fun shouldSkipRailServiceJourneyWithoutTrainNumber() {
        writeLineFile(salesDir, "line_without_train_number.xml", lineId = "SE:030:Line:1", transportMode = "rail", trainNr = null)

        val result = salesDataService.processLineFiles(salesDir)

        assertThat(result).isEmpty()
    }

    @Test
    fun shouldSkipServiceJourneyWithoutProductCode() {
        writeLineFile(salesDir, "line_a.xml", lineId = "SE:030:Line:1", transportMode = "rail", trainNr = "100", productCode = null)

        val result = salesDataService.processLineFiles(salesDir)

        assertThat(result).isEmpty()
    }

    @Test
    fun shouldGroupServiceJourneysFromDifferentFilesByTrainNr() {
        writeLineFile(salesDir, "line_a.xml", lineId = "SE:030:Line:1", transportMode = "rail", trainNr = "100")
        writeLineFile(salesDir, "line_b.xml", lineId = "SE:030:Line:2", transportMode = "rail", trainNr = "100")

        val result = salesDataService.processLineFiles(salesDir)

        assertThat(result.keys).containsExactly("100")
        assertThat(result.getValue("100")).hasSize(2)
    }

    @Test
    fun shouldIgnoreSharedDataFiles() {
        writeLineFile(salesDir, "_shared_data.xml", lineId = "SE:030:Line:1", transportMode = "rail", trainNr = "100")

        val result = salesDataService.processLineFiles(salesDir)

        assertThat(result).isEmpty()
    }

    private fun cliConfig(useSalesData: Boolean = true) =
        CliConfig(
            sourceCodespace = "SE:050",
            targetCodespace = "SAM",
            timetableFilterFeatures = TimetableFilterFeatures(useSalesData = useSalesData),
        )
}
