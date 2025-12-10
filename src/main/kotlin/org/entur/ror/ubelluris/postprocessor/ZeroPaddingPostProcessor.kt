package org.entur.ror.ubelluris.postprocessor

import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class ZeroPaddingPostProcessor {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val keysToZeroPad = setOf("owner", "data-from")

    fun process(xmlFile: File): File {
        logger.info("Zero-padding KeyValue elements in: ${xmlFile.name}")

        val docFactory = DocumentBuilderFactory.newInstance()
        val docBuilder = docFactory.newDocumentBuilder()
        val doc = docBuilder.parse(xmlFile)

        val keyValueElements = doc.getElementsByTagName("KeyValue")

        for (i in 0 until keyValueElements.length) {
            val keyValueElement = keyValueElements.item(i) as Element

            var keyElement: Element? = null
            var valueElement: Element? = null

            var child = keyValueElement.firstChild
            while (child != null) {
                if (child is Element) {
                    when (child.localName ?: child.nodeName) {
                        "Key" -> keyElement = child
                        "Value" -> valueElement = child
                    }
                }
                child = child.nextSibling
            }

            if (keyElement != null && valueElement != null) {
                val keyValue = keyElement.textContent?.trim() ?: ""

                if (keyValue in keysToZeroPad) {
                    val originalValue = valueElement.textContent?.trim() ?: ""
                    val paddedValue = if (originalValue.all { it.isDigit() }) {
                        originalValue.padStart(3, '0')
                    } else {
                        originalValue
                    }
                    valueElement.textContent = paddedValue
                }
            }
        }

        writeDocument(doc, xmlFile)

        logger.info("Finished zero-padding")

        return xmlFile
    }

    private fun writeDocument(doc: Document, outputFile: File) {
        val transformerFactory = TransformerFactory.newInstance()
        val transformer = transformerFactory.newTransformer()
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")

        val source = DOMSource(doc)
        val result = StreamResult(outputFile)
        transformer.transform(source, result)
    }
}
