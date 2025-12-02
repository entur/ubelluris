package org.entur.ror.ubelluris.sax.handlers.util

import org.xml.sax.Attributes

object AttributeReplacer {

    fun replaceAttribute(attributes: Attributes?, qName: String): String {
        return attributes?.getValue(qName)?.replace("SE:050", "SAM") ?: ""
    }
}