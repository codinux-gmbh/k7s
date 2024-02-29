package net.dankito.k8s.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import net.dankito.k8s.domain.service.KubernetesService

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class KubernetesResource(
    val group: String?,
    /**
     * A resource might have multiple versions, e.g. v1beta1, v1beta2 and v1, but each resource only has one current
     * storage version, that is the version in which new and updated resource items are stored.
     *
     * You can request resource items in any served version and the items are returned in that specific version no matter
     * in which version they are actually stored. So there's no reason to specify a resource's version on our API, we
     * simply take the storage version to request the resource items at the Kubernetes API, which should be fine for
     * almost all cases
     */
    val storageVersion: String,
    val name: String,
    val kind: String,
    val isNamespaced: Boolean,
    val isCustomResourceDefinition: Boolean,
    val singularName: String? = null,
    val shortNames: List<String>? = null,
    val verbs: List<Verb> = emptyList(),
    val servedVersions: List<String>
) {

    companion object {

        fun createIdentifier(resource: KubernetesResource) =
            createIdentifier(resource.group, resource.name)

        fun createIdentifier(group: String?, name: String): String =
            "${group.takeUnless { it.isNullOrBlank() } ?: ""}.$name"

    }

    @get:JsonIgnore
    val identifier by lazy { createIdentifier(this) }

    @get:JsonIgnore
    val version: String = storageVersion // to be better readable

    val isLoggable by lazy { KubernetesService.LoggableResourceNames.contains(name) }

    fun containsVerb(verb: Verb) = verbs.contains(verb)

    override fun toString() = "$kind (${group?.let { "${it}." } ?: ""}${name} $version)"

}