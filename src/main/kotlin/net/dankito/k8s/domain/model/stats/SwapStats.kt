package net.dankito.k8s.domain.model.stats

import java.time.Instant

data class SwapStats(
    val time: Instant,
    val swapAvailableBytes: ULong? = null,
    val swapUsageBytes: ULong? = null
)