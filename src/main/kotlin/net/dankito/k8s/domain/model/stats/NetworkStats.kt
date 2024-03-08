package net.dankito.k8s.domain.model.stats

import java.time.Instant

class NetworkStats(
    val time: Instant,
    name: String,
    rxBytes: ULong? = null,
    rxErrors: ULong? = null,
    txBytes: ULong? = null,
    txErrors: ULong? = null,
    val interfaces: List<InterfaceStats> = emptyList()
): InterfaceStats(name, rxBytes, rxErrors, txBytes, txErrors)