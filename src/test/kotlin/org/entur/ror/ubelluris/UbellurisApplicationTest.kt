package org.entur.ror.ubelluris

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path

class UbellurisApplicationTest {
    @Test
    fun `timetableBlobPaths keeps an absolute input path absolute`() {
        val paths = timetableBlobPaths(listOf("sl", "vt"), Path.of("/artifacts/input/timetable"))

        assertThat(paths).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "sl" to "/artifacts/input/timetable/sl.zip",
                "vt" to "/artifacts/input/timetable/vt.zip",
            ),
        )
    }

    @Test
    fun `timetableBlobPaths keeps a relative input path relative`() {
        val paths = timetableBlobPaths(listOf("sl"), Path.of("2026/01/01/timetable"))

        assertThat(paths).containsExactlyEntriesOf(mapOf("sl" to "2026/01/01/timetable/sl.zip"))
    }
}
