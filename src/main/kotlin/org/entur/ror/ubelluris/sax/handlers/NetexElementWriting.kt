package org.entur.ror.ubelluris.sax.handlers

import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.xml.sax.Attributes
import org.xml.sax.helpers.AttributesImpl

// netex-tools parses without namespace awareness, so localName can be empty and only qName has the element name
internal fun elementNameOf(
    localName: String?,
    qName: String?,
): String? = localName?.takeIf { it.isNotEmpty() } ?: qName?.substringAfter(':')

internal fun prefixOf(qName: String?): String? = qName?.substringBefore(':', missingDelimiterValue = "")?.takeIf { it.isNotEmpty() }

internal fun attributesOf(vararg nameAndValues: Pair<String, String>): Attributes =
    AttributesImpl().apply { nameAndValues.forEach { (name, value) -> addAttribute("", name, name, "CDATA", value) } }

/** Writes a new element with [attributes], in the namespace and with the prefix of the surrounding element. */
internal fun DelegatingXMLElementWriter.writeElement(
    uri: String?,
    prefix: String?,
    localName: String,
    attributes: Attributes = AttributesImpl(),
    writeContent: () -> Unit = {},
) {
    val qName = if (prefix == null) localName else "$prefix:$localName"
    startElement(uri, localName, qName, attributes)
    writeContent()
    endElement(uri, localName, qName)
}

internal fun DelegatingXMLElementWriter.writeText(text: String) = characters(text.toCharArray(), 0, text.length)
