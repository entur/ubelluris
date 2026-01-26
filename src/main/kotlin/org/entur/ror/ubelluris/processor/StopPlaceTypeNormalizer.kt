package org.entur.ror.ubelluris.processor

import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.input.SAXBuilder
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.slf4j.LoggerFactory
import java.io.File

class StopPlaceTypeNormalizer {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val ns = Namespace.getNamespace("http://www.netex.org.uk/netex")

    fun process(xmlFile: File): File {
        logger.info("Running StopPlaceType normalization on ${xmlFile.name}")

        val doc = SAXBuilder().build(xmlFile)
        processStopPlaces(doc)
        writeDocument(doc, xmlFile)

        logger.info("Done running StopPlaceType normalization")
        return xmlFile
    }

    private fun processStopPlaces(doc: Document) {
        val stopPlaces = doc.rootElement.descendants
            .filterIsInstance<Element>()
            .filter { it.name == "StopPlace" }

        for (stopPlace in stopPlaces) {
            normalizeTransportMode(stopPlace)
            normalizeStopPlaceType(stopPlace)
        }
    }

    private fun normalizeTransportMode(stopPlace: Element) {
        val transportMode = stopPlace.getChild("TransportMode", ns)

        if (transportMode != null && transportMode.text?.trim() == "other") {
            transportMode.text = "bus"
            logger.debug("Normalized TransportMode 'other' -> 'bus' for ${stopPlace.getAttributeValue("id")}")
        }
    }

    private fun normalizeStopPlaceType(stopPlace: Element) {
        val stopPlaceType = stopPlace.getChild("StopPlaceType", ns)
        val transportMode = stopPlace.getChild("TransportMode", ns)

        if (stopPlaceType == null) {
            return
        }

        val currentType = stopPlaceType.text?.trim()

        if (currentType == "other") {
            val transportModeValue = transportMode?.text?.trim()
            val normalizedType = when (transportModeValue) {
                "water" -> "ferryStop"
                "tram" -> "onstreetTram"
                else -> "onstreetBus"
            }
            stopPlaceType.text = normalizedType
            logger.debug(
                "Normalized StopPlaceType 'other' -> '$normalizedType' (TransportMode='$transportModeValue') for ${
                    stopPlace.getAttributeValue(
                        "id"
                    )
                }"
            )
            return
        }

        if (currentType == "tramStation") {
            stopPlaceType.text = "onstreetTram"
            logger.debug("Normalized StopPlaceType 'tramStation' -> 'onstreetTram' for ${stopPlace.getAttributeValue("id")}")
            return
        }

        if (transportMode != null && transportMode.text?.trim() == "bus") {
            val quays = stopPlace.getChild("quays", ns)?.getChildren("Quay", ns) ?: emptyList()
            val quayCount = quays.size

            if (quayCount < 6 && currentType == "busStation") {
                stopPlaceType.text = "onstreetBus"
                logger.debug(
                    "Normalized StopPlaceType 'busStation' -> 'onstreetBus' (${quayCount} quays) for ${
                        stopPlace.getAttributeValue(
                            "id"
                        )
                    }"
                )
            } else if (quayCount >= 6 && currentType == "onstreetBus") {
                stopPlaceType.text = "busStation"
                logger.debug(
                    "Normalized StopPlaceType 'onstreetBus' -> 'busStation' (${quayCount} quays) for ${
                        stopPlace.getAttributeValue(
                            "id"
                        )
                    }"
                )
            }
        }
    }

    private fun writeDocument(doc: Document, file: File) {
        val out = XMLOutputter()
        out.format = Format.getPrettyFormat().setEncoding("UTF-8")
        file.outputStream().use { out.output(doc, it) }
    }
}
