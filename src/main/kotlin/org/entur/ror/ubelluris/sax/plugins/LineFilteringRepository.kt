package org.entur.ror.ubelluris.sax.plugins

/**
 * Repository that stores Line IDs that should be removed by line filter plugins.
 */
data class LineFilteringRepository(
    val lineIdsToRemove: MutableSet<String> = mutableSetOf(),
) {
    fun addLineId(lineId: String) {
        lineIdsToRemove.add(lineId)
    }
}
