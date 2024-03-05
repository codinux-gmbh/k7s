package net.dankito.k8s.domain.model

class PodResourceItem(
    name: String,
    namespace: String?,
    val phase: String,
    val podIP: String?,
    val hostIP: String?,
    val container: List<ContainerStatus>
) : ResourceItem(name, namespace, mapOf("IP" to podIP, "HostIP" to hostIP)) {

    val countReadyContainers = container.count { it.ready }

}