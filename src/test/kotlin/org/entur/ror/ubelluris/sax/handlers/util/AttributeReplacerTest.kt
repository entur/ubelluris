package org.entur.ror.ubelluris.sax.handlers.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.xml.sax.Attributes

class AttributeReplacerTest {

    private val attributeReplacer = AttributeReplacer("SE:050", "SAM")

    @Test
    fun shouldReplaceSourceCodespaceWithTargetCodespace() {
        val attrs: Attributes = mock()
        whenever(attrs.getValue("id")).thenReturn("SE:050:StopPlace:123")

        val result = attributeReplacer.replaceAttribute(attrs, "id")

        assertThat(result).isEqualTo("SAM:StopPlace:123")
    }

    @Test
    fun shouldReturnUnchangedValueWhenNoMatchOnSourceCodespaceNotPresent() {
        val attrs: Attributes = mock()
        whenever(attrs.getValue("name")).thenReturn("Some:Other:Value")

        val result = attributeReplacer.replaceAttribute(attrs, "name")

        assertThat(result).isEqualTo("Some:Other:Value")
    }

    @Test
    fun shouldReturnEmptyStringWhenAttributeDoesNotExist() {
        val attrs: Attributes = mock()
        whenever(attrs.getValue("nonexistent")).thenReturn(null)

        val result = attributeReplacer.replaceAttribute(attrs, "nonexistent")

        assertThat(result).isEmpty()
    }

    @Test
    fun shouldReturnEmptyStringWhenAttributeIsNull() {
        val result = attributeReplacer.replaceAttribute(null, "id")

        assertThat(result).isEmpty()
    }

    @Test
    fun shouldReturnEmptyStringWhenAttributeValueIsEmpty() {
        val attrs: Attributes = mock()
        whenever(attrs.getValue("id")).thenReturn("")

        val result = attributeReplacer.replaceAttribute(attrs, "id")

        assertThat(result).isEmpty()
    }

    @Test
    fun shouldHandleAttributeWithOnlySourceCodespaceValueMatch() {
        val attrs: Attributes = mock()
        whenever(attrs.getValue("id")).thenReturn("SE:050")

        val result = attributeReplacer.replaceAttribute(attrs, "id")

        assertThat(result).isEqualTo("SAM")
    }

    @Test
    fun shouldReplaceWithCustomSourceAndTarget() {
        val customReplacer = AttributeReplacer("NO:123", "NSR")
        val attrs: Attributes = mock()
        whenever(attrs.getValue("id")).thenReturn("NO:123:StopPlace:456")

        val result = customReplacer.replaceAttribute(attrs, "id")

        assertThat(result).isEqualTo("NSR:StopPlace:456")
    }
}