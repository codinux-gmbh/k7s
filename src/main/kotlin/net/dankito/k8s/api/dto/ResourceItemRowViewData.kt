package net.dankito.k8s.api.dto

import net.dankito.k8s.domain.model.KubernetesResource
import net.dankito.k8s.domain.model.ResourceItem

data class ResourceItemRowViewData(
    val item: ResourceItem,
    val resource: KubernetesResource,
    val selectedNamespace: String?,
)