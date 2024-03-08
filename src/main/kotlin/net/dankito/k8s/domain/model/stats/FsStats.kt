package net.dankito.k8s.domain.model.stats

import java.time.Instant

open class FsStats(
    val time: Instant,
    val availableBytes: ULong? = null,
    val capacityBytes: ULong? = null,
    val inodes: ULong? = null,
    val inodesFree: ULong? = null,
    val inodesUsed: ULong? = null,
    val usedBytes: ULong? = null
)