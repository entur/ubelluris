package org.entur.ror.ubelluris.publish

import java.nio.file.Path

interface FilePublisher {
    fun publish(file: Path): Path
}