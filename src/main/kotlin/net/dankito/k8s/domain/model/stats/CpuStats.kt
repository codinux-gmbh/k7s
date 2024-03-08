package net.dankito.k8s.domain.model.stats

import java.time.Instant

data class CpuStats(
    val time: Instant,
    val usageCoreNanoSeconds: ULong? = null,
    val usageNanoCores: ULong? = null
)