package file

import java.nio.file.Path

interface FileFetcher {
    fun fetch(): Path
}