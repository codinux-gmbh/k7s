package net.dankito.k8s.api.dto

import net.dankito.k8s.domain.model.KubernetesResource
import net.dankito.k8s.domain.model.ResourceItem
import net.dankito.k8s.domain.model.Verb

@Suppress("MemberVisibilityCanBePrivate")
class HomePageData(
    val allResources: List<KubernetesResource>,
    val allNamespaces: List<ResourceItem>,
    resource: KubernetesResource,
    resourceItems: List<ResourceItem> = emptyList()
) : ResourceItemsViewData(resource, resourceItems, null) {

    companion object {
        private val highlightedResourcesNames = hashSetOf("pods", "services", "ingresses")
    }


    private val listableResources = allResources
        .filter { it.containsVerb(Verb.list) }

    val highlightedResources = listableResources
        .filter { highlightedResourcesNames.contains(it.name) && it.kind != "PodMetrics" }

    val standardResources = listableResources.filter { it.isCustomResourceDefinition == false }
        .sortedBy { it.name }

    val customResourceDefinitions = listableResources.filter { it.isCustomResourceDefinition }
        .sortedBy { it.identifier }

    val commandNamesToUrlPath: Map<String, String> =
        allResources.flatMap { resource ->
            val commandToExecute = """{ "command": "displayResourceItems",${resource.group?.let { """"resourceGroup": "$it", """ } ?: ""} "resourceName": "${resource.name}" }"""
            (listOf(resource.singularName ?: resource.kind) + resource.shortNames.orEmpty()).map {
                it to commandToExecute
            }
        }.toMap() +
                mapOf("ns:all" to """{ "command": "switchToNamespace", "namespace": null }""") +
                allNamespaces.map { "ns:${it.name}" to """{ "command": "switchToNamespace", "namespace": "${it.name}" }""" }

}