package net.dankito.k8s.domain.model

import io.fabric8.kubernetes.client.Config

data class KubeConfigs(
    val defaultContext: String?,
    val contextConfigs: Map<String, Config>
)