package net.dankito.k8s.domain.model

import java.time.Instant

class PodResourceItem(
    name: String,
    namespace: String?,
    creationTimestamp: Instant,
    val status: String,
    val podIP: String?,
    val container: List<ContainerStatus>,
    highlightedItemSpecificValues: Map<String, String?> = emptyMap(),
    secondaryItemSpecificValues: Map<String, String?> = emptyMap()
) : ResourceItem(name, namespace, creationTimestamp, highlightedItemSpecificValues, secondaryItemSpecificValues) {

    val countReadyContainers = container.count { it.ready }

}