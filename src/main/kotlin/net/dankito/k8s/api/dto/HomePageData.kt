package net.dankito.k8s.api.dto

import net.dankito.k8s.domain.model.KubernetesResource
import net.dankito.k8s.domain.model.ResourceItem
import net.dankito.k8s.domain.model.Verb

@Suppress("MemberVisibilityCanBePrivate")
class HomePageData(
    val allResources: List<KubernetesResource>,
    resourceItems: List<ResourceItem> = emptyList()
) : ResourceItemsViewData(resourceItems) {

    companion object {
        private val highlightedResourcesNames = hashSetOf("pods", "services", "ingresses")
    }


    private val listableResources = allResources
        .filter { it.verbs.contains(Verb.list) }

    val highlightedResources = listableResources
        .filter { highlightedResourcesNames.contains(it.name) && it.kind != "PodMetrics" }

    val standardResources = listableResources.filter { it.isCustomResourceDefinition == false }
        .filter { highlightedResources.contains(it) == false }
        .sortedBy { it.name }

    val customResourceDefinitions = listableResources.filter { it.isCustomResourceDefinition }
        .filter { highlightedResources.contains(it) == false }
        .sortedBy { it.identifier }

}