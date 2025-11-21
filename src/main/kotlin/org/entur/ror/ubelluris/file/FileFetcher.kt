package org.entur.ror.ubelluris.file

import java.nio.file.Path

interface FileFetcher {
    fun fetch(): Path
}