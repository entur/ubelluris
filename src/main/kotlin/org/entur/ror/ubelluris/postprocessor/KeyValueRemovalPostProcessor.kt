package org.entur.ror.ubelluris.postprocessor

import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class KeyValueRemovalPostProcessor {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val blacklistedKeys = setOf(
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

    fun process(xmlFile: File): File {
        logger.info("Removing blacklisted KeyValues from: ${xmlFile.name}")

        val docFactory = DocumentBuilderFactory.newInstance()
        val docBuilder = docFactory.newDocumentBuilder()
        val doc = docBuilder.parse(xmlFile)

        val keyValueElements = doc.getElementsByTagName("KeyValue")
        val elementsToRemove = mutableListOf<Element>()

        for (i in 0 until keyValueElements.length) {
            val keyValueElement = keyValueElements.item(i) as Element

            val keyElements = keyValueElement.getElementsByTagName("Key")
            if (keyElements.length > 0) {
                val keyElement = keyElements.item(0) as Element
                val keyValue = keyElement.textContent?.trim() ?: ""

                if (keyValue in blacklistedKeys) {
                    elementsToRemove.add(keyValueElement)
                }
            }
        }

        elementsToRemove.forEach { element ->
            element.parentNode?.removeChild(element)
        }

        removeWhitespaceNodes(doc)
        writeDocument(doc, xmlFile)

        return xmlFile
    }

    private fun writeDocument(doc: Document, outputFile: File) {
        val transformerFactory = TransformerFactory.newInstance()
        val transformer = transformerFactory.newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")

        val source = DOMSource(doc)
        val result = StreamResult(outputFile)
        transformer.transform(source, result)
    }

    private fun removeWhitespaceNodes(node: Node) {
        val toRemove = mutableListOf<Node>()

        var child = node.firstChild
        while (child != null) {
            if (child.nodeType == Node.TEXT_NODE && child.textContent.trim().isEmpty()) {
                toRemove.add(child)
            } else {
                removeWhitespaceNodes(child)
            }
            child = child.nextSibling
        }

        toRemove.forEach { node.removeChild(it) }
    }

}
