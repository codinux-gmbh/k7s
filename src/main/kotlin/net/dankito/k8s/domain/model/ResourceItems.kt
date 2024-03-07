package net.dankito.k8s.domain.model

data class ResourceItems(
    val resourceVersion: String?,
    val items: List<ResourceItem>
)