package org.entur.ror.ubelluris.sax.selectors.refs

import org.entur.netex.tools.lib.model.EntityModel
import org.entur.netex.tools.lib.selections.RefSelection
import org.entur.netex.tools.lib.selectors.refs.RefSelector

class StopPlacePurgingRefSelector() : RefSelector {

    override fun selectRefs(model: EntityModel): RefSelection {
        val selectedRefs = model.listAllRefs().toMutableSet()
        return RefSelection(selectedRefs)
    }
}