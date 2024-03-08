package net.dankito.k8s.domain.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder
import io.fabric8.kubernetes.api.model.networking.v1.Ingress
import io.fabric8.kubernetes.client.ApiVisitor
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.fabric8.kubernetes.client.dsl.*
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext
import jakarta.inject.Singleton
import kotlinx.coroutines.*
import net.codinux.log.logger
import net.dankito.k8s.domain.model.*
import net.dankito.k8s.domain.model.ContainerStatus
import net.dankito.k8s.domain.model.KubernetesResource
import net.dankito.k8s.domain.model.stats.StatsSummary
import net.dankito.k8s.domain.model.stats.VolumeStats
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap

@Singleton
class KubernetesService(
    private val kubeConfigs: KubeConfigs,
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
    }


    val contextsNames: List<String> = kubeConfigs.contextConfigs.keys.sorted()

    val defaultContext = kubeConfigs.defaultContext ?: NonNullDefaultContextName // ConcurrentHashMap throws an error on null keys

    private val clientForContext = ConcurrentHashMap<String, KubernetesClient>()

    private val allAvailableResourceTypes = ConcurrentHashMap<String, List<KubernetesResource>>()

    private val isMetricsApiAvailable = ConcurrentHashMap<String, Boolean>()

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
        }
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

        val crds = getCustomResourceDefinitions().associateBy { it.identifier }
        // here the group is null for a lot of resources (Kubernetes standard resources), for two singular is null and verbs are always set
        val resources = apiResourceByIdentifier.map { (identifier, apiResources) ->
            val group = apiResources.firstNotNullOf { it.first }
            val versions = apiResources.map { it.second }
            map(group.takeUnless { it.isNullOrBlank() }, versions, apiResources.firstNotNullOf { it.third }, crds)
        }

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

    fun getResourceItems(resource: KubernetesResource, context: String? = null, namespace: String? = null): ResourceItems? {
        val client = getClient(context)

        return if (resource == podResource) {
            getPods(context, namespace)
        } else if (resource == serviceResource) {
            getServices(context, namespace)
        } else if (resource == namespaceResource) {
            getNamespaces(context)
        } else if (resource == nodeResource) {
            getNodes(context)
        } else if (resource == configMapResource) {
            listItems(configMapResource, client.configMaps(), namespace)
        } else if (resource == secretResource) {
            listItems(secretResource, client.secrets(), namespace)
        } else if (resource == serviceAccountResource) {
            listItems(serviceAccountResource, client.serviceAccounts(), namespace)
        } else if (resource == persistentVolumeResource) {
            listItems(persistentVolumeResource, client.persistentVolumes(), namespace)
        } else if (resource == persistentVolumeClaimResource) {
            listItems(persistentVolumeClaimResource, client.persistentVolumeClaims(), namespace, context)
        } else if (resource.name == "ingresses") {
            if (resource.group == "extensions") {
                listItems(resource, client.extensions().ingresses(), namespace)
            } else if (resource.version == "v1beta1") {
                listItems(resource, client.network().v1beta1().ingresses(), namespace)
            } else {
                listItems(resource, client.network().v1().ingresses(), namespace)
            }
        } else if (resource.name == "deployments") {
            if (resource.group == "extensions") {
                listItems(resource, client.extensions().deployments(), namespace)
            } else {
                listItems(resource, client.apps().deployments(), namespace) // TODO: where are apps/v1beta1 and apps/v1beta2 versions of deployments?
            }
        } else {
            listItems(resource, getGenericResources(resource, context, namespace))
        }
    }

    fun watchResourceItems(resource: KubernetesResource, context: String? = null, namespace: String? = null, resourceVersion: String? = null, update: (ResourceItems) -> Unit) {
        if (resource.isWatchable == false) {
            return // a not watchable resource like Binding, ComponentStatus, NodeMetrics, PodMetrics, ...
        }

        val resources = getGenericResources(resource, context, namespace)

        val options = ListOptions().apply {
            resourceVersion?.let { this.resourceVersion = it }
        }
        resources.watch(options, KubernetesResourceWatcher<GenericKubernetesResource> { _, _ ->
            // TODO: make diff update instead of fetching all items again
            getResourceItems(resource, context, namespace)?.let { update(it) }
        })
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
            val mappedItems = items.map { mapResourceItem(it, stats) }
            ResourceItems(listable.metadata.resourceVersion, mappedItems)
        } catch (e: Exception) {
            log.error(e) { "Could not get items for resource '$resource'" }
            null
        }

    private fun isResourceWithStats(resource: KubernetesResource): Boolean =
        ResourcesWithStats.contains(resource.kind)

    // TODO: cache stats
    private fun <T> getStats(items: List<T>, context: String?): Map<String, StatsSummary?>? = runBlocking(Dispatchers.IO) {
        val nodes = if (items.all { it is Node }) items as List<Node> else getClient(context).nodes().list().items
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

    private fun <T : HasMetadata> mapResourceItem(item: T, stats: Map<String, StatsSummary?>? = null): ResourceItem {
        val name = item.metadata.name
        val namespace = item.metadata.namespace?.takeUnless { it.isBlank() }
        val itemSpecificValues = getItemSpecificValues(item, stats)

        return if (item is Pod) {
            val status = item.status
            PodResourceItem(name, namespace, mapPodStatus(status), status.podIP, itemSpecificValues, status.containerStatuses.map {
                ContainerStatus(it.name, try { it.containerID } catch (ignored: Exception) { null }, it.image, it.imageID, it.restartCount, it.started, it.ready, it.state.waiting != null, it.state.running != null, it.state.terminated != null)
            })
        } else {
            ResourceItem(name, namespace, itemSpecificValues)
        }
    }

    private fun mapPodStatus(status: PodStatus): String {
        val notRunningContainer = if (status.containerStatuses.size == 1) status.containerStatuses.first().takeIf { it.state.running == null }
        else if (status.containerStatuses.size > 1) status.containerStatuses.firstOrNull { it.state.running == null && it.state.terminated?.reason != "Completed" }
        else null

        // if a container is not running, use their state reason as status for whole pod
        if (notRunningContainer != null) {
            /**
             * Possible values of waiting.reason:
             *     ContainerCreating,
             *     CrashLoopBackOff,
             *     ErrImagePull,
             *     ImagePullBackOff,
             *     CreateContainerConfigError,
             *     InvalidImageName,
             *     ErrImageNeverPull
             *     CreateContainerError
             *
             *
             * Possible values of terminated.reason:
             *     OOMKilled,
             *     Error,
             *     Completed,
             *     ContainerCannotRun,
             *     DeadlineExceeded
             *
             */
            val reason = notRunningContainer.state.waiting?.reason ?: notRunningContainer.state.terminated?.reason
            if (reason != null) {
                return reason.replace("ContainerCreating", "Creating")
            }
        }

        return status.phase // else use PodStatus.phase (Pending, Running, Succeeded, Failed, Unknown) as default
    }

    private fun <T> getItemSpecificValues(item: T, stats: Map<String, StatsSummary?>? = null): Map<String, String?> {
        return if (item is Pod) {
            val emptyValue = if (stats.isNullOrEmpty()) "n/a" else "0"
            buildMap {
                put("IP", item.status.podIP)
                put("Host", item.status.hostIP)
                put("CPU", emptyValue)
                put("Mem", emptyValue)
                if (stats.isNullOrEmpty() == false) {
                    val podStats = stats.values.firstNotNullOfOrNull { it?.pods.orEmpty().firstOrNull { it.podRef.name == item.metadata.name && it.podRef.namespace == item.metadata.namespace } }
                    if (podStats != null) {
                        put("CPU", toDisplayValue(toMilliCore(podStats.containers.sumOf { it.cpu?.usageNanoCores ?: 0UL })))
                        put("Mem", toDisplayValue(toMiByte(podStats.containers.sumOf { it.memory?.workingSetBytes ?: 0UL }), RoundingMode.DOWN))

                        val nodeStats = stats.values.firstOrNull { it?.pods?.contains(podStats) == true }?.node
                        if (nodeStats != null) {
                            this["Host"] = nodeStats.nodeName
                        }
                    }
                }
            }
        } else if (item is Service) {
            val spec = item.spec
            mapOf("Type" to spec.type, "ClusterIP" to spec.clusterIP, "ExternalIPs" to spec.externalIPs.joinToString(), "Ports" to spec.ports.joinToString { "${it.name}: ${it.port}►${it.nodePort ?: 0}" })
        } else if (item is Ingress) {
            val spec = item.spec
            mapOf("Class" to spec.ingressClassName, "Hosts" to spec.rules.joinToString { it.host }, "Address" to item.status.loadBalancer.ingress.joinToString { it.hostname }, "Ports" to spec.rules.joinToString { it.http.paths.joinToString { it.backend.service.port.number.toString() } })
        } else if (item is Deployment) {
            val status = item.status
            mapOf("Ready" to "${status.readyReplicas ?: 0}/${status.replicas ?: 0}", "Up-to-date" to "${status.updatedReplicas ?: 0}", "Available" to "${status.availableReplicas ?: 0}")
        } else if (item is ConfigMap) {
            mapOf("Data" to item.data.size.toString())
        } else if (item is Secret) {
            mapOf("Type" to item.type, "Data" to item.data.size.toString())
        } else if (item is Node) {
            val status = item.status
            val availableCpu = toMilliCore(status.capacity?.get("cpu"))
            val availableMemory = toMiByte(status.capacity?.get("memory"))
            val emptyValue = if (stats.isNullOrEmpty()) "n/a" else "0"

            buildMap { // TODO: where to get roles from, like for master: "control-plane,etcd,master"?
                put("Status", status.conditions.firstOrNull { it.status == "True" }?.type)
                put("Taints", item.spec.taints.size.toString())
                put("Version", status.nodeInfo?.kubeletVersion)
                put("Kernel", status.nodeInfo?.kernelVersion)
                put("CPU", emptyValue)
                put("%CPU", emptyValue)
                put("CPU/A", toDisplayValue(availableCpu))
                put("Mem", emptyValue)
                put("%Mem", emptyValue)
                put("Mem/A", toDisplayValue(availableMemory))
                put("Images", status.images.size.toString())
                put("Pods", "n/a")

                if (stats.isNullOrEmpty() == false) {
                    val statsSummaryForNode = stats[item.metadata.name]
                    if (statsSummaryForNode != null) {
                        val nodeStats = statsSummaryForNode.node

                        val cpu = toMilliCore(nodeStats.cpu?.usageNanoCores)
                        this["CPU"] = toDisplayValue(cpu)
                        this["%CPU"] = toDisplayValue((cpu ?: BigDecimal.ZERO).multiply(BigDecimal.valueOf(100)).divide(availableCpu, 0, RoundingMode.DOWN))

                        val memory = toMiByte(nodeStats.memory?.workingSetBytes)
                        this["Mem"] = toDisplayValue(memory) ?: "n/a"
                        this["%Mem"] = toDisplayValue((memory?.let { BigDecimal(memory.toString()) } ?: BigDecimal.ZERO).multiply(BigDecimal.valueOf(100)).divide(availableMemory, 0, RoundingMode.DOWN))

                        this["Pods"] = statsSummaryForNode.pods.size.toString()
                    }
                }
            }
        } else if (item is PersistentVolume) {
            val spec = item.spec
            mapOf(
                "Capacity" to spec.capacity["storage"]?.toString(),
                "Access Modes" to mapAccessModes(spec.accessModes),
                "Reclaim Policy" to spec.persistentVolumeReclaimPolicy,
                "Status" to item.status.phase,
                "Claim" to "${spec.claimRef.namespace}/${spec.claimRef.name}",
                "StorageClass" to spec.storageClassName,
                "Reason" to (item.status.reason ?: "")
            )
        } else if (item is PersistentVolumeClaim) {
            val spec = item.spec
            val volumeStats = if (stats.isNullOrEmpty()) null else stats.values.flatMap { it?.pods.orEmpty().flatMap { it.volume.filter { it.pvcRef != null } } }
                ?.firstOrNull { it.pvcRef!!.name == item.metadata.name && it.pvcRef.namespace == item.metadata.namespace }
            val usedBytes = getUsedBytes(volumeStats)
            mapOf("Status" to item.status.phase, "Volume" to spec.volumeName, "Used Mi" to (toDisplayValue(toMiByte(usedBytes)) ?: "n/a"), "Used %" to (toUsagePercentage(usedBytes, volumeStats?.capacityBytes) ?: "n/a"), "Capacity" to item.status.capacity["storage"]?.toString(), "Access Modes" to mapAccessModes(spec.accessModes), "StorageClass" to spec.storageClassName)
        } else {
            emptyMap()
        }
    }

    private fun mapAccessModes(accessModes: List<String>) = accessModes.joinToString {
            it.replace("ReadWriteOnce", "RWO").replace("ReadOnlyMany", "ROM")
                .replace("ReadWriteMany", "RWM").replace("ReadWriteOncePod", "RWOP")
        }

    private fun toMilliCore(usageNanoCores: ULong?): BigDecimal? = usageNanoCores?.let {
        BigDecimal(usageNanoCores.toString()).divide(BigDecimal.valueOf(1_000_000L))
    }

    private fun toMilliCore(cpu: Quantity?): BigDecimal? = cpu?.let {
        when (cpu.format) {
            "n" -> cpu.numericalAmount.multiply(BigDecimal.valueOf(1_000))
            "" -> cpu.numericalAmount.multiply(BigDecimal.valueOf(1_000))
            else -> cpu.numericalAmount
        }
    }

    private fun toMiByte(bytes: ULong?): BigDecimal? = bytes?.let {
        BigDecimal(bytes.toString()).divide(BigDecimal.valueOf(1_024L * 1_024L))
    }

    private fun toMiByte(memory: Quantity?): BigDecimal? = memory?.let {
        when (memory.format) {
            "Ki" -> memory.numericalAmount?.divide(BigDecimal.valueOf(1_024 * 1_024))
            else -> memory.numericalAmount
        }
    }

    private fun toUsagePercentage(used: ULong?, capacity: ULong?): String? =
        if (used != null && capacity != null) {
            BigDecimal(used.toString()).multiply(BigDecimal.valueOf(100)).divide(BigDecimal(capacity.toString()), 0, RoundingMode.DOWN).toString()
        } else {
            null
        }

    private fun getUsedBytes(volumeStats: VolumeStats?): ULong? = volumeStats?.let {
        if (volumeStats.availableBytes != null && volumeStats.capacityBytes != null) {
            volumeStats.capacityBytes - volumeStats.availableBytes
        } else {
            null
        }
    }

    private fun toDisplayValue(metricValue: BigDecimal?, roundingMode: RoundingMode = RoundingMode.UP): String? = metricValue?.let {
        metricValue.setScale(0, roundingMode).toString()
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