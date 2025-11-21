package org.entur.ror.ubelluris.filter

import org.entur.netex.tools.lib.config.FilterConfig

interface FilterProfileConfiguration {
    fun build(): FilterConfig
}