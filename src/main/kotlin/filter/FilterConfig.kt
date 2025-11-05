package filter

import org.entur.netex.tools.lib.config.FilterConfig

interface FilterProfileConfiguration {
    fun build(): FilterConfig
}