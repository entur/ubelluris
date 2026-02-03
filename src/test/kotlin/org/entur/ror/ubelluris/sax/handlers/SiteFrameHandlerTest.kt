package org.entur.ror.ubelluris.sax.handlers

import org.assertj.core.api.Assertions.assertThat
import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.sax.handlers.util.AttributeReplacer
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.xml.sax.Attributes
import org.xml.sax.helpers.AttributesImpl

class SiteFrameHandlerTest {
    private val attributeReplacer = AttributeReplacer("SE:050", "SAM")
    private val handler = SiteFrameHandler(attributeReplacer)
    private val writer = mock<DelegatingXMLElementWriter>()

    @Test
    fun testSiteFrameHandler() {
        val attrs: Attributes = mock()
        whenever(attrs.getValue("id")).thenReturn("SE:050:SiteFrame:1")
        whenever(attrs.getValue("version")).thenReturn("42")

        handler.startElement(null, "SiteFrame", "SiteFrame", attrs, writer)

        argumentCaptor<AttributesImpl>().apply {
            verify(writer).startElement(
                eq(null),
                eq(NetexTypes.SITE_FRAME),
                eq(NetexTypes.SITE_FRAME),
                capture()
            )

            val rewrittenId = firstValue.getValue("id")
            val version = firstValue.getValue("version")

            assertThat(rewrittenId).isEqualTo("SAM:SiteFrame:1")
            assertThat(version).isEqualTo("1")
        }
    }
}