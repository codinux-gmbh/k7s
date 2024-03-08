package net.dankito.k8s.domain.model.stats

import java.time.Instant

data class MemoryStats(
    val time: Instant,
    val availableBytes: ULong? = null,
    val majorPageFaults: ULong? = null,
    val pageFaults: ULong? = null,
    val rssBytes: ULong? = null,
    val usageBytes: ULong? = null,
    val workingSetBytes: ULong? = null
)