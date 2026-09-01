package org.entur.ror.ubelluris.sax.plugins.data

/**
 * Registry that tracks locally defined entity IDs to distinguish them from external references.
 * Used by VersionRefNormalizerPlugin to identify when versionRef is incorrectly used for local entities.
 */
class LocalEntityRegistry {
    private val localEntityIds = mutableSetOf<String>()

    /**
     * Register an entity ID as locally defined in the current document.
     */
    fun registerLocalEntity(id: String) {
        localEntityIds.add(id)
    }

    /**
     * Check if an entity ID is defined locally in the current document.
     * @return true if the entity is defined locally, false if it's external
     */
    fun isLocalEntity(id: String): Boolean = localEntityIds.contains(id)

    /**
     * Get all locally registered entity IDs.
     */
    fun getLocalEntityIds(): Set<String> = localEntityIds.toSet()

    /**
     * Clear all registered entity IDs (useful for processing new documents).
     */
    fun clear() {
        localEntityIds.clear()
    }
}
