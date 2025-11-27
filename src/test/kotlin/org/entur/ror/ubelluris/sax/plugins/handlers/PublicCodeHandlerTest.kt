package org.entur.ror.ubelluris.sax.plugins.handlers

import org.assertj.core.api.Assertions.assertThat
import org.entur.netex.tools.lib.model.Entity
import org.entur.netex.tools.lib.model.PublicationEnumeration
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.sax.plugins.PublicCodeParsingContext
import org.entur.ror.ubelluris.sax.plugins.PublicCodeRepository
import org.junit.jupiter.api.Test

class PublicCodeHandlerTest {
    val context = PublicCodeParsingContext()

    val publicCodeRepository = PublicCodeRepository()

    val publicCodeHandler = PublicCodeHandler(publicCodeRepository)

    @Test
    fun shouldAddEntityIdIfBlacklistedPublicCode() {
        val quayEntity = defaultEntity(id = "entityId", type = NetexTypes.QUAY)

        publicCodeHandler.startElement(context, null, quayEntity)
        publicCodeHandler.characters(context, "81".toCharArray(), 0, 2)
        publicCodeHandler.endElement(context, quayEntity)

        assertThat(publicCodeRepository.entityIds).containsOnly("entityId")
    }

    @Test
    fun shouldNotAddEntityIdIfBlacklistedPublicCode() {
        val quayEntity = defaultEntity(id = "entityId", type = NetexTypes.QUAY)

        publicCodeHandler.startElement(context, null, quayEntity)
        publicCodeHandler.characters(context, "A".toCharArray(), 0, 1)
        publicCodeHandler.endElement(context, quayEntity)

        assertThat(publicCodeRepository.entityIds).isEmpty()
    }

    fun defaultEntity(
        id: String,
        type: String = "testType",
        publication: String = PublicationEnumeration.PUBLIC.toString().lowercase(),
        parent: Entity? = null
    ): Entity = Entity(
        id = id,
        type = type,
        publication = publication,
        parent = parent,
    )
}