package org.entur.ror.ubelluris.sax.plugins

class PublicCodeRepository(
    val entityIds: MutableList<String> = mutableListOf()
) {

    fun addEntityId(entityId: String) {
        entityIds.add(entityId)
    }
}