package org.entur.ror.ubelluris.publish

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class LocalFilePublisher(
    private val resultsDir: Path = Path.of("results")
) : FilePublisher {

    override fun publish(file: Path): Path {
        Files.createDirectories(resultsDir)

        val targetFile = resultsDir.resolve(file.fileName)

        Files.move(
            file,
            targetFile,
            StandardCopyOption.REPLACE_EXISTING
        )

        return targetFile
    }
}
