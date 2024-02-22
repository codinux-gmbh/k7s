package net.dankito.k8s.domain.model

open class ResourceItem(
    val name: String,
    val namespace: String? = null
) {
    override fun toString() = "${namespace?.let { "$it " }}$name"
}