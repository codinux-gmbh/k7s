package net.dankito.k8s.domain.model.stats

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class PodStats(
    val podRef: PodReference,
    val startTime: Instant,
    val containers: List<ContainerStats> = emptyList(),
    val cpu: CpuStats? = null,
    val memory: MemoryStats? = null,
    val network: NetworkStats? = null,
    val volume: List<VolumeStats> = emptyList(),
    @get:JsonProperty("ephemeral-storage")
    val ephemeralStorage: FsStats? = null,
    @get:JsonProperty("process_stats")
    val processStats: ProcessStats? = null,
    val swap: SwapStats? = null
)