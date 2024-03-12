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
    val displayName: String by lazy {
        // name is the plural but it is lowercase whilst in kind all words start with an uppercase letter -> try to merge both
        if (name.startsWith(kind.substring(0, kind.length - 1).lowercase())) {
            "${kind.substring(0, kind.length - 1)}${name.substring(kind.length - 1)}"
        } else {
            kind
        }
    }

    @get:JsonIgnore
    val isPod: Boolean by lazy { group == null && kind == "Pod" }

    @get:JsonIgnore
    val identifier by lazy { createIdentifier(this) }

    @get:JsonIgnore
    val version: String = storageVersion // to be better readable

    val isLoggable by lazy { KubernetesService.LoggableResourceKinds.contains(kind) }

    @get:JsonIgnore
    val isScalable by lazy { kind == "Deployment" || kind == "StatefulSet" }

    @get:JsonIgnore
    val isDeletable by lazy { containsVerb(Verb.delete) }

    @get:JsonIgnore
    val allowDeletingWithoutConfirmation by lazy { isPod }

    @get:JsonIgnore
    val isWatchable by lazy { containsVerb(Verb.watch) }

    fun containsVerb(verb: Verb) = verbs.contains(verb)


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KubernetesResource) return false

        if (group != other.group) return false
        if (storageVersion != other.storageVersion) return false
        if (name != other.name) return false
        if (kind != other.kind) return false

        return true
    }

    override fun hashCode(): Int {
        var result = group?.hashCode() ?: 0
        result = 31 * result + storageVersion.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + kind.hashCode()
        return result
    }

    override fun toString() = "$kind (${group?.let { "${it}." } ?: ""}${name} $version)"

}