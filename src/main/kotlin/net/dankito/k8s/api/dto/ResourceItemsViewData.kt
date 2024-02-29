package net.dankito.k8s.api.dto

import net.dankito.k8s.domain.model.KubernetesResource
import net.dankito.k8s.domain.model.ResourceItem

open class ResourceItemsViewData(
    val resource: KubernetesResource,
    resourceItems: List<ResourceItem> = emptyList(),
    val selectedNamespace: String?
) {

    companion object {
        fun sort(items: List<ResourceItem>): List<ResourceItem> =
            items.sortedWith(compareBy( { it.namespace }, { it.name } ))
    }

    val resourceItems: List<ResourceItem> = sort(resourceItems)

}