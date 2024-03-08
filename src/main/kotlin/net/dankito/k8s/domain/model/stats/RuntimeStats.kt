package net.dankito.k8s.domain.model.stats

data class RuntimeStats(
    val imageFs: FsStats? = null,
    val containerFs: FsStats? = null
)