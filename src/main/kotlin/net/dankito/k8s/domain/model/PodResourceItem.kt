package net.dankito.k8s.domain.model

import java.time.Instant

class PodResourceItem(
    name: String,
    namespace: String?,
    creationTimestamp: Instant,
    val status: String,
    val podIP: String?,
    val container: List<ContainerStatus>,
    highlightedItemSpecificValues: List<ItemValue> = emptyList(),
    secondaryItemSpecificValues: List<ItemValue> = emptyList()
) : ResourceItem(name, namespace, creationTimestamp, highlightedItemSpecificValues, secondaryItemSpecificValues) {

    val countReadyContainers = container.count { it.ready }

}