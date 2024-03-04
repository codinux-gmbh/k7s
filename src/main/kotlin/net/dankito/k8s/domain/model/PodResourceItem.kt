package net.dankito.k8s.domain.model

class PodResourceItem(
    name: String,
    namespace: String?,
    val phase: String,
    val podIP: String?,
    val container: List<ContainerStatus>
) : ResourceItem(name, namespace) {

    val countReadyContainers = container.count { it.ready }

}