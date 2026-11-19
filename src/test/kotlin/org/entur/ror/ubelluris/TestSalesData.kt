package org.entur.ror.ubelluris

import org.entur.ror.ubelluris.model.SalesData

/** Sales data without ServiceJourneys or calendar, for tests that do not use the sales matching. */
fun emptySalesData() = SalesData.EMPTY

/** A provider that is not in the default salesPatches, so the timetable filter does not run the sales steps for it. */
const val PROVIDER_WITHOUT_SALES_PATCH = "vt"

/** The provider of the default salesPatches, so the timetable filter runs the sales steps for it. */
const val PROVIDER_WITH_SALES_PATCH = "sj"
