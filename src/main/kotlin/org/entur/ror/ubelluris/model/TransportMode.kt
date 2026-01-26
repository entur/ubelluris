package org.entur.ror.ubelluris.model

enum class TransportMode(val netexValue: String) {
    BUS("bus"),
    TRAM("tram"),
    WATER("water");

    companion object {
        fun fromNetexValue(value: String): TransportMode? {
            return entries.find { it.netexValue.equals(value, ignoreCase = true) }
        }
    }
}