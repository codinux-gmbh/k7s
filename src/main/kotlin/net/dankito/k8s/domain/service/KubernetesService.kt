package net.dankito.k8s.domain.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder
import io.fabric8.kubernetes.client.*
import io.fabric8.kubernetes.client.dsl.*
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import net.codinux.log.logger
import net.dankito.k8s.domain.model.*
import net.dankito.k8s.domain.model.KubernetesResource
import net.dankito.k8s.domain.model.stats.StatsSummary
import net.dankito.k8s.domain.service.mapper.ModelMapper
import java.io.Closeable
import java.io.InputStream
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap

@Singleton
class KubernetesService(
    private val kubeConfigs: KubeConfigs,
    private val mapper: ModelMapper,
    private val objectMapper: ObjectMapper
) {

    companion object {
        val LoggableResourceKinds = hashSetOf(
            "Pod", "Deployment", "StatefulSet", "DaemonSet", "ReplicaSet", "Job"
        )

        val ResourcesWithStats = hashSetOf(
            "Pod", "Node", "PersistentVolumeClaim"
        )

        val NonNullDefaultContextName = "__<default>__"

        private val StatsCacheDuration = Duration.ofMinutes(1)
    }


    val contextsNames: List<String> = kubeConfigs.contextConfigs.keys.sorted()

    val defaultContext = kubeConfigs.defaultContext ?: NonNullDefaultContextName // ConcurrentHashMap throws an error on null keys

    private val clientForContext = ConcurrentHashMap<String, KubernetesClient>()

    private val allAvailableResourceTypes = ConcurrentHashMap<String, List<KubernetesResource>>()

    private val cachedStats: Cache<String, Map<String, StatsSummary?>?> = Caffeine.newBuilder()
        .expireAfterWrite(StatsCacheDuration)
        .build()

    private val log by logger()


    // for deprecated API groups see https://kubernetes.io/docs/reference/using-api/deprecation-guide/
    fun getNamespaces(context: String? = null) = listItems(namespaceResource, getClient(context).namespaces())

    fun getPods(context: String? = null, namespace: String? = null) = listItems(podResource, getClient(context).pods(), namespace, context)

    fun getServices(context: String? = null, namespace: String? = null) = listItems(serviceResource, getClient(context).services(), namespace)

    fun getNodes(context: String? = null) = listItems(nodeResource, getClient(context).nodes(), contextForStats = context)

    val namespaceResource by lazy { getResource(null, "namespaces")!! }

    val nodeResource by lazy { getResource(null, "nodes")!! }

    val podResource by lazy { getResource(null, "pods")!! }

    val serviceResource by lazy { getResourceByName("services")!! }

    val configMapResource by lazy { getResource(null, "configmaps")!! }

    val secretResource by lazy { getResource(null, "secrets")!! }

    val serviceAccountResource by lazy { getResource(null, "serviceaccounts")!! }

    val persistentVolumeResource by lazy { getResource(null, "persistentvolumes")!! }

    val persistentVolumeClaimResource by lazy { getResource(null, "persistentvolumeclaims")!! }


    private fun getClient(context: String?): KubernetesClient {
        val contextToSearch = context ?: defaultContext

        return clientForContext.getOrPut(contextToSearch) {
            if (contextToSearch == null || contextToSearch == NonNullDefaultContextName) { // e.g. in Kubernetes clusters where there is no context available
                KubernetesClientBuilder().build()
            } else {
                KubernetesClientBuilder().withConfig(kubeConfigs.contextConfigs[contextToSearch]).build()
            }
        }
    }

    fun getCustomResourceDefinitions(context: String? = null): List<KubernetesResource> {
        val crds = getClient(context).apiextensions().v1().customResourceDefinitions().list().items

        return mapper.mapCustomResourceDefinitions(crds)
    }

    fun getAllAvailableResourceTypes(context: String? = null): List<KubernetesResource> =
        allAvailableResourceTypes.getOrPut(context ?: defaultContext) { retrieveAllAvailableResourceTypes(context) }

    private fun retrieveAllAvailableResourceTypes(context: String? = null): List<KubernetesResource> {
        val apiResourceByIdentifier = mutableMapOf<String, MutableList<Triple<String, String, APIResource>>>()
        getClient(context).visitResources { group, version, apiResource, _ ->
            apiResourceByIdentifier.getOrPut(KubernetesResource.createIdentifier(group, apiResource.name), { mutableListOf() })
                .add(Triple(group, version, apiResource))

            ApiVisitor.ApiVisitResult.CONTINUE
        }

        return mapper.mapResourceTypes(apiResourceByIdentifier, getCustomResourceDefinitions(context))
    }


    fun getResource(group: String?, name: String, context: String? = null): KubernetesResource? {
        val resources = getAllAvailableResourceTypes(context).filter { it.group == group && it.name == name }

        if (resources.isEmpty()) {
            log.error { "Could not find resource for group = $group and name = $name. Should never happen." }
        }

        return findBestAvailableResource(resources)
    }

    fun getResourceByName(resourceName: String, context: String? = null): KubernetesResource? {
        val resources = getAllAvailableResourceTypes(context).filter { it.name == resourceName }

        if (resources.isEmpty()) {
            log.error { "Could not find resource with name '$resourceName'. Are you sure that it exists?." }
        }

        return findBestAvailableResource(resources)
    }

    private fun findBestAvailableResource(resources: List<KubernetesResource>): KubernetesResource {
        if (resources.size == 1) {
            return resources.first()
        }

        return resources.first { it.version == it.storageVersion }
    }

    fun getResourceItems(resource: KubernetesResource, context: String? = null, namespace: String? = null): ResourceItems? =
        listItems(resource, getOperationForResource(resource, context) as AnyNamespaceOperation<HasMetadata, KubernetesResourceList<HasMetadata>, *>, namespace)

    private fun getOperationForResource(resource: KubernetesResource, context: String? = null): NonNamespaceOperation<*, *, *> {
        val client = getClient(context)

        return if (resource == podResource) {
            client.pods()
        } else if (resource == serviceResource) {
            client.services()
        } else if (resource == namespaceResource) {
            client.namespaces()
        } else if (resource == nodeResource) {
            client.nodes()
        } else if (resource == configMapResource) {
            client.configMaps()
        } else if (resource == secretResource) {
            client.secrets()
        } else if (resource == serviceAccountResource) {
            client.serviceAccounts()
        } else if (resource == persistentVolumeResource) {
            client.persistentVolumes()
        } else if (resource == persistentVolumeClaimResource) {
            client.persistentVolumeClaims()
        } else if (resource.name == "ingresses") {
            if (resource.group == "extensions") {
                client.extensions().ingresses()
            } else if (resource.version == "v1beta1") {
                client.network().v1beta1().ingresses()
            } else {
                client.network().v1().ingresses()
            }
        } else if (resource.name == "deployments") {
            if (resource.group == "extensions") {
                client.extensions().deployments()
            } else {
                client.apps().deployments() // TODO: where are apps/v1beta1 and apps/v1beta2 versions of deployments?
            }
        } else {
            getGenericResources(resource, context, null)
        }
    }

    fun watchResourceItems(resource: KubernetesResource, context: String? = null, namespace: String? = null, resourceVersion: String? = null, update: (WatchAction, ResourceItem, Int?) -> Boolean): Closeable? {
        if (resource.isWatchable == false) {
            return null // a not watchable resource like Binding, ComponentStatus, NodeMetrics, PodMetrics, ...
        }

        val resources = getOperationForResource(resource, context).let { operation ->
            if (namespace != null && operation is MixedOperation<*, *, *>) {
                operation.inNamespace(namespace)
            } else if (operation is MixedOperation<*, *, *>) {
                operation.inAnyNamespace() // in Kubernetes cluster otherwise only resource items of default namespace are returned, not that one of all namespaces
            } else {
                operation
            }
        } as MixedOperation<HasMetadata, KubernetesResourceList<HasMetadata>, *>

        val options = ListOptions().apply {
            resourceVersion?.let { this.resourceVersion = it }
        }

        var watch: Watch? = null
        // Kubernetes 1.27 introduced sendInitialEvents as an alpha feature, so we have to wait till this is broadly available (https://kubernetes.io/docs/reference/using-api/api-concepts/#streaming-lists)
        watch = resources.watch(options, KubernetesResourceWatcher<HasMetadata> { action, item ->
            if (action == Watcher.Action.ERROR) {
                log.warn { "An error occurred for resource watcher of $resource, closing it" }
                watch?.close() // TODO: check via callback if SSESinkEvent is already closed, otherwise restart watch
            } else if (action == Watcher.Action.ADDED) {
                val allItems = resources.list().items.map { "${it.metadata.namespace}/${it.metadata.name}" }
                val insertionIndex = allItems.indexOf("${item.metadata.namespace}/${item.metadata.name}")
                update(WatchAction.Added, mapper.mapResourceItem(item), insertionIndex) // stats retrieval is not senseful as for newly created resources there are no stats yet
            }  else if (action == Watcher.Action.MODIFIED) {
                val stats = if (isResourceWithStats(resource)) getStats(emptyList<Node>(), context, forceStatsRetrievalForItem(resource, item)) else null
                update(WatchAction.Modified, mapper.mapResourceItem(item, stats), null)
            } else if (action == Watcher.Action.DELETED) {
                update(WatchAction.Deleted, mapper.mapResourceItem(item), null)
            } else {
                log.warn { "Retrieved watch event of unhandled type '$action'. This should never happen." }
            }
        })

        return watch
    }

    private fun forceStatsRetrievalForItem(resource: KubernetesResource, item: HasMetadata): Boolean {
        if (isResourceWithStats(resource) == false) {
            return false
        }

        val creationTimestamp = Instant.parse(item.metadata.creationTimestamp)
        val durationSinceCreationTime = Duration.between(Instant.now(), creationTimestamp)

        return durationSinceCreationTime < StatsCacheDuration
    }

    private fun getGenericResources(resource: KubernetesResource, context: String?, namespace: String?) =
        getClient(context).genericKubernetesResources(getResourceContext(resource)).let {
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

    fun getResourceItemsResponse(resource: KubernetesResource, context: String? = null): String? {
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
                    getClient(context).raw("/api/${resource.storageVersion}/${resource.name}")
                } else {
                    getClient(context).raw("/apis/${resource.group}/${resource.storageVersion}/${resource.name}")
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
        namespace: String? = null,
        contextForStats: String? = null
    ): ResourceItems? =
        try {
            val listable = operation.let {
                if (namespace != null && operation is MixedOperation<T, L, *>) {
                    operation.inNamespace(namespace)
                } else if (operation is MixedOperation<T, L, *>) {
                    operation.inAnyNamespace() // in Kubernetes cluster otherwise only resource items of default namespace are returned, not that one of all namespaces
                } else {
                    operation
                }
            }
                .list()

            val items = listable.items.let { it as List<T> }
            val stats = if (isResourceWithStats(resource)) getStats(items, contextForStats) else null
            val mappedItems = items.map { mapper.mapResourceItem(it, stats) }
            ResourceItems(listable.metadata.resourceVersion, mappedItems)
        } catch (e: Exception) {
            log.error(e) { "Could not get items for resource '$resource'" }
            null
        }

    private fun isResourceWithStats(resource: KubernetesResource): Boolean =
        isResourceWithStats(resource.kind)

    private fun isResourceWithStats(resourceKind: String): Boolean =
        ResourcesWithStats.contains(resourceKind)

    private fun <T> getStats(items: List<T>, context: String?, forceRetrieval: Boolean = false): Map<String, StatsSummary?>? {
        if (forceRetrieval == false) {
            cachedStats.asMap()[context ?: defaultContext]?.let { statsFromCache ->
                return statsFromCache
            }
        }

        val stats = retrieveStats(items, context)
        cachedStats.put(context ?: defaultContext, stats)

        return stats
    }

    private fun <T> retrieveStats(items: List<T>, context: String?): Map<String, StatsSummary?>? = runBlocking(Dispatchers.IO) {
        val nodes = if (items.isNotEmpty() && items.all { it is Node }) items as List<Node> else getClient(context).nodes().list().items
        val nodeNames = nodes.map { it.metadata.name }

        val stats = nodeNames.map { nodeName ->
            async(Dispatchers.IO) {
                try {
                    // see https://kubernetes.io/docs/reference/instrumentation/node-metrics/
                    val statsResponse = getClient(context).raw("/api/v1/nodes/$nodeName/proxy/stats/summary")
                    if (statsResponse != null) {
                        return@async nodeName to objectMapper.readValue<StatsSummary>(statsResponse)
                    }
                } catch (e: Throwable) {
                    log.error(e) { "Could not get stats for nodeName $nodeName" }
                }

                nodeName to null
            }
        }.awaitAll().toMap()

        stats.takeUnless { it.isEmpty() || it.values.all { it == null } }
    }

    fun patchResourceItem(resourceName: String, namespace: String?, itemName: String, context: String? = null, scaleTo: Int? = null): Boolean {
        val resource = getResourceByName(resourceName, context)
        if (resource != null) {
            if (scaleTo != null && scaleTo > -1 && resource.isScalable) {
                // TODO: check for older versions e.g. extensions/deployment
                val result = if (resource.name == "deployments") {
                    getClient(context).apps().deployments().inNamespace(namespace).withName(itemName).edit { item ->
                        DeploymentBuilder(item).editSpec().withReplicas(scaleTo).endSpec().build()
                    }
                } else if (resource.name == "statefulsets") {
                    getClient(context).apps().statefulSets().inNamespace(namespace).withName(itemName).edit { item ->
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

    fun deleteResourceItem(resourceName: String, namespace: String?, itemName: String, context: String? = null): Boolean {
        val resource = getResourceByName(resourceName, context)
        if (resource != null) {
            val statuses = getGenericResources(resource, context, namespace).withName(itemName).delete()

            return statuses.all { it.causes.isNullOrEmpty() }
        }

        return false
    }


    fun getLogs(resourceKind: String, namespace: String, itemName: String, containerName: String? = null, context: String? = null, sinceTimeUtc: ZonedDateTime? = null): List<String> =
        getLoggable(resourceKind, namespace, itemName, containerName, context)
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

    fun watchLogs(resourceKind: String, namespace: String, itemName: String, containerName: String? = null, context: String? = null, sinceTimeUtc: ZonedDateTime? = null): InputStream? =
        getLoggable(resourceKind, namespace, itemName, containerName, context)
            .sinceTime((sinceTimeUtc ?: Instant.now().atOffset(ZoneOffset.UTC)).toString())
            // if the pods of an (old) RollableScalableResource like Deployment, ReplicaSet, ... don't exist anymore then watchLog() returns null
            .watchLog()?.output

    // oh boy is that ugly code!
    private fun getLoggable(resourceKind: String, namespace: String, itemName: String, containerName: String?, context: String? = null): TimeTailPrettyLoggable =
        ((getLoggableForResource(resourceKind, context).inNamespace(namespace) as Nameable<*>).withName(itemName) as TimeTailPrettyLoggable).let {
            if (containerName != null && it is PodResource) {
                it.inContainer(containerName) as TimeTailPrettyLoggable
            } else {
                it
            }
        }

    //private fun <E, L : KubernetesResourceList<E>, T> getLoggableForResource(resourceKind: String): MixedOperation<out E, out L, out T> where T : Resource<E>, T : Loggable = when (resourceKind) {
    private fun getLoggableForResource(resourceKind: String, context: String? = null): Namespaceable<*> = when (resourceKind) {
        "pods" -> getClient(context).pods()
        "deployments" -> getClient(context).apps().deployments()
        "statefulsets" -> getClient(context).apps().statefulSets()
        "daemonsets" -> getClient(context).apps().daemonSets()
        "replicasets" -> getClient(context).apps().replicaSets()
        "jobs" -> getClient(context).batch().v1().jobs()
        else -> throw IllegalArgumentException("Trying to get Loggable for resource '$resourceKind' which is not loggable")
    } as Namespaceable<*>

}