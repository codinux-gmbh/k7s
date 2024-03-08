package net.dankito.k8s.domain.model.stats

import java.time.Instant

data class ContainerStats(
    val name: String,
    val startTime: Instant,
    val cpu: CpuStats? = null,
    val memory: MemoryStats? = null,
    val rootfs: FsStats? = null,
    val logs: FsStats? = null,
    val swap: SwapStats? = null,
    // TODO:
//    val accelerators: List<AcceleratorStats> = emptyList(),
//    val userDefinedMetrics: List<UserDefinedMetrics> = emptyList()
)