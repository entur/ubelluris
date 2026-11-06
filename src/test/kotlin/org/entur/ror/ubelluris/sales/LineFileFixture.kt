package org.entur.ror.ubelluris.sales

import java.nio.file.Files
import java.nio.file.Path

/** Writes a minimal Samtrafiken line file with one Line and one ServiceJourney to [directory]. */
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
) {
    val productCodeKeyValueXml =
        productCode?.let { "<KeyValue><Key>productCode</Key><Value>$it</Value></KeyValue>" }.orEmpty()
    val lineKeyListXml =
        salesAuthorityNumber
            ?.let { "<keyList><KeyValue><Key>salesAuthorityNumber</Key><Value>$it</Value></KeyValue></keyList>" }
            .orEmpty()
    val serviceJourneyTransportModeXml =
        serviceJourneyTransportMode?.let { "<TransportMode>$it</TransportMode>" }.orEmpty()
    val trainNumbersXml =
        trainNr
            ?.let {
                """<trainNumbers><TrainNumberRef ref="SE:030:TrainNumber:$it" version="any"/></trainNumbers>"""
            }.orEmpty()
    val trainNumberDefinitionsXml =
        trainNr
            ?.let {
                """<trainNumbers><TrainNumber id="SE:030:TrainNumber:$it"><ForAdvertisement>$it</ForAdvertisement></TrainNumber></trainNumbers>"""
            }.orEmpty()

    Files.writeString(
        directory.resolve(fileName),
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
          <dataObjects>
            <CompositeFrame id="SE:030:CompositeFrame:1">
              <frames>
                <ServiceFrame id="SE:030:ServiceFrame:1">
                  <routes>
                    <Route id="SE:030:Route:1">
                      <LineRef ref="$lineId"/>
                    </Route>
                  </routes>
                  <lines>
                    <Line id="$lineId">
                      $lineKeyListXml
                      <TransportMode>$transportMode</TransportMode>
                      <RepresentedByGroupRef ref="SE:030:Network:813"/>
                    </Line>
                  </lines>
                  <journeyPatterns>
                    <JourneyPattern id="SE:030:JourneyPattern:1">
                      <RouteRef ref="SE:030:Route:1"/>
                    </JourneyPattern>
                  </journeyPatterns>
                </ServiceFrame>
                <TimetableFrame id="SE:030:TimetableFrame:1">
                  <vehicleJourneys>
                    <ServiceJourney id="$serviceJourneyId">
                      <keyList>
                        $productCodeKeyValueXml
                        <KeyValue><Key>fingerprint</Key><Value>abc</Value></KeyValue>
                      </keyList>
                      $serviceJourneyTransportModeXml
                      <dayTypes>
                        <DayTypeRef ref="SE:030:DayType:A"/>
                        <DayTypeRef ref="SE:030:DayType:B"/>
                      </dayTypes>
                      <JourneyPatternRef ref="SE:030:JourneyPattern:1"/>
                      $trainNumbersXml
                    </ServiceJourney>
                  </vehicleJourneys>
                  $trainNumberDefinitionsXml
                </TimetableFrame>
              </frames>
            </CompositeFrame>
          </dataObjects>
        </PublicationDelivery>
        """.trimIndent(),
    )
}
