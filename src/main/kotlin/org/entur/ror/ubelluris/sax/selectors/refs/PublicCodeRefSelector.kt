package org.entur.ror.ubelluris.sax.selectors.refs

import org.entur.netex.tools.lib.model.EntityModel
import org.entur.netex.tools.lib.selections.RefSelection
import org.entur.netex.tools.lib.selectors.refs.RefSelector
import org.entur.ror.ubelluris.sax.plugins.PublicCodeRepository

class PublicCodeRefSelector(val publicCodeRepository: PublicCodeRepository) : RefSelector {

    override fun selectRefs(model: EntityModel): RefSelection {
        val selectedRefs = model.listAllRefs().toMutableSet()
        val repository = publicCodeRepository
        val test = repository.types

        return RefSelection(selectedRefs)
    }
}