package org.entur.ror.ubelluris.sales

import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration
import org.rutebanken.netex.model.DayTypeRefStructure
import org.rutebanken.netex.model.DayTypeRefs_RelStructure
import org.rutebanken.netex.model.GroupOfLinesRefStructure
import org.rutebanken.netex.model.JourneyPattern
import org.rutebanken.netex.model.JourneyPatternRefStructure
import org.rutebanken.netex.model.JourneyPatternsInFrame_RelStructure
import org.rutebanken.netex.model.JourneysInFrame_RelStructure
import org.rutebanken.netex.model.KeyListStructure
import org.rutebanken.netex.model.KeyValueStructure
import org.rutebanken.netex.model.Line
import org.rutebanken.netex.model.LineRefStructure
import org.rutebanken.netex.model.LinesInFrame_RelStructure
import org.rutebanken.netex.model.Route
import org.rutebanken.netex.model.RouteRefStructure
import org.rutebanken.netex.model.RoutesInFrame_RelStructure
import org.rutebanken.netex.model.ServiceFrame
import org.rutebanken.netex.model.ServiceJourney
import org.rutebanken.netex.model.TimetableFrame
import org.rutebanken.netex.model.TrainNumber
import org.rutebanken.netex.model.TrainNumberRefStructure
import org.rutebanken.netex.model.TrainNumberRefs_RelStructure
import org.rutebanken.netex.model.TrainNumbersInFrame_RelStructure
import org.rutebanken.netex.model.ValidBetween
import java.nio.file.Path
import java.time.LocalDateTime

/**
 * Writes a minimal Samtrafiken line file with one Line and one ServiceJourney to [directory].
 * Route and JourneyPattern ids are derived from [lineId], so files with different Lines can share a directory.
 * The XML is built with netex-java-model and written with [writePublicationDelivery].
 */
fun writeLineFile(
    directory: Path,
    fileName: String,
    lineId: String,
    transportMode: String,
    trainNr: String?,
    serviceJourneyTransportMode: String? = null,
    salesAuthorityNumber: String? = "279",
    productCode: String? = "LT",
    serviceJourneyId: String = "SE:030:ServiceJourney:${trainNr ?: "bus"}",
    dayTypeRefs: List<String> = listOf("SE:030:DayType:A", "SE:030:DayType:B"),
    serviceJourneyKeyList: Boolean = true,
    serviceJourneyValidBetween: Boolean = false,
) {
    val routeId = "$lineId:Route"
    val journeyPatternId = "$lineId:JourneyPattern"
    val trainNumberId = trainNr?.let { "SE:030:TrainNumber:$it" }

    val line =
        Line()
            .withId(lineId)
            .withVersion(TEST_NETEX_VERSION)
            .withTransportMode(AllVehicleModesOfTransportEnumeration.fromValue(transportMode))
            .withRepresentedByGroupRef(GroupOfLinesRefStructure().withRef("SE:030:Network:813"))
    salesAuthorityNumber?.let { line.withKeyList(keyList("salesAuthorityNumber" to it)) }

    val serviceJourney =
        ServiceJourney()
            .withId(serviceJourneyId)
            .withVersion(TEST_NETEX_VERSION)
            .withDayTypes(
                DayTypeRefs_RelStructure().withDayTypeRef(
                    dayTypeRefs.map { netexFactory.createDayTypeRef(DayTypeRefStructure().withRef(it)) },
                ),
            ).withJourneyPatternRef(netexFactory.createJourneyPatternRef(JourneyPatternRefStructure().withRef(journeyPatternId)))
    if (serviceJourneyValidBetween) {
        serviceJourney.withValidBetween(ValidBetween().withFromDate(LocalDateTime.parse("2026-11-01T00:00:00")))
    }
    if (serviceJourneyKeyList) {
        serviceJourney.withKeyList(keyList(*listOfNotNull(productCode?.let { "productCode" to it }, "fingerprint" to "abc").toTypedArray()))
    }
    serviceJourneyTransportMode?.let { serviceJourney.withTransportMode(AllVehicleModesOfTransportEnumeration.fromValue(it)) }
    trainNumberId?.let {
        serviceJourney.withTrainNumbers(TrainNumberRefs_RelStructure().withTrainNumberRef(TrainNumberRefStructure().withRef(it)))
    }

    val serviceFrame =
        ServiceFrame()
            .withId("SE:030:ServiceFrame:1")
            .withVersion(TEST_NETEX_VERSION)
            .withRoutes(
                RoutesInFrame_RelStructure().withRoute_(
                    netexFactory.createRoute(
                        Route()
                            .withId(routeId)
                            .withVersion(TEST_NETEX_VERSION)
                            .withLineRef(netexFactory.createLineRef(LineRefStructure().withRef(lineId))),
                    ),
                ),
            ).withLines(LinesInFrame_RelStructure().withLine_(netexFactory.createLine(line)))
            .withJourneyPatterns(
                JourneyPatternsInFrame_RelStructure().withJourneyPattern_OrJourneyPatternView(
                    netexFactory.createJourneyPattern(
                        JourneyPattern()
                            .withId(journeyPatternId)
                            .withVersion(TEST_NETEX_VERSION)
                            .withRouteRef(RouteRefStructure().withRef(routeId)),
                    ),
                ),
            )

    val timetableFrame =
        TimetableFrame()
            .withId("SE:030:TimetableFrame:1")
            .withVersion(TEST_NETEX_VERSION)
            .withVehicleJourneys(
                JourneysInFrame_RelStructure().withVehicleJourneyOrDatedVehicleJourneyOrNormalDatedVehicleJourney(serviceJourney),
            )
    trainNumberId?.let {
        timetableFrame.withTrainNumbers(
            TrainNumbersInFrame_RelStructure().withTrainNumberOrTrainNumberRef(
                TrainNumber().withId(it).withVersion(TEST_NETEX_VERSION).withForAdvertisement(trainNr),
            ),
        )
    }

    writePublicationDelivery(
        directory.resolve(fileName),
        "SE:030:CompositeFrame:1",
        netexFactory.createServiceFrame(serviceFrame),
        netexFactory.createTimetableFrame(timetableFrame),
    )
}

private fun keyList(vararg keyValues: Pair<String, String>): KeyListStructure =
    KeyListStructure().withKeyValue(keyValues.map { (key, value) -> KeyValueStructure().withKey(key).withValue(value) })
