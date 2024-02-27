package net.dankito.k8s.domain.service

import io.fabric8.kubernetes.api.model.APIResource
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.KubernetesResourceList
import io.fabric8.kubernetes.client.ApiVisitor
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.dsl.AnyNamespaceOperation
import io.fabric8.kubernetes.client.dsl.MixedOperation
import io.fabric8.kubernetes.client.dsl.TimeTailPrettyLoggable
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext
import jakarta.inject.Singleton
import net.codinux.log.logger
import net.dankito.k8s.domain.model.KubernetesResource
import net.dankito.k8s.domain.model.ResourceItem
import net.dankito.k8s.domain.model.Verb
import java.io.InputStream
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Singleton
class KubernetesService(
    private val client: KubernetesClient
) {

    private lateinit var customResourceDefinitions: List<KubernetesResource>

    private lateinit var allAvailableResourceTypes: List<KubernetesResource>

    private val log by logger()


    fun getNamespaces() = listItems(namespaceResource, client.namespaces())

    fun getPods(namespace: String? = null) = listItems(podResource, client.pods(), namespace)

    fun getServices(namespace: String? = null) = listItems(serviceResource, client.services(), namespace)

    fun getIngresses(namespace: String? = null) = listItems(ingressResource, client.network().ingresses(), namespace)

    val namespaceResource by lazy { getResource(null, "namespaces", "v1")!! }

    val podResource by lazy { getResource(null, "pods", "v1")!! }

    val serviceResource by lazy { getResource(null, "services", "v1")!! }

    val ingressResource by lazy { getResource("networking.k8s.io", "ingresses", "v1")!! }


    fun getCustomResourceDefinitions(): List<KubernetesResource> {
        if (this::customResourceDefinitions.isInitialized) {
            customResourceDefinitions
        }

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
        }.also {
            this.customResourceDefinitions = it
        }
    }

    fun getAllAvailableResourceTypes(): List<KubernetesResource> {
        if (this::allAvailableResourceTypes.isInitialized) {
            allAvailableResourceTypes
        }

        val resources = mutableListOf<KubernetesResource>()
        val crds = getCustomResourceDefinitions().associateBy { it.identifier }

        client.visitResources { group, version, apiResource, _ ->
            // here the group is null for a lot of resources (Kubernetes standard resources), for two singular is null and verbs are always set
            resources.add(map(group, version, apiResource, crds))

            ApiVisitor.ApiVisitResult.CONTINUE
        }

        this.allAvailableResourceTypes = resources

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


    fun getResource(group: String?, name: String, version: String): KubernetesResource? {
        val resource = getAllAvailableResourceTypes().firstOrNull { it.group == group && it.name == name && it.version == version }

        if (resource == null) {
            log.error { "Could not find resource for group = $group, name = $name and version = $version. Should never happen." }
        }

        return resource
    }

    fun getResourceItems(resource: KubernetesResource): List<ResourceItem> {
        val context = ResourceDefinitionContext.Builder()
            .withGroup(resource.group)
            .withVersion(resource.version)
            .withPlural(resource.name)
            .withKind(resource.kind)
            .withNamespaced(resource.isNamespaced)
            .build()

        return listItems(resource, client.genericKubernetesResources(context))
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

    private fun <T : HasMetadata, L : KubernetesResourceList<T>, R> listItems(
        resource: KubernetesResource,
        operation: AnyNamespaceOperation<T, L, R>,
        namespace: String? = null
    ): List<ResourceItem> =
        operation.let {
            if (namespace != null && operation is MixedOperation<T, L, *>) {
                operation.inNamespace(namespace)
            } else {
                operation
            }
        }
            .list().items.let { it as List<T> }.map { item ->
                ResourceItem(resource, item.metadata.name, item.metadata.namespace?.takeUnless { it.isBlank() })
            }


    fun getLogs(podName: String, podNamespace: String, containerName: String? = null): List<String> =
        getLoggable(podNamespace, podName, containerName)
            .getLog(true)
            .split('\n')
            .let { logs ->
                if (logs.lastOrNull()?.isBlank() == true) { // the last message is in most cases an empty string, remove it
                    logs.dropLast(1)
                } else {
                    logs
                }
            }

    fun watchLogs(podName: String, podNamespace: String, containerName: String? = null, sinceTimeUtc: ZonedDateTime? = null): InputStream =
        getLoggable(podNamespace, podName, containerName).sinceTime((sinceTimeUtc ?: Instant.now().atOffset(ZoneOffset.UTC)).toString()).watchLog().output

    private fun getLoggable(podNamespace: String, podName: String, containerName: String?): TimeTailPrettyLoggable =
        client.pods().inNamespace(podNamespace).withName(podName).let {
            if (containerName != null) {
                it.inContainer(containerName)
            } else {
                it
            }
        }

}