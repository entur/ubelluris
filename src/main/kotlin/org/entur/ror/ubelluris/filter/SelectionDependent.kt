package org.entur.ror.ubelluris.filter

import org.entur.netex.tools.lib.selections.EntitySelection
import org.entur.netex.tools.lib.selections.RefSelection

/** Data that must be reduced to the final entity selection before the export writes it. */
interface SelectionDependent {
    fun updateBasedOnSelection(
        entitySelection: EntitySelection,
        refSelection: RefSelection,
    )
}
