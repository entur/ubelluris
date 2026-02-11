package org.entur.ror.ubelluris.timetable.fetch

import com.google.cloud.storage.Storage
import org.entur.ror.ubelluris.timetable.config.TimetableConfig
import org.entur.ror.ubelluris.timetable.model.TimetableData
import org.jdom2.filter.Filters
import org.jdom2.input.SAXBuilder
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.util.zip.ZipInputStream

class GcsTimetableFetcher(
    private val config: TimetableConfig,
    private val storage: Storage,
    private val inputBucketName: String
) : TimetableFetcher {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun fetch(providers: List<String>): Map<String, TimetableData> {
        logger.info("Fetching timetables for providers: $providers")

        Files.createDirectories(config.helperDir)

        return providers.associateWith { provider ->
            fetchProviderData(provider)
        }
    }

    private fun fetchProviderData(provider: String): TimetableData {
        logger.info("Fetching timetable data for provider: $provider")

        val today = LocalDate.now()
        val blobPath = "${today.year}/${"%02d".format(today.monthValue)}/${"%02d".format(today.dayOfMonth)}/$provider.zip"
        logger.info("Reading timetable from GCS: $inputBucketName/$blobPath")

        val blob = storage.get(inputBucketName, blobPath)
            ?: throw RuntimeException("Blob not found: $inputBucketName/$blobPath")

        val zipBytes = blob.getContent()

        return extractAndFilter(provider, zipBytes)
    }

    private fun extractAndFilter(provider: String, zipBytes: ByteArray): TimetableData {
        logger.info("Extracting and filtering timetable files for provider: $provider")

        val providerHelperDir = config.helperDir.resolve(provider)
        Files.createDirectories(providerHelperDir)

        val allFiles = mutableListOf<Path>()
        val modeHelperFiles = mutableListOf<Path>()
        val blacklistPatterns = config.blacklist[provider] ?: emptyList()

        ZipInputStream(zipBytes.inputStream()).use { zipStream ->
            var entry = zipStream.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".xml", ignoreCase = true)) {
                    if (isBlacklisted(entry.name, blacklistPatterns)) {
                        logger.info("Skipping blacklisted file: ${entry.name}")
                        entry = zipStream.nextEntry
                        continue
                    }

                    val fileName = Path.of(entry.name).fileName.toString()
                    val extractedFile = providerHelperDir.resolve(fileName)

                    val tempContent = zipStream.readBytes()
                    Files.write(extractedFile, tempContent)
                    allFiles.add(extractedFile)

                    if (containsRelevantModes(tempContent)) {
                        modeHelperFiles.add(extractedFile)
                        logger.info("File contains relevant modes: $fileName")
                    }
                }
                entry = zipStream.nextEntry
            }
        }

        logger.info("Extracted ${allFiles.size} files, ${modeHelperFiles.size} contain relevant modes")

        return TimetableData(
            provider = provider,
            modeHelperFiles = modeHelperFiles,
            allFiles = allFiles
        )
    }

    private fun isBlacklisted(fileName: String, patterns: List<String>): Boolean {
        return patterns.any { pattern ->
            val regex = pattern.replace("*", ".*").toRegex()
            regex.matches(fileName)
        }
    }

    private fun containsRelevantModes(xmlContent: ByteArray): Boolean {
        return try {
            val saxBuilder = SAXBuilder()
            val document = saxBuilder.build(xmlContent.inputStream())
            val root = document.rootElement
            val namespace = root.namespace

            val lines = root.getDescendants(Filters.element("Line", namespace))

            lines.forEach { lineElement ->
                val transportModeElement = lineElement.getChild("TransportMode", namespace)
                if (transportModeElement != null) {
                    val modeText = transportModeElement.text
                    config.modeFilter.forEach { mode ->
                        if (mode.netexValue.equals(modeText, ignoreCase = true)) {
                            return true
                        }
                    }
                }
            }

            false
        } catch (e: Exception) {
            logger.warn("Failed to parse XML for mode filtering", e)
            false
        }
    }
}
