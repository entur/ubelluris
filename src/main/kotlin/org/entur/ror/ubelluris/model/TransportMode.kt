package org.entur.ror.ubelluris.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TransportMode(val netexValue: String) {
    @SerialName("bus")
    BUS("bus"),
    @SerialName("tram")
    TRAM("tram"),
    @SerialName("water")
    WATER("water");

    companion object {
        fun fromNetexValue(value: String): TransportMode? {
            return entries.find { it.netexValue.equals(value, ignoreCase = true) }
        }
    }
}