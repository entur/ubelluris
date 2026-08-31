package org.entur.ror.ubelluris.sax.plugins

import net.logstash.logback.argument.StructuredArguments.kv
import org.entur.netex.tools.lib.model.Entity
import org.entur.netex.tools.lib.plugin.AbstractNetexPlugin
import org.entur.ror.ubelluris.model.NetexTypes
import org.slf4j.LoggerFactory
import org.xml.sax.Attributes

/**
 * Plugin that filters out Line entities based on regex patterns matching their PublicCode.
 * When a Line's PublicCode matches any of the provided regex patterns, the Line is marked
 * for removal and stored in the LineFilteringRepository.
 *
 * @property regexPatterns List of regex patterns to match against Line PublicCode elements
 */
class LinePublicCodeFilterPlugin(
    private val regexPatterns: List<Regex>,
) : AbstractNetexPlugin() {
    private val logger = LoggerFactory.getLogger(javaClass)
    val repository = LineFilteringRepository()

    private val linePublicCodeHandler = LinePublicCodeHandler(regexPatterns, repository, logger)

    override fun getName(): String = "LinePublicCodeFilterPlugin"

    override fun getDescription(): String = "Filters out Lines based on regex patterns matching their PublicCode"

    override fun getSupportedElementTypes(): Set<String> =
        setOf(
            NetexTypes.LINE,
            NetexTypes.PUBLIC_CODE,
        )

    override fun startElement(
        elementName: String,
        attributes: Attributes?,
        currentEntity: Entity?,
    ) {
        currentEntity?.let { entity ->
            linePublicCodeHandler.startElement(elementName, entity)
        }
    }

    override fun characters(
        elementName: String,
        ch: CharArray?,
        start: Int,
        length: Int,
    ) {
        linePublicCodeHandler.characters(elementName, ch, start, length)
    }

    override fun endElement(
        elementName: String,
        currentEntity: Entity?,
    ) {
        currentEntity?.let { _ ->
            linePublicCodeHandler.endElement(elementName)
        }
    }

    override fun getCollectedData(): LineFilteringRepository = repository
}

/**
 * Repository that stores Line IDs that should be removed based on regex filtering.
 */
data class LineFilteringRepository(
    val lineIdsToRemove: MutableSet<String> = mutableSetOf(),
) {
    fun addLineId(lineId: String) {
        lineIdsToRemove.add(lineId)
    }
}

/**
 * Handler that processes Line and PublicCode elements to identify Lines for removal.
 * When processing a Line element, it captures the Line ID.
 * When processing a PublicCode element within that Line, it checks if the code matches
 * any of the regex patterns and marks the Line for removal if there's a match.
 */
private class LinePublicCodeHandler(
    private val regexPatterns: List<Regex>,
    private val repository: LineFilteringRepository,
    private val logger: org.slf4j.Logger,
) {
    private val stringBuilder = StringBuilder()
    private var currentLineId: String? = null
    private var isProcessingPublicCode = false

    fun startElement(
        elementName: String,
        currentEntity: Entity,
    ) {
        when (elementName) {
            NetexTypes.LINE -> {
                // Capture the Line ID when entering a Line element
                currentLineId = currentEntity.id
                isProcessingPublicCode = false
            }
            NetexTypes.PUBLIC_CODE -> {
                // Check if we're inside a Line element
                if (currentEntity.type == NetexTypes.LINE) {
                    isProcessingPublicCode = true
                    stringBuilder.clear()
                }
            }
        }
    }

    fun characters(
        elementName: String,
        ch: CharArray?,
        start: Int,
        length: Int,
    ) {
        if (elementName == NetexTypes.PUBLIC_CODE && isProcessingPublicCode && ch != null) {
            stringBuilder.appendRange(ch, start, start + length)
        }
    }

    fun endElement(elementName: String) {
        when (elementName) {
            NetexTypes.PUBLIC_CODE -> {
                if (isProcessingPublicCode && currentLineId != null) {
                    val publicCode = stringBuilder.toString().trim()

                    // Check if publicCode matches any regex pattern
                    val matchingPattern = regexPatterns.firstOrNull { it.matches(publicCode) }

                    if (matchingPattern != null) {
                        repository.addLineId(currentLineId!!)
                        logger.info(
                            "Filtering Line with PublicCode matching pattern",
                            kv("lineId", currentLineId),
                            kv("publicCode", publicCode),
                            kv("pattern", matchingPattern.pattern),
                        )
                    }

                    stringBuilder.clear()
                    isProcessingPublicCode = false
                }
            }
            NetexTypes.LINE -> {
                // Clear state when leaving Line element
                currentLineId = null
                isProcessingPublicCode = false
            }
        }
    }
}
