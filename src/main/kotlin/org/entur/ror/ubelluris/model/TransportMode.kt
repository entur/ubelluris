package org.entur.ror.ubelluris.model

/**
 * NeTEx TransportMode values
 */
enum class TransportMode(val netexValue: String) {
    AIR("air"),
    BUS("bus"),
    TRAM("tram"),
    COACH("coach"),
    RAIL("rail"),
    WATER("water"),
    METRO("metro"),
    TAXI("taxi"),
    OTHER("other");

    companion object {
        fun fromNetexValue(value: String): TransportMode? {
            return entries.find { it.netexValue.equals(value, ignoreCase = true) }
        }
    }
}