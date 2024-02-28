package net.dankito.k8s.domain.service

import io.fabric8.kubernetes.api.model.*
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

    val namespaceResource by lazy { getResource(null, "namespaces")!! }

    val podResource by lazy { getResource(null, "pods")!! }

    val serviceResource by lazy { getResource(null, "services")!! }

    // for deprecated API groups see https://kubernetes.io/docs/reference/using-api/deprecation-guide/
    val ingressResource by lazy { getResource("networking.k8s.io", "ingresses") ?: getResource("extensions", "ingresses")!! }


    fun getCustomResourceDefinitions(): List<KubernetesResource> {
        if (this::customResourceDefinitions.isInitialized) {
            customResourceDefinitions
        }

        val crds = client.apiextensions().v1().customResourceDefinitions().list().items

        return crds.map { crd ->
            val storageVersion = crd.spec.versions.first { it.storage }

            // TODO: here verbs are always null / not set and group and singular are always set
            KubernetesResource(
                group = crd.spec.group,
                storageVersion = storageVersion.name,
                name = crd.spec.names.plural,
                kind = crd.spec.names.kind,
                isNamespaced = crd.spec.scope == "Namespaced",
                isCustomResourceDefinition = true,
                singularName = crd.spec.names.singular,
                shortNames = crd.spec.names.shortNames.takeIf { it.isNotEmpty() },
                servedVersions = crd.spec.versions.map { it.name }
            )
        }.also {
            this.customResourceDefinitions = it
        }
    }

    fun getAllAvailableResourceTypes(): List<KubernetesResource> {
        if (this::allAvailableResourceTypes.isInitialized) {
            allAvailableResourceTypes
        }

        val apiResourceByIdentifier = mutableMapOf<String, MutableList<Triple<String, String, APIResource>>>()
        client.visitResources { group, version, apiResource, _ ->
            apiResourceByIdentifier.getOrPut(KubernetesResource.createIdentifier(group, apiResource.name), { mutableListOf() })
                .add(Triple(group, version, apiResource))

            ApiVisitor.ApiVisitResult.CONTINUE
        }

        val crds = getCustomResourceDefinitions().associateBy { it.identifier }
        // here the group is null for a lot of resources (Kubernetes standard resources), for two singular is null and verbs are always set
        val resources = apiResourceByIdentifier.map { (identifier, apiResources) ->
            val group = apiResources.firstNotNullOf { it.first }
            val versions = apiResources.map { it.second }
            map(group.takeUnless { it.isNullOrBlank() }, versions, apiResources.firstNotNullOf { it.third }, crds)
        }

        this.allAvailableResourceTypes = resources

        return resources
    }

    fun map(group: String?, versions: List<String>, apiResource: APIResource, crds: Map<String, KubernetesResource>): KubernetesResource {
        val name = apiResource.name
        val identifier = KubernetesResource.createIdentifier(group, name)
        val matchingCrd = crds[identifier]

        return KubernetesResource(
            group = group,
            // we can only know for sure for CustomResourceDefinitions which the storageVersion is, for the standard k8s resources we just say it's the latest one
            storageVersion = matchingCrd?.storageVersion ?: findLatestVersion(versions),
            name = name,
            kind = apiResource.kind,
            isNamespaced = apiResource.namespaced,
            isCustomResourceDefinition = matchingCrd != null,
            singularName = apiResource.singularName.takeUnless { it.isNullOrBlank() },
            shortNames = apiResource.shortNames.takeUnless { it.isEmpty() },
            verbs = apiResource.verbs.orEmpty().mapNotNull { Verb.getByName(it) }
                .sorted(),
            servedVersions = versions
        )
    }

    private fun findLatestVersion(versions: List<String>): String {
        if (versions.size == 1) {
            return versions.first()
        }

        val versionNamesByLength = versions.groupBy { it.length }.toSortedMap()
        val shortestVersionNames = versionNamesByLength[versionNamesByLength.firstKey()]!!

        return shortestVersionNames.maxOf { it }
    }


    fun getResource(group: String?, name: String): KubernetesResource? {
        val resource = getAllAvailableResourceTypes().firstOrNull { it.group == group && it.name == name }

        if (resource == null) {
            log.error { "Could not find resource for group = $group and name = $name. Should never happen." }
        }

        return resource
    }

    fun getResourceItems(resource: KubernetesResource): List<ResourceItem> {
        val context = getResourceContext(resource)

        return listItems(resource, client.genericKubernetesResources(context))
    }

    fun watchResourceItems(resource: KubernetesResource, namespace: String? = null, update: (List<ResourceItem>) -> Unit) {
        val context = getResourceContext(resource)
        val resources = client.genericKubernetesResources(context).let {
            if (namespace !=  null) {
                it.inNamespace(namespace)
            } else {
                it
            }
        }

        resources.watch(KubernetesResourceWatcher<GenericKubernetesResource> { action, item ->
            log.info { "Retrieved resource update: $action ${item.metadata.name}" }

            update(getResourceItems(resource)) // TODO: make diff update instead of fetching all items again
        })
    }

    private fun getResourceContext(resource: KubernetesResource): ResourceDefinitionContext =
        ResourceDefinitionContext.Builder()
            .withGroup(resource.group)
            .withVersion(resource.storageVersion)
            .withPlural(resource.name)
            .withKind(resource.kind)
            .withNamespaced(resource.isNamespaced)
            .build()

    fun getResourceItemsResponse(resource: KubernetesResource): String? {
        return if (resource.containsVerb(Verb.list) == false) {
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
                    client.raw("/api/${resource.storageVersion}/${resource.name}")
                } else {
                    client.raw("/apis/${resource.group}/${resource.storageVersion}/${resource.name}")
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
        try {
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
        } catch (e: Exception) {
            log.error(e) { "Could not get items for resource '$resource'" }
            emptyList()
        }


    fun getLogs(podName: String, podNamespace: String, containerName: String? = null, sinceTimeUtc: ZonedDateTime? = null): List<String> =
        getLoggable(podNamespace, podName, containerName)
            .sinceTime((sinceTimeUtc ?: Instant.now().atOffset(ZoneOffset.UTC).minusMinutes(10)).toString())
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