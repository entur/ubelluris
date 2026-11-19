package org.entur.ror.ubelluris.model

import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.emptySalesData
import org.junit.jupiter.api.Test

class SalesDataTest {
    private val salesData =
        emptySalesData().copy(
            trainNrServiceJourneysMap =
                mapOf(
                    "100" to
                        listOf(salesServiceJourney("sj_100", "100", carrier = "74"), salesServiceJourney("lt_100", "100", carrier = "275")),
                    "200" to listOf(salesServiceJourney("lt_200", "200", carrier = "275")),
                ),
        )

    @Test
    fun shouldKeepOnlyServiceJourneysOfGivenCarriers() {
        val sjSalesData = salesData.forCarriers(setOf("74"))

        assertThat(sjSalesData.trainNrServiceJourneysMap.keys).containsExactly("100")
        assertThat(sjSalesData.trainNrServiceJourneysMap.getValue("100").map { it.serviceJourneyId }).containsExactly("sj_100")
        assertThat(sjSalesData.calendarData).isEqualTo(salesData.calendarData)
    }

    @Test
    fun shouldKeepNothingWithoutCarriers() {
        assertThat(salesData.forCarriers(emptySet()).trainNrServiceJourneysMap).isEmpty()
    }

    private fun salesServiceJourney(
        serviceJourneyId: String,
        trainNr: String,
        carrier: String,
    ) = SalesServiceJourney(
        serviceJourneyId = serviceJourneyId,
        productCode = "RE",
        salesAuthorityNumber = carrier,
        dayTypeRefs = emptyList(),
        trainNr = trainNr,
    )
}
