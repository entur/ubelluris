package org.entur.ror.ubelluris.publish

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class LocalFilePublisher(
    private val storagePath: Path,
    private val resultsDir: Path = Path.of("results")
) : FilePublisher {

    override fun publish(file: Path): Path {
        val targetDir = resultsDir.resolve(storagePath)
        Files.createDirectories(targetDir)

        val targetFile = targetDir.resolve(file.fileName)

        Files.move(
            file,
            targetFile,
            StandardCopyOption.REPLACE_EXISTING
        )

        return targetFile
    }
}
