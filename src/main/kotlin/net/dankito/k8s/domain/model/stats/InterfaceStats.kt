package net.dankito.k8s.domain.model.stats

open class InterfaceStats(
    val name: String,
    val rxBytes: ULong? = null,
    val rxErrors: ULong? = null,
    val txBytes: ULong? = null,
    val txErrors: ULong? = null
)