package org.entur.ror.ubelluris.filter

import org.entur.netex.tools.lib.config.FilterConfig
import org.entur.netex.tools.lib.config.FilterConfigBuilder
import org.entur.ror.ubelluris.config.CliConfig
import org.entur.ror.ubelluris.sax.handlers.PublicationTimestampHandler
import org.entur.ror.ubelluris.sax.plugins.timetable.TransportModeToLocalScheduledStopPointMapper

class TimetableFilterConfig(
    private val cliConfig: CliConfig,
) : FilterProfileConfiguration {

    val plugin = TransportModeToLocalScheduledStopPointMapper(cliConfig.transportModes)

    override fun build(): FilterConfig {
        return FilterConfigBuilder()
            .withSkipElements(listOf())
            .withCustomElementHandlers(
                mapOf(
                    "/PublicationDelivery/PublicationTimestamp" to PublicationTimestampHandler()
                )
            )
            .withPlugins(listOf(plugin))
            .withRemovePrivateData(false)
            .withPreserveComments(false)
            .withUseSelfClosingTagsWhereApplicable(true)
            .withPruneReferences(false)
            .build()
    }
}
