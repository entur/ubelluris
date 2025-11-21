package org.entur.ror.ubelluris.filter

import org.entur.netex.tools.pipeline.app.FilterNetexApp
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class FilterService(
    private val filterConfig: StandardImportFilterConfig = StandardImportFilterConfig()
) : XmlProcessor {
    override fun process(inputFile: Path): Path {
        val outputFile = inputFile.parent.resolve(inputFile.fileName.toString().replace(".xml", "_filtered.xml"))
        return filter(inputFile, outputFile)
    }

    fun filter(inputFile: Path, outputFile: Path): Path {
        val inputDir = inputFile.parent
        val outputDir = outputFile.parent

        Files.createDirectories(outputDir)

        FilterNetexApp(
            filterConfig = filterConfig.build(),
            input = inputDir.toFile(),
            target = outputDir.toFile(),
        ).run()

        val filteredFile = Files.list(outputDir)
            .filter { it.fileName.toString().endsWith(".xml") }
            .max(Comparator.comparing { Files.getLastModifiedTime(it).toMillis() })
            .orElseThrow { IllegalStateException("No filtered file produced.") }

        if (filteredFile != outputFile) {
            Files.move(filteredFile, outputFile, StandardCopyOption.REPLACE_EXISTING)
        }

        return outputFile
    }
}