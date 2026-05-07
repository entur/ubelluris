package org.entur.ror.ubelluris.file

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

fun zipDirectory(
    sourceDir: Path,
    targetZip: Path,
    exclude: (Path) -> Boolean = { false },
): Path {
    ZipOutputStream(Files.newOutputStream(targetZip)).use { zip ->
        Files
            .walk(sourceDir)
            .filter { Files.isRegularFile(it) && !exclude(it) }
            .forEach { file ->
                zip.putNextEntry(ZipEntry(file.fileName.toString()))
                Files.copy(file, zip)
                zip.closeEntry()
            }
    }
    return targetZip
}

fun extractXmlFromZip(
    zipPath: Path,
    outputPath: Path,
): Path {
    ZipInputStream(Files.newInputStream(zipPath)).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
            if (!entry.isDirectory && entry.name.endsWith(".xml")) {
                Files.createDirectories(outputPath.parent)
                Files.write(outputPath, zip.readBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
                return outputPath
            }
            entry = zip.nextEntry
        }
    }
    error("No XML file found in ZIP: $zipPath")
}

fun extractZipToDirectory(
    zipPath: Path,
    outputDir: Path,
): Path {
    ZipInputStream(Files.newInputStream(zipPath)).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
            if (!entry.isDirectory && entry.name.endsWith(".xml")) {
                Files.write(
                    outputDir.resolve(Path.of(entry.name).fileName),
                    zip.readBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                )
            }
            entry = zip.nextEntry
        }
    }
    return outputDir
}
