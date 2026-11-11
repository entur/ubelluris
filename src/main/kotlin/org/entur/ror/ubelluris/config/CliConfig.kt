package org.entur.ror.ubelluris.config

import kotlinx.serialization.Serializable
import org.entur.ror.ubelluris.model.TransportMode

@Serializable
data class CliConfig(
    var sourceCodespace: String,
    var targetCodespace: String,
    var timetableProviders: List<String> = listOf("vt", "halland", "skane"),
    var transportModes: List<TransportMode> = listOf(TransportMode.TRAM, TransportMode.WATER),
    val illegalPublicCodes: List<String> = listOf("-"),
    val filterPublicCodes: List<String> = listOf("*"),
    val lineTransportModesToRemove: List<TransportMode> = listOf(TransportMode.TAXI),
    val resultsDir: String = "results",
    val dropParentStops: Boolean = false,
    val salesAuthorityNumberPriority: List<String> = emptyList(),
    val salesPatches: List<SalesPatch> = emptyList(),
    val timetableFilterFeatures: TimetableFilterFeatures = TimetableFilterFeatures(),
) {
    fun salesPatchCarriers(provider: String): Set<String> = salesPatches.filter { it.provider == provider }.map { it.carrier }.toSet()

    /** Fails when the configuration is not consistent. Provider names are case-sensitive, because they are used in file paths. */
    fun validate() {
        val unknownProviders = salesPatches.map { it.provider }.filterNot { it in timetableProviders }.distinct()
        require(unknownProviders.isEmpty()) {
            "salesPatches has providers $unknownProviders that are not in timetableProviders $timetableProviders"
        }
    }
}

/** Patch the timetable data of [provider] with the sales data of [carrier], i.e. the sales authority number. */
@Serializable
data class SalesPatch(
    val provider: String,
    val carrier: String,
)

@Serializable
data class TimetableFilterFeatures(
    val versionRefNormalizer: Boolean = true,
    val transportModeToLocalScheduledStopPoint: Boolean = true,
    val lineRemovalBasedOnPublicCodes: Boolean = true,
    val lineRemovalBasedOnTransportMode: Boolean = true,
    val skipTopographicPlaceCountryRef: Boolean = true,
    val skipRequestStop: Boolean = true,
    val publicationTimestamp: Boolean = true,
    val bookWhenFilter: Boolean = true,
    val timetabledPassingTimeId: Boolean = true,
    val interchangeDeduplication: Boolean = true,
    val firstAndLastStopBoardingAlighting: Boolean = true,
    val emptyGroupOfStopPlacesMembersFilter: Boolean = true,
    val lineOperatorEnricher: Boolean = true,
    val pruneUnreferencedEntities: Boolean = true,
    val useSalesData: Boolean = true,
)
