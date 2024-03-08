package net.dankito.k8s.domain.model.stats

import java.time.Instant

/**
 * NodeStats holds node-level unprocessed sample stats.
 */
data class NodeStats(
    val nodeName: String,
    val startTime: Instant,
    val systemContainers: List<ContainerStats> = emptyList(),
    val cpu: CpuStats? = null,
    val memory: MemoryStats? = null,
    val network: NetworkStats? = null,
    val fs: FsStats? = null,
    val runtime: RuntimeStats? = null,
    val rlimit: RlimitStats? = null,
    val swap: SwapStats? = null
)