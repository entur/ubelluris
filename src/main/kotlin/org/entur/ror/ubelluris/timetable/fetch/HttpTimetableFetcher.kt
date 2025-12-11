package org.entur.ror.ubelluris.timetable.fetch

import org.entur.ror.ubelluris.timetable.config.TimetableConfig
import org.entur.ror.ubelluris.timetable.model.TimetableData
import org.jdom2.filter.Filters
import org.jdom2.input.SAXBuilder
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream

/**
 * Downloads timetable ZIPs from API, caches them, and filters for relevant transport modes
 */
class HttpTimetableFetcher(
    private val config: TimetableConfig
) : TimetableFetcher {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    override fun fetch(providers: List<String>): Map<String, TimetableData> {
        logger.info("Fetching timetables for providers: $providers")

        Files.createDirectories(config.cacheDir)
        Files.createDirectories(config.helperDir)

        return providers.associateWith { provider ->
            fetchProviderData(provider)
        }
    }

    private fun fetchProviderData(provider: String): TimetableData {
        logger.info("Fetching timetable data for provider: $provider")

        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val cachedZip = config.cacheDir.resolve("${today}_${provider}.zip")

        if (!Files.exists(cachedZip)) {
            downloadTimetable(provider, cachedZip)
        } else {
            logger.info("Using cached timetable: $cachedZip")
        }

        return extractAndFilter(provider, cachedZip)
    }

    private fun downloadTimetable(provider: String, destination: Path) {
        val url = "${config.apiUrl}/$provider/$provider.zip?key=${config.apiKey}"
        logger.info("Downloading timetable from: ${url.replace(config.apiKey, "***")}")

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept-Encoding", "gzip")
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())

        if (response.statusCode() != 200) {
            throw RuntimeException("Failed to download timetable for $provider: HTTP ${response.statusCode()}")
        }

        Files.copy(response.body(), destination, StandardCopyOption.REPLACE_EXISTING)
        logger.info("Downloaded timetable to: $destination")
    }

    private fun extractAndFilter(provider: String, zipFile: Path): TimetableData {
        logger.info("Extracting and filtering timetable files for provider: $provider")

        val providerHelperDir = config.helperDir.resolve(provider)
        Files.createDirectories(providerHelperDir)

        val allFiles = mutableListOf<Path>()
        val modeHelperFiles = mutableListOf<Path>()
        val blacklistPatterns = config.blacklist[provider] ?: emptyList()

        ZipInputStream(Files.newInputStream(zipFile)).use { zipStream ->
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
