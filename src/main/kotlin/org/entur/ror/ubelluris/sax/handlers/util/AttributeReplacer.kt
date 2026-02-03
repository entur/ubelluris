package org.entur.ror.ubelluris.sax.handlers.util

import org.xml.sax.Attributes

class AttributeReplacer(
    private val sourceCodespace: String,
    private val targetCodespace: String
) {
    fun replaceAttribute(attributes: Attributes?, qName: String): String {
        return attributes?.getValue(qName)?.replace(sourceCodespace, targetCodespace) ?: ""
    }
}