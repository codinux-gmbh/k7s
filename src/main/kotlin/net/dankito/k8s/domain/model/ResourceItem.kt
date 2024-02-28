package net.dankito.k8s.domain.model

open class ResourceItem(
    val resource: KubernetesResource,
    val name: String,
    val namespace: String? = null
) {

    override fun toString() = "${resource.kind} ${namespace?.let { "$it " }}$name"

}