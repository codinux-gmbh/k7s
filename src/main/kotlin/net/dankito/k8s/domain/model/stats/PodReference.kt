package net.dankito.k8s.domain.model.stats

data class PodReference(
    val name: String,
    val namespace: String,
    val uid: String
)