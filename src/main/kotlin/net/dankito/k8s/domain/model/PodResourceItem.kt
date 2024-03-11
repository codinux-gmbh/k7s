package net.dankito.k8s.domain.model

import java.time.Instant

class PodResourceItem(
    name: String,
    namespace: String?,
    creationTimestamp: Instant,
    val status: String,
    val podIP: String?,
    itemSpecificValues: Map<String, String?> = emptyMap(),
    val container: List<ContainerStatus>
) : ResourceItem(name, namespace, creationTimestamp, itemSpecificValues) {

    val countReadyContainers = container.count { it.ready }

}