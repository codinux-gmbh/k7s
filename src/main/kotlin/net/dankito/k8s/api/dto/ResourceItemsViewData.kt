package net.dankito.k8s.api.dto

import net.dankito.k8s.domain.model.ClusterStats
import net.dankito.k8s.domain.model.KubernetesResource
import net.dankito.k8s.domain.model.ResourceItem

open class ResourceItemsViewData(
    val resource: KubernetesResource,
    resourceItems: List<ResourceItem> = emptyList(),
    val stats: ClusterStats,
    val contexts: List<String>,
    val defaultContext: String?,
    val selectedContext: String?,
    val selectedNamespace: String?,
    val resourceVersion: String? = null
) {

    companion object {
        fun sort(resource: KubernetesResource, items: List<ResourceItem>): List<ResourceItem> =
            if (resource.kind == "PersistentVolume") {
                items.sortedBy { it.secondaryItemSpecificValues.firstOrNull { it.name == "Claim" }?.value }
            } else {
                items.sortedWith(compareBy({ it.namespace }, { it.name }))
            }
    }

    val resourceItems: List<ResourceItem> = sort(resource, resourceItems)

}