package org.entur.ror.ubelluris.file

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun zipDirectory(
    sourceDir: Path,
    targetZip: Path,
    exclude: (Path) -> Boolean = { false },
): Path {
    ZipOutputStream(Files.newOutputStream(targetZip)).use { zip ->
        Files.walk(sourceDir)
            .filter { Files.isRegularFile(it) && !exclude(it) }
            .forEach { file ->
                zip.putNextEntry(ZipEntry(file.fileName.toString()))
                Files.copy(file, zip)
                zip.closeEntry()
            }
    }
    return targetZip
}
