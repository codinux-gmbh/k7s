package net.dankito.k8s.domain.service.mapper

import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.networking.v1.Ingress
import jakarta.inject.Singleton
import net.dankito.k8s.domain.model.*
import net.dankito.k8s.domain.model.ContainerStatus
import net.dankito.k8s.domain.model.KubernetesResource
import net.dankito.k8s.domain.model.stats.StatsSummary
import net.dankito.k8s.domain.model.stats.VolumeStats
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

@Singleton
class ModelMapper {

    fun mapResourceTypes(
        apiResourceByIdentifier: Map<String, MutableList<Triple<String, String, APIResource>>>,
        customResourceDefinitions: List<KubernetesResource>
    ): List<KubernetesResource> {
        val crds = customResourceDefinitions.associateBy { it.identifier }

        // here the group is null for a lot of resources (Kubernetes standard resources), for two singular is null and verbs are always set
        return apiResourceByIdentifier.map { (identifier, apiResources) ->
            val group = apiResources.firstNotNullOf { it.first }
            val versions = apiResources.map { it.second }
            mapResourceTypes(group.takeUnless { it.isNullOrBlank() }, versions, apiResources.firstNotNullOf { it.third }, crds)
        }
    }

    private fun mapResourceTypes(group: String?, versions: List<String>, apiResource: APIResource, crds: Map<String, KubernetesResource>): KubernetesResource {
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


    fun mapCustomResourceDefinitions(crds: List<CustomResourceDefinition>): List<KubernetesResource> = crds.map { crd ->
        mapCustomResourceDefinition(crd)
    }

    private fun mapCustomResourceDefinition(crd: CustomResourceDefinition): KubernetesResource {
        val storageVersion = crd.spec.versions.first { it.storage }

        // here verbs are always null / not set and group and singular are always set
        return KubernetesResource(
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


    fun <T : HasMetadata> mapResourceItem(item: T, stats: Map<String, StatsSummary?>? = null): ResourceItem {
        val name = item.metadata.name
        val namespace = item.metadata.namespace?.takeUnless { it.isBlank() }
        val creationTimestamp = item.metadata.creationTimestamp?.let { Instant.parse(it) }
        val (highlightedItemSpecificValues, secondaryItemSpecificValues) = getItemSpecificValues(item, stats)

        return if (item is Pod) {
            val status = item.status
            val container = status.containerStatuses.map {
                ContainerStatus(it.name, try { it.containerID } catch (ignored: Exception) { null }, it.image, it.imageID, it.restartCount, it.started, it.ready, it.state.waiting != null, it.state.running != null, it.state.terminated != null)
            }
            PodResourceItem(name, namespace, creationTimestamp, mapPodStatus(status), status.podIP, container, highlightedItemSpecificValues, secondaryItemSpecificValues)
        } else {
            ResourceItem(name, namespace, creationTimestamp, highlightedItemSpecificValues, secondaryItemSpecificValues)
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

    private fun <T> getItemSpecificValues(item: T, stats: Map<String, StatsSummary?>? = null): Pair<List<ItemValue>, List<ItemValue>> {
        return if (item is Pod) {
            getItemSpecificValuesForPod(item, stats)
        } else if (item is Service) {
            getItemSpecificValuesForService(item)
        } else if (item is Ingress) {
            getItemSpecificValuesForIngress(item)
        } else if (item is Deployment) {
            getItemSpecificValuesForDeployment(item)
        } else if (item is ConfigMap) {
            listOf(ItemValue("Data", item.data.size.toString(), "${item.data.size} data")) to
                emptyList()
        } else if (item is Secret) {
            listOf(ItemValue("Type", item.type, item.type), ItemValue("Data", item.data.size.toString(), "${item.data.size} data")) to
                emptyList()
        } else if (item is Node) {
            getItemSpecificValuesForNode(item, stats)
        } else if (item is PersistentVolume) {
            getItemSpecificValuesForPersistentVolume(item, stats)
        } else if (item is PersistentVolumeClaim) {
            getItemSpecificValuesForPersistentVolumeClaim(item, stats)
        } else {
            emptyList<ItemValue>() to emptyList()
        }
    }

    private fun getItemSpecificValuesForPod(item: Pod, stats: Map<String, StatsSummary?>? = null): Pair<List<ItemValue>, List<ItemValue>> {
        val emptyValue = if (stats.isNullOrEmpty()) "n/a" else "0"
        val status = item.status
        val countReadyContainers = "${status.containerStatuses.filter { it.ready }.size}/${status.containerStatuses.size}"
        val podStatus = mapPodStatus(status)
        val podStats = stats?.values?.firstNotNullOfOrNull { it?.pods.orEmpty().firstOrNull { it.podRef.name == item.metadata.name && it.podRef.namespace == item.metadata.namespace } }
        val nodeStats = stats?.values?.firstOrNull { it?.pods?.contains(podStats) == true }?.node

        return listOf(ItemValue("Ready", countReadyContainers, countReadyContainers), ItemValue("Status", podStatus, podStatus)) to
                listOf(
                    ItemValue("CPU", toDisplayValue(toMilliCore(podStats?.containers?.sumOf { it.cpu?.usageNanoCores ?: 0UL })) ?: emptyValue),
                    ItemValue("Mem", toDisplayValue(toMiByte(podStats?.containers?.sumOf { it.memory?.workingSetBytes ?: 0UL }), RoundingMode.DOWN) ?: emptyValue),
                    ItemValue("IP", item.status.podIP, item.status.podIP),
                    ItemValue("Host", nodeStats?.nodeName ?: item.status.hostIP, useRemainingSpace = true)
                )
    }

    private fun getItemSpecificValuesForService(item: Service): Pair<List<ItemValue>, List<ItemValue>> {
        val spec = item.spec

        return listOf(ItemValue("Type", spec.type, spec.type)) to
            listOf(
                ItemValue("ClusterIP", spec.clusterIP),
                ItemValue("ExternalIPs", spec.externalIPs.joinToString()),
                ItemValue("Ports", spec.ports.joinToString { "${it.name}: ${it.port}►${it.nodePort ?: 0}" })
            )
    }

    private fun getItemSpecificValuesForIngress(item: Ingress): Pair<List<ItemValue>, List<ItemValue>> {
        val spec = item.spec
        val hosts = spec.rules.joinToString { it.host }

        return listOf(ItemValue("Class", spec.ingressClassName, spec.ingressClassName)) to
            listOf(
                ItemValue("Hosts", hosts, hosts),
                ItemValue("Ports", spec.rules.joinToString { it.http.paths.joinToString { it.backend.service.port.number.toString() } }),
                ItemValue("Address", item.status.loadBalancer.ingress.joinToString { it.hostname })
            )
    }

    private fun getItemSpecificValuesForDeployment(item: Deployment): Pair<List<ItemValue>, List<ItemValue>> {
        val status = item.status
        val countReadyReplicas = "${status.readyReplicas ?: 0}/${status.replicas ?: 0}"
        val countUpdatedReplicas = "${status.updatedReplicas ?: 0}"
        val countAvailableReplicas = "${status.availableReplicas ?: 0}"

        return listOf(ItemValue("Ready", countReadyReplicas, countReadyReplicas), ItemValue("Up-to-date", countUpdatedReplicas, "Updated: $countUpdatedReplicas"), ItemValue("Available", countAvailableReplicas, "Avail: $countAvailableReplicas")) to
                emptyList()
    }

    private fun getItemSpecificValuesForPersistentVolume(item: PersistentVolume, stats: Map<String, StatsSummary?>? = null): Pair<List<ItemValue>, List<ItemValue>> {
        val spec = item.spec
        val accessModes = mapAccessModes(spec.accessModes)
        val capacity = spec.capacity["storage"]?.toString()
        val claim = "${spec.claimRef.namespace}/${spec.claimRef.name}"

        return listOf(ItemValue("Status", item.status.phase, item.status.phase), ItemValue("Access Modes", accessModes, accessModes), ItemValue("Capacity", capacity, capacity)) to
            listOf(
                ItemValue("StorageClass", spec.storageClassName, spec.storageClassName),
                ItemValue("Claim", claim, claim),
                ItemValue("Reclaim Policy", spec.persistentVolumeReclaimPolicy),
                ItemValue("Reason", item.status.reason ?: "", "Reason ${item.status.reason ?: "-"}")
            )
    }

    private fun getItemSpecificValuesForPersistentVolumeClaim(item: PersistentVolumeClaim, stats: Map<String, StatsSummary?>? = null): Pair<List<ItemValue>, List<ItemValue>> {
        val spec = item.spec
        val accessModes = mapAccessModes(spec.accessModes)
        val volumeStats = if (stats.isNullOrEmpty()) null else stats.values.flatMap { it?.pods.orEmpty().flatMap { it.volume.filter { it.pvcRef != null } } }
            ?.firstOrNull { it.pvcRef!!.name == item.metadata.name && it.pvcRef.namespace == item.metadata.namespace }
        val usedBytes = getUsedBytes(volumeStats)
        val usedMi = toDisplayValue(toMiByte(usedBytes)) ?: "n/a"
        val usedPercentage = toUsagePercentage(usedBytes, volumeStats?.capacityBytes) ?: "n/a"
        val capacity = item.status.capacity["storage"]?.toString()

        return listOf(ItemValue("Status", item.status.phase, item.status.phase), ItemValue("Access Modes", accessModes, accessModes), ItemValue("Used", null, "${usedMi}Mi, ${usedPercentage}% of $capacity", showOnDesktop = false)) to
            listOf(
                ItemValue("StorageClass", spec.storageClassName, spec.storageClassName),
                ItemValue("Volume", spec.volumeName, spec.volumeName),
                ItemValue("Used Mi", usedMi, showOnMobile = false),
                ItemValue("Used %", usedPercentage, showOnMobile = false),
                ItemValue("Capacity", capacity, showOnMobile = false)
            )
    }

    private fun getItemSpecificValuesForNode(item: Node, stats: Map<String, StatsSummary?>? = null): Pair<List<ItemValue>, List<ItemValue>> {
        val status = item.status

        // other statuses are: DiskPressure, MemoryPressure, PIDPressure, NetworkUnavailable. See https://kubernetes.io/docs/reference/node/node-status/#condition
        val readyStatus = status.conditions.firstOrNull { it.type == "Ready" }
        val nodeStatus = when (readyStatus?.status) {
            "True" -> "Ready"
            "False" -> "Not Ready"
            null -> readyStatus?.reason ?: "Unknown"
            else -> readyStatus.reason ?: readyStatus.status
        }
        val availableCpu = toMilliCore(status.capacity?.get("cpu"))
        val availableMemory = toMiByte(status.capacity?.get("memory"))
        val emptyValue = if (stats.isNullOrEmpty()) "n/a" else "0"

        val statsSummaryForNode = stats?.get(item.metadata.name)
        val nodeStats = statsSummaryForNode?.node
        val cpu = toMilliCore(nodeStats?.cpu?.usageNanoCores)
        val cpuPercentage = cpuPercentage(cpu, availableCpu, emptyValue)
        val memory = toMiByte(nodeStats?.memory?.workingSetBytes)
        val memoryPercentage = memoryPercentage(memory, availableMemory, emptyValue)
        val countPods = statsSummaryForNode?.pods?.size

        return listOf(ItemValue(
            "Status", nodeStatus, nodeStatus),
            ItemValue("%CPU", null, "CPU ${cpuPercentage}%", showOnDesktop = false),
            ItemValue("%Mem", null, "Mem ${memoryPercentage}%", showOnDesktop = false)
        ) to
            listOf( // TODO: where to get roles from, like for master: "control-plane,etcd,master"? -> they seem to be set as annotations (or labels)
                ItemValue("CPU", toDisplayValue(cpu) ?: emptyValue, showOnMobile = false),
                ItemValue("%CPU", cpuPercentage, showOnMobile = false),
                ItemValue("CPU/A", toDisplayValue(availableCpu), showOnMobile = false),
                ItemValue("CPU", null, "CPU ${toDisplayValue(cpu) ?: emptyValue}, ${cpuPercentage}% of ${toDisplayValue(availableCpu)}", showOnDesktop = false),
                ItemValue("Mem", toDisplayValue(memory) ?: emptyValue, showOnMobile = false),
                ItemValue("%Mem", memoryPercentage, showOnMobile = false),
                ItemValue("Mem/A", toDisplayValue(availableMemory), showOnMobile = false),
                ItemValue("Mem", null, "Mem ${toDisplayValue(memory) ?: emptyValue}, ${memoryPercentage}% of ${toDisplayValue(availableMemory)}", showOnDesktop = false),
                ItemValue("Pods",  countPods?.toString() ?: emptyValue, countPods?.let { "$it pods" } ?: "# Pods n/a"),
                ItemValue("Images", status.images.size.toString(), "${status.images.size} images"),
                ItemValue("Taints", item.spec.taints.size.toString(), "${item.spec.taints.size} taints"),
                ItemValue("Version", status.nodeInfo?.kubeletVersion, "K8s: ${status.nodeInfo?.kubeletVersion}"),
                ItemValue("Kernel", status.nodeInfo?.kernelVersion)
            )
    }

    private fun mapAccessModes(accessModes: List<String>) = accessModes.joinToString {
        it.replace("ReadWriteOnce", "RWO").replace("ReadOnlyMany", "ROM")
            .replace("ReadWriteMany", "RWM").replace("ReadWriteOncePod", "RWOP")
    }

    private fun cpuPercentage(cpu: BigDecimal?, availableCpu: BigDecimal?, emptyValue: String): String =
        if (cpu != null && availableCpu != null && availableCpu != BigDecimal.ZERO) {
            toDisplayValue(cpu.multiply(BigDecimal.valueOf(100)).divide(availableCpu, 0, RoundingMode.DOWN))
        } else {
            emptyValue
        }

    private fun memoryPercentage(memory: BigDecimal?, availableMemory: BigDecimal?, emptyValue: String) =
        if (memory != null && availableMemory != null && availableMemory != BigDecimal.ZERO) {
            toDisplayValue(memory.multiply(BigDecimal.valueOf(100)).divide(availableMemory, 0, RoundingMode.DOWN))
        } else {
            emptyValue
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

    @JvmName("toDisplayValueNullable")
    private fun toDisplayValue(metricValue: BigDecimal?, roundingMode: RoundingMode = RoundingMode.UP): String? = metricValue?.let {
        toDisplayValue(metricValue, roundingMode)
    }

    private fun toDisplayValue(metricValue: BigDecimal, roundingMode: RoundingMode = RoundingMode.UP): String =
        metricValue.setScale(0, roundingMode).toString()

}