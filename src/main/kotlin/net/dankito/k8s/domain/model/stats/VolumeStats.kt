package net.dankito.k8s.domain.model.stats

import java.time.Instant

class VolumeStats(
    val name: String,
    time: Instant,
    availableBytes: ULong? = null,
    capacityBytes: ULong? = null,
    inodes: ULong? = null,
    inodesFree: ULong? = null,
    inodesUsed: ULong? = null,
    usedBytes: ULong? = null,
    val pvcRef: PvcReference? = null,
    val volumeHealthStats: VolumeHealthStats? = null
) : FsStats(time, availableBytes, capacityBytes, inodes, inodesFree, inodesUsed, usedBytes)