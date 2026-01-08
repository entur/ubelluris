package org.entur.ror.ubelluris.processor

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class StopPlaceTypeNormalizerTest {

    private val processor = StopPlaceTypeNormalizer()

    @Test
    fun shouldNormalizeTransportModeFromOtherToBus(@TempDir tempDir: Path) {
        val inputXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <dataObjects>
                    <SiteFrame>
                        <stopPlaces>
                            <StopPlace id="SAM:StopPlace:1">
                                <TransportMode>other</TransportMode>
                                <StopPlaceType>onstreetBus</StopPlaceType>
                            </StopPlace>
                        </stopPlaces>
                    </SiteFrame>
                </dataObjects>
            </PublicationDelivery>
        """.trimIndent()

        val xmlFile = tempDir.resolve("test.xml").toFile()
        xmlFile.writeText(inputXml)

        processor.process(xmlFile)

        val result = xmlFile.readText()
        assertThat(result).contains("<TransportMode>bus</TransportMode>")
    }

    @Test
    fun shouldNormalizeStopPlaceTypeFromOtherToOnstreetBus(@TempDir tempDir: Path) {
        val inputXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <dataObjects>
                    <SiteFrame>
                        <stopPlaces>
                            <StopPlace id="SAM:StopPlace:1">
                                <TransportMode>bus</TransportMode>
                                <StopPlaceType>other</StopPlaceType>
                            </StopPlace>
                        </stopPlaces>
                    </SiteFrame>
                </dataObjects>
            </PublicationDelivery>
        """.trimIndent()

        val xmlFile = tempDir.resolve("test.xml").toFile()
        xmlFile.writeText(inputXml)

        processor.process(xmlFile)

        val result = xmlFile.readText()
        assertThat(result).contains("<StopPlaceType>onstreetBus</StopPlaceType>")
    }

    @Test
    fun shouldNormalizeStopPlaceTypeFromTramStationToOnstreetTram(@TempDir tempDir: Path) {
        val inputXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <dataObjects>
                    <SiteFrame>
                        <stopPlaces>
                            <StopPlace id="SAM:StopPlace:1">
                                <TransportMode>tram</TransportMode>
                                <StopPlaceType>tramStation</StopPlaceType>
                            </StopPlace>
                        </stopPlaces>
                    </SiteFrame>
                </dataObjects>
            </PublicationDelivery>
        """.trimIndent()

        val xmlFile = tempDir.resolve("test.xml").toFile()
        xmlFile.writeText(inputXml)

        processor.process(xmlFile)

        val result = xmlFile.readText()
        assertThat(result).contains("<StopPlaceType>onstreetTram</StopPlaceType>")
    }

    @Test
    fun shouldNormalizeBusStationToOnstreetBusWhenLessThan6Quays(@TempDir tempDir: Path) {
        val inputXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <dataObjects>
                    <SiteFrame>
                        <stopPlaces>
                            <StopPlace id="SAM:StopPlace:1">
                                <TransportMode>bus</TransportMode>
                                <StopPlaceType>busStation</StopPlaceType>
                                <quays>
                                    <Quay id="SAM:Quay:1"/>
                                    <Quay id="SAM:Quay:2"/>
                                    <Quay id="SAM:Quay:3"/>
                                    <Quay id="SAM:Quay:4"/>
                                    <Quay id="SAM:Quay:5"/>
                                </quays>
                            </StopPlace>
                        </stopPlaces>
                    </SiteFrame>
                </dataObjects>
            </PublicationDelivery>
        """.trimIndent()

        val xmlFile = tempDir.resolve("test.xml").toFile()
        xmlFile.writeText(inputXml)

        processor.process(xmlFile)

        val result = xmlFile.readText()
        assertThat(result).contains("<StopPlaceType>onstreetBus</StopPlaceType>")
    }

    @Test
    fun shouldNormalizeOnstreetBusToBusStationWhen6OrMoreQuays(@TempDir tempDir: Path) {
        val inputXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <dataObjects>
                    <SiteFrame>
                        <stopPlaces>
                            <StopPlace id="SAM:StopPlace:1">
                                <TransportMode>bus</TransportMode>
                                <StopPlaceType>onstreetBus</StopPlaceType>
                                <quays>
                                    <Quay id="SAM:Quay:1"/>
                                    <Quay id="SAM:Quay:2"/>
                                    <Quay id="SAM:Quay:3"/>
                                    <Quay id="SAM:Quay:4"/>
                                    <Quay id="SAM:Quay:5"/>
                                    <Quay id="SAM:Quay:6"/>
                                </quays>
                            </StopPlace>
                        </stopPlaces>
                    </SiteFrame>
                </dataObjects>
            </PublicationDelivery>
        """.trimIndent()

        val xmlFile = tempDir.resolve("test.xml").toFile()
        xmlFile.writeText(inputXml)

        processor.process(xmlFile)

        val result = xmlFile.readText()
        assertThat(result).contains("<StopPlaceType>busStation</StopPlaceType>")
    }

    @Test
    fun shouldKeepBusStationWhen6OrMoreQuays(@TempDir tempDir: Path) {
        val inputXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <dataObjects>
                    <SiteFrame>
                        <stopPlaces>
                            <StopPlace id="SAM:StopPlace:1">
                                <TransportMode>bus</TransportMode>
                                <StopPlaceType>busStation</StopPlaceType>
                                <quays>
                                    <Quay id="SAM:Quay:1"/>
                                    <Quay id="SAM:Quay:2"/>
                                    <Quay id="SAM:Quay:3"/>
                                    <Quay id="SAM:Quay:4"/>
                                    <Quay id="SAM:Quay:5"/>
                                    <Quay id="SAM:Quay:6"/>
                                    <Quay id="SAM:Quay:7"/>
                                </quays>
                            </StopPlace>
                        </stopPlaces>
                    </SiteFrame>
                </dataObjects>
            </PublicationDelivery>
        """.trimIndent()

        val xmlFile = tempDir.resolve("test.xml").toFile()
        xmlFile.writeText(inputXml)

        processor.process(xmlFile)

        val result = xmlFile.readText()
        assertThat(result).contains("<StopPlaceType>busStation</StopPlaceType>")
    }

    @Test
    fun shouldNotChangeQuayCountLogicForNonBusMode(@TempDir tempDir: Path) {
        val inputXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <dataObjects>
                    <SiteFrame>
                        <stopPlaces>
                            <StopPlace id="SAM:StopPlace:1">
                                <TransportMode>tram</TransportMode>
                                <StopPlaceType>busStation</StopPlaceType>
                                <quays>
                                    <Quay id="SAM:Quay:1"/>
                                    <Quay id="SAM:Quay:2"/>
                                </quays>
                            </StopPlace>
                        </stopPlaces>
                    </SiteFrame>
                </dataObjects>
            </PublicationDelivery>
        """.trimIndent()

        val xmlFile = tempDir.resolve("test.xml").toFile()
        xmlFile.writeText(inputXml)

        processor.process(xmlFile)

        val result = xmlFile.readText()
        assertThat(result).contains("<StopPlaceType>busStation</StopPlaceType>")
    }

    @Test
    fun shouldHandleStopPlaceWithoutQuays(@TempDir tempDir: Path) {
        val inputXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <dataObjects>
                    <SiteFrame>
                        <stopPlaces>
                            <StopPlace id="SAM:StopPlace:1">
                                <TransportMode>bus</TransportMode>
                                <StopPlaceType>busStation</StopPlaceType>
                            </StopPlace>
                        </stopPlaces>
                    </SiteFrame>
                </dataObjects>
            </PublicationDelivery>
        """.trimIndent()

        val xmlFile = tempDir.resolve("test.xml").toFile()
        xmlFile.writeText(inputXml)

        processor.process(xmlFile)

        val result = xmlFile.readText()
        assertThat(result).contains("<StopPlaceType>onstreetBus</StopPlaceType>")
    }
}
