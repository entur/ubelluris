import file.FileFetcher
import filter.XmlProcessor
import publish.FilePublisher
import java.nio.file.Path

class UbellurisService(
    private val fetcher: FileFetcher,
    private val processor: XmlProcessor,
    private val publisher: FilePublisher
) {
    fun run(): Path {
        println("Starting Ubelluris pipeline...")

        val rawFile: Path = fetcher.fetch()
        println("Fetched file: $rawFile")

        val processedFile: Path = processor.process(rawFile)
        println("Processed file: $processedFile")

        val publishedFile: Path = publisher.publish(processedFile)
        println("Published file: $publishedFile")

        return publishedFile
    }
}