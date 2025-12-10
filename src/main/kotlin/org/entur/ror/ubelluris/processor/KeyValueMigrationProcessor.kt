package org.entur.ror.ubelluris.processor

import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.input.SAXBuilder
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.slf4j.LoggerFactory
import java.io.File

class KeyValueMigrationProcessor {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val ignoredStopPlaceKeys = setOf(
        "trafikverket-name",
        "stip.StopArea.DefaultInterchangeDurationSeconds",
        "local-name",
        "local-number",
        "sellable",
        "preliminary",
        "stip.StopPoint.ExistsFromDate",
        "stip.StopPoint.ExistsUpToDate",
        "local-journeypatternpoint-gid",
        "local-designation"
    )

    private val ns = Namespace.getNamespace("http://www.netex.org.uk/netex")

    fun process(xmlFile: File): File {
        logger.info("Running key value migration processing on ${xmlFile.name}")

        val doc = SAXBuilder().build(xmlFile)

        processStopPlaces(doc)
        processQuays(doc)

        writeDocument(doc, xmlFile)

        logger.info("Done running key value migration processing")
        return xmlFile
    }

    private fun processStopPlaces(doc: Document) {
        val stopPlaces = doc.rootElement.descendants
            .filterIsInstance<Element>()
            .filter { it.name == "StopPlace" }

        for (stopPlace in stopPlaces) {
            val keyList = stopPlace.getChild("keyList", ns) ?: continue

            val toRemove = mutableListOf<Element>()
            val importedIds = mutableListOf<String>()

            for (kv in keyList.getChildren("KeyValue", ns)) {
                val key = kv.getChildText("Key", ns)?.trim().orEmpty()
                val value = kv.getChildText("Value", ns)?.trim().orEmpty()

                when (key) {
                    "owner" -> {
                        val padded = padCodespace(value)
                        kv.getChild("Value", ns).text = padded
                    }

                    "data-from" -> {
                        val padded = padCodespace(value)
                        kv.getChild("Value", ns).text = padded
                    }

                    "local-gid" -> {
                        val parts = value.split("|").map {
                            val (prefix, rest) = it.split(":", limit = 2)
                            "${padCodespace(prefix)}:$rest"
                        }
                        importedIds += parts
                        toRemove += kv
                    }

                    in ignoredStopPlaceKeys -> {
                        toRemove += kv
                    }
                }
            }

            toRemove.forEach { keyList.removeContent(it) }

            stopPlace.getAttributeValue("id")?.let { origId ->
                importedIds += stripCodespace(origId)
            }

            if (importedIds.isNotEmpty()) {
                val kv = Element("KeyValue", ns)
                kv.addContent(Element("Key", ns).setText("imported-id"))
                kv.addContent(Element("Value", ns).setText(importedIds.joinToString(",")))
                keyList.addContent(kv)
            }

        }
    }

    private fun processQuays(doc: Document) {
        val quays = doc.rootElement.descendants
            .filterIsInstance<Element>()
            .filter { it.name == "Quay" }

        val now = java.time.LocalDate.now()

        for (quay in quays.toList()) {
            val keyList = quay.getChild("keyList", ns) ?: continue

            var removeQuay = false
            val importedIds = mutableListOf<String>()
            val toRemove = mutableListOf<Element>()

            for (kv in keyList.getChildren("KeyValue", ns)) {
                val key = kv.getChildText("Key", ns)?.trim().orEmpty()
                val value = kv.getChildText("Value", ns)?.trim().orEmpty()

                when (key) {
                    "stip.StopPoint.ExistsUpToDate" -> {
                        if (value.isNotBlank()) {
                            try {
                                val date = java.time.LocalDate.parse(value)
                                if (date.isBefore(now)) {
                                    removeQuay = true
                                    break
                                }
                            } catch (_: Exception) {
                            }
                        }
                        toRemove += kv
                    }

                    "local-stoppoint-gid" -> {
                        val parts = value.split("|").map {
                            val (prefix, rest) = it.split(":", limit = 2)
                            "${padCodespace(prefix)}:$rest"
                        }
                        importedIds += parts
                        toRemove += kv
                    }

                    "stip.StopPoint.ExistsFromDate",
                    "local-journeypatternpoint-gid",
                    "local-designation" -> {
                        toRemove += kv
                    }
                }
            }

            if (removeQuay) {
                quay.parentElement?.removeContent(quay)
                continue
            }

            toRemove.forEach { keyList.removeContent(it) }

            quay.getAttributeValue("id")?.let { origId ->
                val replaced = stripCodespace(origId)
                importedIds += replaced
            }

            if (importedIds.isNotEmpty()) {
                val kv = Element("KeyValue", ns)
                kv.addContent(Element("Key", ns).setText("imported-id"))
                kv.addContent(Element("Value", ns).setText(importedIds.joinToString(",")))
                keyList.addContent(kv)
            }
        }
    }


    private fun writeDocument(doc: Document, file: File) {
        val out = XMLOutputter()
        out.format = Format.getPrettyFormat().setEncoding("UTF-8")
        file.outputStream().use { out.output(doc, it) }
    }

    private fun padCodespace(s: String): String = s.padStart(3, '0')
    private fun stripCodespace(s: String): String = s.substringAfter(":")
}
