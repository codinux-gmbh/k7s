package net.dankito.k8s.domain.model

open class ResourceItem(
    val name: String,
    val namespace: String? = null,
    val itemSpecificValues: Map<String, String?> = emptyMap()
) {

    override fun toString() = "${namespace?.let { "$it " }}$name"

}