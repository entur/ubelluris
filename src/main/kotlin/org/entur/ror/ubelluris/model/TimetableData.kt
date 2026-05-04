package org.entur.ror.ubelluris.model

import java.nio.file.Path

data class TimetableData(
    val provider: String,
    val providerDir: Path = Path.of(""),
    val quayModes: Map<String, Set<TransportMode>> = emptyMap()
)
