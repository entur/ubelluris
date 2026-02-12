package org.entur.ror.ubelluris.timetable.fetch

import com.google.cloud.storage.Blob
import com.google.cloud.storage.Storage
import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.model.TransportMode
import org.entur.ror.ubelluris.timetable.config.TimetableConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class GcsTimetableFetcherTest {

    @TempDir
    lateinit var tempDir: Path

    private val storage: Storage = mock()

    @Test
    fun shouldFetchRelevantTimetableDataForMultipleProviders() {
        val helperDir = tempDir.resolve("helper")
        val inputBucket = "test-input-bucket"

        val config = TimetableConfig(
            providers = listOf("provider1", "provider2"),
            modeFilter = setOf(TransportMode.TRAM, TransportMode.WATER),
            blacklist = emptyMap(),
            helperDir = helperDir
        )

        val today = LocalDate.now()
        val datePath = "${today.year}/${"%02d".format(today.monthValue)}/${"%02d".format(today.dayOfMonth)}"

        val provider1Zip = createZipBytes(
            mapOf(
                "line_p1_001.xml" to """
                <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                    <Line>
                        <TransportMode>tram</TransportMode>
                    </Line>
                </PublicationDelivery>
            """.trimIndent(),
                "line_p1_002.xml" to """
                <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                    <Line>
                        <TransportMode>bus</TransportMode>
                    </Line>
                </PublicationDelivery>
            """.trimIndent()
            )
        )

        val provider2Zip = createZipBytes(
            mapOf(
                "line_p2_001.xml" to """
                <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                    <Line>
                        <TransportMode>tram</TransportMode>
                    </Line>
                </PublicationDelivery>
            """.trimIndent(),
                "line_p2_002.xml" to """
                <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                    <Line>
                        <TransportMode>water</TransportMode>
                    </Line>
                </PublicationDelivery>
            """.trimIndent()
            )
        )

        val blob1: Blob = mock()
        whenever(blob1.getContent()).thenReturn(provider1Zip)
        whenever(storage.get(eq(inputBucket), eq("$datePath/timetable/provider1.zip"))).thenReturn(blob1)

        val blob2: Blob = mock()
        whenever(blob2.getContent()).thenReturn(provider2Zip)
        whenever(storage.get(eq(inputBucket), eq("$datePath/timetable/provider2.zip"))).thenReturn(blob2)

        val fetcher = GcsTimetableFetcher(config, storage, inputBucket)

        val result = fetcher.fetch(listOf("provider1", "provider2"))

        val provider1Data = result["provider1"]!!
        assertThat(provider1Data.allFiles).hasSize(2)
        assertThat(provider1Data.modeHelperFiles).hasSize(1)
        assertThat(provider1Data.modeHelperFiles.first().fileName.toString())
            .isEqualTo("line_p1_001.xml")

        val provider2Data = result["provider2"]!!
        assertThat(provider2Data.allFiles).hasSize(2)
        assertThat(provider2Data.modeHelperFiles).hasSize(2)
        assertThat(provider2Data.modeHelperFiles)
            .extracting { it.fileName.toString() }
            .containsExactlyInAnyOrder("line_p2_001.xml", "line_p2_002.xml")
    }

    @Test
    fun shouldHandleBlacklistedFiles() {
        val helperDir = tempDir.resolve("helper")
        val inputBucket = "test-input-bucket"

        val config = TimetableConfig(
            providers = listOf("provider1"),
            modeFilter = setOf(TransportMode.TRAM),
            blacklist = mapOf(
                "provider1" to listOf("blacklisted_*.xml")
            ),
            helperDir = helperDir
        )

        val today = LocalDate.now()
        val datePath = "${today.year}/${"%02d".format(today.monthValue)}/${"%02d".format(today.dayOfMonth)}"

        val zipBytes = createZipBytes(
            mapOf(
                "line_ok.xml" to """
                <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                    <Line>
                        <TransportMode>tram</TransportMode>
                    </Line>
                </PublicationDelivery>
            """.trimIndent(),
                "blacklisted_001.xml" to """
                <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                    <Line>
                        <TransportMode>tram</TransportMode>
                    </Line>
                </PublicationDelivery>
            """.trimIndent()
            )
        )

        val blob: Blob = mock()
        whenever(blob.getContent()).thenReturn(zipBytes)
        whenever(storage.get(eq(inputBucket), eq("$datePath/timetable/provider1.zip"))).thenReturn(blob)

        val fetcher = GcsTimetableFetcher(config, storage, inputBucket)

        val result = fetcher.fetch(listOf("provider1"))
        val data = result["provider1"]!!

        assertThat(data.allFiles)
            .extracting { it.fileName.toString() }
            .containsExactly("line_ok.xml")

        assertThat(data.modeHelperFiles)
            .extracting { it.fileName.toString() }
            .containsExactly("line_ok.xml")
    }

    @Test
    fun shouldHandleNoLines() {
        val helperDir = tempDir.resolve("helper")
        val inputBucket = "test-input-bucket"

        val config = TimetableConfig(
            providers = listOf("provider1"),
            modeFilter = setOf(TransportMode.TRAM),
            blacklist = emptyMap(),
            helperDir = helperDir
        )

        val today = LocalDate.now()
        val datePath = "${today.year}/${"%02d".format(today.monthValue)}/${"%02d".format(today.dayOfMonth)}"

        val zipBytes = createZipBytes(
            mapOf(
                "no_lines.xml" to """
                <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                    <JourneyPattern>
                        <SomethingElse/>
                    </JourneyPattern>
                </PublicationDelivery>
            """.trimIndent()
            )
        )

        val blob: Blob = mock()
        whenever(blob.getContent()).thenReturn(zipBytes)
        whenever(storage.get(eq(inputBucket), eq("$datePath/timetable/provider1.zip"))).thenReturn(blob)

        val fetcher = GcsTimetableFetcher(config, storage, inputBucket)

        val result = fetcher.fetch(listOf("provider1"))
        val data = result["provider1"]!!

        assertThat(data.allFiles)
            .extracting { it.fileName.toString() }
            .containsExactly("no_lines.xml")

        assertThat(data.modeHelperFiles).isEmpty()
    }

    @Test
    fun shouldHandleInvalidXml() {
        val helperDir = tempDir.resolve("helper")
        val inputBucket = "test-input-bucket"

        val config = TimetableConfig(
            providers = listOf("provider1"),
            modeFilter = setOf(TransportMode.TRAM),
            blacklist = emptyMap(),
            helperDir = helperDir
        )

        val today = LocalDate.now()
        val datePath = "${today.year}/${"%02d".format(today.monthValue)}/${"%02d".format(today.dayOfMonth)}"

        val zipBytes = createZipBytes(
            mapOf(
                "invalid.xml" to """
                <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                    <Line>
                        <TransportMode>tram</TransportMode>
                    <!-- INVALID tags -->
            """.trimIndent()
            )
        )

        val blob: Blob = mock()
        whenever(blob.getContent()).thenReturn(zipBytes)
        whenever(storage.get(eq(inputBucket), eq("$datePath/timetable/provider1.zip"))).thenReturn(blob)

        val fetcher = GcsTimetableFetcher(config, storage, inputBucket)

        val result = fetcher.fetch(listOf("provider1"))
        val data = result["provider1"]!!

        assertThat(data.allFiles)
            .extracting { it.fileName.toString() }
            .containsExactly("invalid.xml")

        assertThat(data.modeHelperFiles).isEmpty()
    }

    private fun createZipBytes(files: Map<String, String>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            files.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return baos.toByteArray()
    }
}
