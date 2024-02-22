package net.dankito.k8s.api.dto

import net.dankito.k8s.domain.model.ResourceItem

open class ResourceItemsViewData(
    resourceItems: List<ResourceItem> = emptyList()
) {

    companion object {
        fun sort(items: List<ResourceItem>): List<ResourceItem> =
            items.sortedWith(compareBy( { it.namespace }, { it.name } ))

        fun isNamespacedResource(items: List<ResourceItem>) =
            items.any { it.namespace != null }
    }

    val resourceItems: List<ResourceItem> = sort(resourceItems)

    val isNamespacedResource: Boolean = isNamespacedResource(resourceItems)

}