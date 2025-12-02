package org.entur.ror.ubelluris.sax.handlers.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.xml.sax.Attributes

class AttributeReplacerTest {

    @Test
    fun shouldReplaceSE050WithSAM() {
        val attrs: Attributes = mock()
        whenever(attrs.getValue("id")).thenReturn("SE:050:StopPlace:123")

        val result = AttributeReplacer.replaceAttribute(attrs, "id")

        assertThat(result).isEqualTo("SAM:StopPlace:123")
    }

    @Test
    fun shouldReturnUnchangedValueWhenSE050NotPresent() {
        val attrs: Attributes = mock()
        whenever(attrs.getValue("name")).thenReturn("Some:Other:Value")

        val result = AttributeReplacer.replaceAttribute(attrs, "name")

        assertThat(result).isEqualTo("Some:Other:Value")
    }

    @Test
    fun shouldReturnEmptyStringWhenAttributeDoesNotExist() {
        val attrs: Attributes = mock()
        whenever(attrs.getValue("nonexistent")).thenReturn(null)

        val result = AttributeReplacer.replaceAttribute(attrs, "nonexistent")

        assertThat(result).isEmpty()
    }

    @Test
    fun shouldReturnEmptyStringWhenAttributeIsNull() {
        val result = AttributeReplacer.replaceAttribute(null, "id")

        assertThat(result).isEmpty()
    }

    @Test
    fun shouldReturnEmptyStringWhenAttributeValueIsEmpty() {
        val attrs: Attributes = mock()
        whenever(attrs.getValue("id")).thenReturn("")

        val result = AttributeReplacer.replaceAttribute(attrs, "id")

        assertThat(result).isEmpty()
    }

    @Test
    fun shouldHandleAttributeWithOnlySE050Value() {
        val attrs: Attributes = mock()
        whenever(attrs.getValue("id")).thenReturn("SE:050")

        val result = AttributeReplacer.replaceAttribute(attrs, "id")

        assertThat(result).isEqualTo("SAM")
    }
}