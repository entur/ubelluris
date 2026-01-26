package org.entur.ror.ubelluris.sax.handlers

import org.assertj.core.api.Assertions.assertThat
import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.entur.ror.ubelluris.model.NetexTypes
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.xml.sax.Attributes
import org.xml.sax.helpers.AttributesImpl

class CodespaceIdHandlerTest {
    private val handler = CodespaceIdHandler()

    private val writer = mock<DelegatingXMLElementWriter>()

    @Test
    fun shouldReplaceCodespaceId() {
        val attrs: Attributes = mock()
        whenever(attrs.getValue("id")).thenReturn("SE:050")

        handler.startElement(null, "Codespace", "Codespace", attrs, writer)

        argumentCaptor<AttributesImpl>().apply {
            verify(writer).startElement(
                eq(null),
                eq(NetexTypes.CODESPACE),
                eq(NetexTypes.CODESPACE),
                capture()
            )

            val rewrittenId = firstValue.getValue("id")
            assertThat(rewrittenId).isEqualTo("SAM")
        }

    }
}