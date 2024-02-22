package net.dankito.k8s.api.dto

import net.dankito.k8s.domain.model.KubernetesResource
import net.dankito.k8s.domain.model.ResourceItem

open class ResourceItemsViewData(
    resource: KubernetesResource,
    resourceItems: List<ResourceItem> = emptyList()
) {

    companion object {
        fun sort(items: List<ResourceItem>): List<ResourceItem> =
            items.sortedWith(compareBy( { it.namespace }, { it.name } ))

        fun isNamespacedResource(items: List<ResourceItem>) =
            items.any { it.namespace != null }
    }

    val resourceItems: List<ResourceItem> = sort(resourceItems)

    val isNamespacedResource: Boolean = resource.isNamespaced

}