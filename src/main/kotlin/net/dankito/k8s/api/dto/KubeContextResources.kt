package net.dankito.k8s.api.dto

import net.dankito.k8s.domain.model.KubernetesResource

data class KubeContextResources(
    val resources: List<KubernetesResource>,
    val namespaces: List<String>,
    val contexts: List<String>,
    val defaultContext: String?,
)