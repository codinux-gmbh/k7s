package net.dankito.k8s.api.dto

import io.fabric8.kubernetes.api.model.HasMetadata
import net.dankito.k8s.domain.model.KubernetesResource
import net.dankito.k8s.domain.model.Verb

@Suppress("MemberVisibilityCanBePrivate")
class HomePageData(
    val allResources: List<KubernetesResource>,
    resourceItems: List<HasMetadata> = emptyList()
) {

    companion object {
        private val highlightedResourcesNames = hashSetOf("pods", "services", "ingresses")

        fun sort(items: List<HasMetadata>): List<HasMetadata> =
            items.sortedWith(compareBy( { it.metadata.namespace }, { it.metadata.name } ))
    }

    val resourceItems: List<HasMetadata> = sort(resourceItems)

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