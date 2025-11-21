package org.entur.ror.ubelluris.sax.plugins

class PublicCodeRepository(
    val types: MutableMap<String, String> = mutableMapOf(),
) {

    fun getType(dayTypeId: String, input: String): String =
        types.getOrPut(dayTypeId) { input }

}