package org.entur.ror.ubelluris.data

import org.entur.netex.tools.lib.model.Entity
import org.entur.netex.tools.lib.model.PublicationEnumeration

object TestDataFactory {
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