package file

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalDate
import java.util.zip.ZipInputStream

class HttpFileFetcher(
    private val url: String,
    private val downloadDir: Path = Path.of("downloads")
) : FileFetcher {

    override fun fetch(): Path {
        val today = LocalDate.now()
        val outputPath = downloadDir.resolve("${today}_stop_places.xml")

        Files.createDirectories(downloadDir)

        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept-Encoding", "gzip")
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())

        if (response.statusCode() != 200) {
            error("Failed to fetch file. HTTP ${response.statusCode()}: ${String(response.body())}")
        }

        val zipBytes = response.body()

        ZipInputStream(zipBytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".xml")) {
                    Files.write(
                        outputPath,
                        zip.readBytes(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                    )
                    return outputPath
                }
                entry = zip.nextEntry
            }
        }

        error("No XML file found in ZIP.")
    }
}