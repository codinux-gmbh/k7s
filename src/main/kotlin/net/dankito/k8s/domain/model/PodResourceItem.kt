package net.dankito.k8s.domain.model

class PodResourceItem(
    name: String,
    namespace: String?,
    val status: String,
    val podIP: String?,
    itemSpecificValues: Map<String, String?> = emptyMap(),
    val container: List<ContainerStatus>
) : ResourceItem(name, namespace, itemSpecificValues) {

    val countReadyContainers = container.count { it.ready }

}