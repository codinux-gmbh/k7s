package net.dankito.k8s.domain.service

import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder
import io.fabric8.kubernetes.api.model.metrics.v1beta1.NodeMetricsList
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetricsList
import io.fabric8.kubernetes.api.model.networking.v1.Ingress
import io.fabric8.kubernetes.client.ApiVisitor
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.dsl.*
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext
import jakarta.inject.Singleton
import net.codinux.log.logger
import net.dankito.k8s.domain.model.ContainerStatus
import net.dankito.k8s.domain.model.KubernetesResource
import net.dankito.k8s.domain.model.PodResourceItem
import net.dankito.k8s.domain.model.ResourceItem
import net.dankito.k8s.domain.model.Verb
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Singleton
class KubernetesService(
    private val client: KubernetesClient
) {

    companion object {
        val LoggableResourceKinds = hashSetOf(
            "Pod", "Deployment", "StatefulSet", "DaemonSet", "ReplicaSet", "Job"
        )
    }


    private lateinit var customResourceDefinitions: List<KubernetesResource>

    private lateinit var allAvailableResourceTypes: List<KubernetesResource>

    private var isMetricsApiAvailable: Boolean? = null

    private val log by logger()


    // for deprecated API groups see https://kubernetes.io/docs/reference/using-api/deprecation-guide/
    fun getNamespaces() = listItems(namespaceResource, client.namespaces())

    fun getPods(namespace: String? = null) = listItems(podResource, client.pods(), namespace, getMetrics(client.top().pods(), namespace))

    fun getServices(namespace: String? = null) = listItems(serviceResource, client.services(), namespace)

    val namespaceResource by lazy { getResource(null, "namespaces")!! }

    val nodeResource by lazy { getResource(null, "nodes")!! }

    val podResource by lazy { getResource(null, "pods")!! }

    val serviceResource by lazy { getResourceByName("services")!! }

    val configMapResource by lazy { getResource(null, "configmaps")!! }

    val secretResource by lazy { getResource(null, "secrets")!! }

    val serviceAccountResource by lazy { getResource(null, "serviceaccounts")!! }

    val persistentVolumeResource by lazy { getResource(null, "persistentvolumes")!! }

    val persistentVolumeClaimResource by lazy { getResource(null, "persistentvolumeclaims")!! }


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
        val resources = getAllAvailableResourceTypes().filter { it.group == group && it.name == name }

        if (resources.isEmpty()) {
            log.error { "Could not find resource for group = $group and name = $name. Should never happen." }
        }

        return findBestAvailableResource(resources)
    }

    fun getResourceByName(resourceName: String): KubernetesResource? {
        val resources = getAllAvailableResourceTypes().filter { it.name == resourceName }

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

    fun getResourceItems(resource: KubernetesResource, namespace: String? = null): List<ResourceItem> {
        return if (resource == podResource) {
            getPods(namespace)
        } else if (resource == serviceResource) {
            getServices(namespace)
        } else if (resource == namespaceResource) {
            listItems(namespaceResource, client.namespaces())
        } else if (resource == nodeResource) {
            listItems(namespaceResource, client.nodes(), metrics = getMetrics(client.top().nodes()))
        } else if (resource == configMapResource) {
            listItems(namespaceResource, client.configMaps(), namespace)
        } else if (resource == secretResource) {
            listItems(namespaceResource, client.secrets(), namespace)
        } else if (resource == serviceAccountResource) {
            listItems(namespaceResource, client.serviceAccounts(), namespace)
        } else if (resource == persistentVolumeResource) {
            listItems(namespaceResource, client.persistentVolumes(), namespace)
        } else if (resource == persistentVolumeClaimResource) {
            listItems(namespaceResource, client.persistentVolumeClaims(), namespace)
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
            listItems(resource, getGenericResources(resource, namespace))
        }
    }

    fun watchResourceItems(resource: KubernetesResource, namespace: String? = null, update: (List<ResourceItem>) -> Unit) {
        if (resource.isWatchable == false) {
            return // a not watchable resource like Binding, ComponentStatus, NodeMetrics, PodMetrics, ...
        }

        val resources = getGenericResources(resource, namespace)

        resources.watch(KubernetesResourceWatcher<GenericKubernetesResource> { _, _ ->
            update(getResourceItems(resource, namespace)) // TODO: make diff update instead of fetching all items again
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
        namespace: String? = null,
        metrics: KubernetesResourceList<HasMetadata>? = null
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
                mapResourceItem(item, metrics)
            }
        } catch (e: Exception) {
            log.error(e) { "Could not get items for resource '$resource'" }
            emptyList()
        }

    private fun <T : HasMetadata> mapResourceItem(item: T, metrics: KubernetesResourceList<HasMetadata>? = null): ResourceItem {
        val name = item.metadata.name
        val namespace = item.metadata.namespace?.takeUnless { it.isBlank() }
        val additionalValues = getAdditionalValues(item, metrics)

        return if (item is Pod) {
            val status = item.status
            PodResourceItem(name, namespace, status.phase, status.podIP, additionalValues, status.containerStatuses.map {
                ContainerStatus(it.name, try { it.containerID } catch (ignored: Exception) { null }, it.image, it.imageID, it.restartCount, it.started, it.ready, it.state.waiting != null, it.state.running != null, it.state.terminated != null)
            })
        } else {
            ResourceItem(name, namespace, additionalValues)
        }
    }

    private fun <T> getAdditionalValues(item: T, metrics: KubernetesResourceList<HasMetadata>? = null): Map<String, String?> {
        return if (item is Pod) {
            val emptyValue = if (metrics is PodMetricsList) "0" else "n/a"
            buildMap {
                put("IP", item.status.podIP)
                put("HostIP", item.status.hostIP)
                put("CPU", emptyValue)
                put("Mem", emptyValue)
                if (metrics is PodMetricsList) {
                    metrics.items.orEmpty().firstOrNull { it.metadata.name == item.metadata.name && it.metadata.namespace == item.metadata.namespace }?.let { podMetrics ->
                        put("CPU", toDisplayValue(podMetrics.containers.orEmpty().sumOf { toMilliCore(it.usage["cpu"]) ?: BigDecimal.ZERO }))
                        put("Mem", toDisplayValue(podMetrics.containers.orEmpty().sumOf { toMiByte(it.usage["memory"]) ?: BigDecimal.ZERO }))
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
            val emptyValue = if (metrics is PodMetricsList) "0" else "n/a"

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

                if (metrics is NodeMetricsList) {
                    metrics.items.orEmpty().firstOrNull { it.metadata.name == item.metadata.name }?.let { nodeMetrics ->
                        val cpu = toMilliCore(nodeMetrics.usage?.get("cpu"))
                        this["CPU"] = toDisplayValue(cpu)
                        this["%CPU"] = toDisplayValue((cpu ?: BigDecimal.ZERO).multiply(BigDecimal.valueOf(100)).divide(availableCpu, 0, RoundingMode.DOWN))

                        val memory = toMiByte(nodeMetrics.usage?.get("memory"))
                        this["Mem"] = toDisplayValue(memory)
                        this["%Mem"] = toDisplayValue((memory ?: BigDecimal.ZERO).multiply(BigDecimal.valueOf(100)).divide(availableMemory, 0, RoundingMode.DOWN))
                    }
                }
            }
        } else {
            emptyMap()
        }
    }

    private fun toMilliCore(cpu: Quantity?): BigDecimal? = cpu?.let {
        when (cpu.format) {
            "n" -> cpu.numericalAmount.multiply(BigDecimal.valueOf(1_000))
            "" -> cpu.numericalAmount.multiply(BigDecimal.valueOf(1_000))
            else -> cpu.numericalAmount
        }
    }

    private fun toMiByte(memory: Quantity?): BigDecimal? = memory?.let {
        when (memory.format) {
            "Ki" -> memory.numericalAmount?.divide(BigDecimal.valueOf(1_024 * 1_024))
            else -> memory.numericalAmount
        }
    }

    private fun toDisplayValue(metricValue: BigDecimal?): String? = metricValue?.let {
        metricValue.setScale(0, RoundingMode.UP).toString()
    }

    private fun getMetrics(metricOperation: MetricOperation<*, *>, namespace: String? = null): KubernetesResourceList<HasMetadata>? {
        if (this.isMetricsApiAvailable == false) {
            return null
        }

        return try {
            // TODO: what an ugly code
            val operation = if (namespace != null && metricOperation is Namespaceable<*>) {
                metricOperation.inNamespace(namespace) as MetricOperation<*, *>
            } else {
                metricOperation
            }

            val result = operation.metrics() as? KubernetesResourceList<HasMetadata>
            if (result != null) {
                isMetricsApiAvailable = true
            }

            result
        } catch (e: Throwable) {
            log.error(e) { "Could not fetch metrics" }
            if (isMetricsApiAvailable == null) {
                isMetricsApiAvailable = false // TODO: this is not fully correct, check kind of error as soon as correct error code (e.g. 404) is known
            }
            null
        }
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