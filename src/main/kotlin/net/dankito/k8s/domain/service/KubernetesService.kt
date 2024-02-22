package net.dankito.k8s.domain.service

import io.fabric8.kubernetes.api.model.APIResource
import io.fabric8.kubernetes.api.model.GenericKubernetesResource
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.KubernetesResourceList
import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress
import io.fabric8.kubernetes.client.ApiVisitor
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.dsl.AnyNamespaceOperation
import io.fabric8.kubernetes.client.dsl.MixedOperation
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext
import jakarta.inject.Singleton
import net.codinux.log.logger
import net.dankito.k8s.domain.model.KubernetesResource
import net.dankito.k8s.domain.model.Verb

@Singleton
class KubernetesService(
    private val client: KubernetesClient
) {

    private val log by logger()


    fun getNodes() = listItems(client.nodes())

    fun getNamespaces() = listItems(client.namespaces())

    fun getPods(namespace: String? = null) = listItems(client.pods(), namespace)

    fun getDeployments(namespace: String? = null) = listItems(client.apps().deployments(), namespace)

    fun getServices(namespace: String? = null) = listItems(client.services(), namespace)

    fun getIngresses(namespace: String? = null): List<Ingress> = listItems(client.network().ingresses(), namespace)

    private fun <T : HasMetadata, L : KubernetesResourceList<T>, R> listItems(operation: AnyNamespaceOperation<T, L, R>, namespace: String? = null): List<T> =
        operation.let {
            if (namespace != null && operation is MixedOperation<T, L, *>) {
                operation.inNamespace(namespace)
            } else {
                operation
            }
        }
            .list().items as List<T>


    fun getCustomResourceDefinitions(): List<KubernetesResource> {
        val crds = client.apiextensions().v1().customResourceDefinitions().list().items

        return crds.flatMap { crd ->
            crd.spec.versions.map { version ->
                // TODO: here verbs are always null / not set and group and singular are always set
                KubernetesResource(
                    group = crd.spec.group,
                    version = version.name,
                    name = crd.spec.names.plural,
                    kind = crd.spec.names.kind,
                    isNamespaced = crd.spec.scope == "Namespaced",
                    isCustomResourceDefinition = true,
                    singularName = crd.spec.names.singular,
                    shortNames = crd.spec.names.shortNames.takeIf { it.isNotEmpty() }
                )
            }
        }
    }

    fun getAllAvailableResourceTypes(): List<KubernetesResource> {
        val resources = mutableListOf<KubernetesResource>()
        val crds = getCustomResourceDefinitions().associateBy { it.identifier }

        client.visitResources { group, version, apiResource, _ ->
            // here the group is null for a lot of resources (Kubernetes standard resources), for two singular is null and verbs are always set
            resources.add(map(group, version, apiResource, crds))

            ApiVisitor.ApiVisitResult.CONTINUE
        }

        return resources
    }

    fun map(group: String?, version: String, apiResource: APIResource, crds: Map<String, KubernetesResource>): KubernetesResource {
        val name = apiResource.name
        val identifier = KubernetesResource.createIdentifier(group, name, version)

        // here the group is null for a lot of resources (Kubernetes standard resources), for two singular is null and verbs are always set
        return KubernetesResource(
            group = group,
            version = version,
            name = name,
            kind = apiResource.kind,
            isNamespaced = apiResource.namespaced,
            isCustomResourceDefinition = crds[identifier] != null,
            singularName = apiResource.singularName.takeUnless { it.isNullOrBlank() },
            shortNames = apiResource.shortNames.takeUnless { it.isEmpty() },
            verbs = apiResource.verbs.orEmpty().mapNotNull { Verb.getByName(it) }
                .sorted()
        )
    }

    fun getResourceItems(group: String?, name: String, version: String): List<GenericKubernetesResource> {
        val context = ResourceDefinitionContext.Builder()
            .withGroup(group)
            .withPlural(name)
            .withVersion(version)
            .build()

        return listItems(client.genericKubernetesResources(context))
    }

    fun getResourceItems(resource: KubernetesResource): List<GenericKubernetesResource> {
        val context = ResourceDefinitionContext.Builder()
            .withGroup(resource.group)
            .withVersion(resource.version)
            .withPlural(resource.name)
            .withKind(resource.kind)
            .withNamespaced(resource.isNamespaced)
            .build()

        return listItems(client.genericKubernetesResources(context))
    }

    fun getResourceItemsResponse(resource: KubernetesResource): String? {
        return if (resource.verbs.contains(Verb.list) == false) {
            null
        } else {
            try {
                // the logic for the URL is:
                // - no group -> /api/<version>/<pluralName>
                // - has group -> /apis/<group>/<version>/<pluralName>

                // namespaced:
                // - without group: /api/v1/namespaces/collab/pods
                // - with group: /apis/apps/v1/namespaces/collab/deployments
                if (resource.group.isNullOrBlank()) {
                    client.raw("/api/${resource.version}/${resource.name}")
                } else {
                    client.raw("/apis/${resource.group}/${resource.version}/${resource.name}")
                }
            } catch (e: Exception) {
                log.error(e) { "Could not request all items of resource $resource" }
                null
            }
        }
    }

}