package net.dankito.k8s.domain.service

import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder
import io.fabric8.kubernetes.client.ApiVisitor
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.dsl.*
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

    companion object {
        val LoggableResourceNames = hashSetOf(
            "pods", "deployments", "statefulsets", "daemonsets", "replicasets", "jobs"
        )
    }


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

    fun getResourceByName(resourceName: String): KubernetesResource? {
        val resource = getAllAvailableResourceTypes().firstOrNull { it.name == resourceName }

        if (resource == null) {
            log.error { "Could not find resource with name '$resourceName'. Are you sure that it exists?." }
        }

        return resource
    }

    fun getResourceItems(resource: KubernetesResource, namespace: String? = null): List<ResourceItem> {
        return listItems(resource, getGenericResources(resource, namespace))
    }

    fun watchResourceItems(resource: KubernetesResource, namespace: String? = null, update: (List<ResourceItem>) -> Unit) {
        val resources = getGenericResources(resource, namespace)

        resources.watch(KubernetesResourceWatcher<GenericKubernetesResource> { _, _ ->
            update(getResourceItems(resource)) // TODO: make diff update instead of fetching all items again
        })
    }

    private fun getGenericResources(resource: KubernetesResource, namespace: String?) =
        client.genericKubernetesResources(getResourceContext(resource)).let {
            if (namespace != null) {
                it.inNamespace(namespace)
            } else {
                it
            }
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
                    ResourceItem(item.metadata.name, item.metadata.namespace?.takeUnless { it.isBlank() })
                }
        } catch (e: Exception) {
            log.error(e) { "Could not get items for resource '$resource'" }
            emptyList()
        }

    fun patchResourceItem(resourceName: String, namespace: String?, itemName: String, scaleTo: Int? = null): Boolean {
        val resource = getResourceByName(resourceName)
        if (resource != null) {
            if (scaleTo != null && scaleTo > -1 && resource.isScalable) {
                // TODO: check for older versions e.g. extensions/deployment
                val result = if (resource.name == "deployments") {
                    client.apps().deployments().inNamespace(namespace).withName(itemName).edit { item ->
                        DeploymentBuilder(item).editSpec().withReplicas(scaleTo).endSpec().build()
                    }
                } else if (resource.name == "statefulsets") {
                    client.apps().statefulSets().inNamespace(namespace).withName(itemName).edit { item ->
                        StatefulSetBuilder(item).editSpec().withReplicas(scaleTo).endSpec().build()
                    }
                } else {
                    null
                }

                return result != null
            }
        }

        return false
    }

    fun deleteResourceItem(resourceName: String, namespace: String?, itemName: String): Boolean {
        val resource = getResourceByName(resourceName)
        if (resource != null) {
            val statuses = getGenericResources(resource, namespace).withName(itemName).delete()

            return statuses.all { it.causes.isNullOrEmpty() }
        }

        return false
    }


    fun getLogs(resourceKind: String, namespace: String, itemName: String, containerName: String? = null, sinceTimeUtc: ZonedDateTime? = null): List<String> =
        getLoggable(resourceKind, namespace, itemName, containerName)
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

    fun watchLogs(resourceKind: String, namespace: String, itemName: String, containerName: String? = null, sinceTimeUtc: ZonedDateTime? = null): InputStream? =
        getLoggable(resourceKind, namespace, itemName, containerName)
            .sinceTime((sinceTimeUtc ?: Instant.now().atOffset(ZoneOffset.UTC)).toString())
            // if the pods of an (old) RollableScalableResource like Deployment, ReplicaSet, ... don't exist anymore then watchLog() returns null
            .watchLog()?.output

    // oh boy is that ugly code!
    private fun getLoggable(resourceKind: String, namespace: String, itemName: String, containerName: String?): TimeTailPrettyLoggable =
        ((getLoggableForResource(resourceKind).inNamespace(namespace) as Nameable<*>).withName(itemName) as TimeTailPrettyLoggable).let {
            if (containerName != null && it is PodResource) {
                it.inContainer(containerName) as TimeTailPrettyLoggable
            } else {
                it
            }
        }

    //private fun <E, L : KubernetesResourceList<E>, T> getLoggableForResource(resourceKind: String): MixedOperation<out E, out L, out T> where T : Resource<E>, T : Loggable = when (resourceKind) {
    private fun getLoggableForResource(resourceKind: String): Namespaceable<*> = when (resourceKind) {
        "pods" -> client.pods()
        "deployments" -> client.apps().deployments()
        "statefulsets" -> client.apps().statefulSets()
        "daemonsets" -> client.apps().daemonSets()
        "replicasets" -> client.apps().replicaSets()
        "jobs" -> client.batch().v1().jobs()
        else -> throw IllegalArgumentException("Trying to get Loggable for resource '$resourceKind' which is not loggable")
    } as Namespaceable<*>

}