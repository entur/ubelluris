package org.entur.ror.ubelluris.filter

import org.entur.netex.tools.pipeline.app.FilterNetexApp
import org.entur.ror.ubelluris.processor.KeyValueMigrationProcessor
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class FilterService(
    private val filterConfig: StandardImportFilterConfig = StandardImportFilterConfig(),
    private val resultsDir: Path = Path.of("results")
) : XmlProcessor {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun process(inputFile: Path): Path {
        val outputFile = resultsDir.resolve(
            inputFile.fileName.toString().replace(".xml", "_filtered.xml")
        )
        return filter(inputFile, outputFile)
    }

    fun filter(inputFile: Path, outputFile: Path): Path {
        Files.createDirectories(resultsDir)

        val tempDir = Files.createTempDirectory("ubelluris-filter-")
        logger.info("Temp processing dir: $tempDir")

        val tempInputFile = tempDir.resolve(inputFile.fileName)
        Files.copy(inputFile, tempInputFile, StandardCopyOption.REPLACE_EXISTING)

        Files.list(tempDir)
            .filter { it.toString().endsWith(".xml") }
            .forEach { xmlPath ->
                KeyValueMigrationProcessor().process(xmlPath.toFile())
            }

        FilterNetexApp(
            filterConfig = filterConfig.build(),
            input = tempDir.toFile(),
            target = tempDir.toFile(),
        ).run()

        val filteredTempFile = Files.list(tempDir)
            .filter { it.fileName.toString().endsWith(".xml") }
            .max(Comparator.comparing { Files.getLastModifiedTime(it).toMillis() })
            .orElseThrow { IllegalStateException("No filtered file produced.") }

        Files.copy(filteredTempFile, outputFile, StandardCopyOption.REPLACE_EXISTING)
        tempDir.toFile().deleteRecursively()

        return outputFile
    }
}
