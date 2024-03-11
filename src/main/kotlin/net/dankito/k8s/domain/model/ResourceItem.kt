package net.dankito.k8s.domain.model

import java.time.Instant

open class ResourceItem(
    val name: String,
    val namespace: String? = null,
    val creationTimestamp: Instant,
    val highlightedItemSpecificValues: Map<String, String?> = emptyMap(),
    val secondaryItemSpecificValues: Map<String, String?> = emptyMap()
) {

    /**
     * The id attribute in HTML may only contain letters, digits, '_' and '-'
     */
    val htmlSafeId by lazy { "${namespace}__$name" }

    override fun toString() = "${namespace?.let { "$it " }}$name"

}