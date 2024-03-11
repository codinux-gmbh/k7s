package net.dankito.k8s.domain.model

open class ResourceItem(
    val name: String,
    val namespace: String? = null,
    val itemSpecificValues: Map<String, String?> = emptyMap()
) {

    /**
     * The id attribute in HTML may only contain letters, digits, '_' and '-'
     */
    val htmlSafeId by lazy { "${namespace}__$name" }

    override fun toString() = "${namespace?.let { "$it " }}$name"

}