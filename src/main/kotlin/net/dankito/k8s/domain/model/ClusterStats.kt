package net.dankito.k8s.domain.model

data class ClusterStats(
    val cpuPercentage: Int? = null,
    val memoryPercentage: Int? = null
)