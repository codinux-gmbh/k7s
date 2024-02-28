package net.dankito.k8s.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class KubernetesResource(
    val group: String?,
    val version: String,
    val name: String,
    val kind: String,
    val isNamespaced: Boolean,
    val isCustomResourceDefinition: Boolean,
    val singularName: String? = null,
    val shortNames: List<String>? = null,
    val verbs: List<Verb> = emptyList()
) {

    companion object {

        fun createIdentifier(resource: KubernetesResource) =
            createIdentifier(resource.group, resource.name, resource.version)

        fun createIdentifier(group: String?, name: String, version: String): String =
            "${group.takeUnless { it.isNullOrBlank() }?.let { "${it}." } ?: ""}${name}.$version"

    }

    @get:JsonIgnore
    val identifier by lazy { createIdentifier(this) }

    override fun toString() = "$kind (${group?.let { "${it}." } ?: ""}${name} $version)"

}