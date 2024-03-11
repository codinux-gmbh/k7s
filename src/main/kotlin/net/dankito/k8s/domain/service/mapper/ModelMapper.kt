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
        val creationTimestamp = Instant.parse(item.metadata.creationTimestamp)
        val itemSpecificValues = getItemSpecificValues(item, stats)

        return if (item is Pod) {
            val status = item.status
            PodResourceItem(name, namespace, creationTimestamp, mapPodStatus(status), status.podIP, itemSpecificValues, status.containerStatuses.map {
                ContainerStatus(it.name, try { it.containerID } catch (ignored: Exception) { null }, it.image, it.imageID, it.restartCount, it.started, it.ready, it.state.waiting != null, it.state.running != null, it.state.terminated != null)
            })
        } else {
            ResourceItem(name, namespace, creationTimestamp, itemSpecificValues)
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

            buildMap { // TODO: where to get roles from, like for master: "control-plane,etcd,master"? -> they seem to be set as annotations (or labels)
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
                        this["%Mem"] = toDisplayValue((memory?.let { BigDecimal(memory.toString()) } ?: BigDecimal.ZERO).multiply(
                            BigDecimal.valueOf(100)).divide(availableMemory, 0, RoundingMode.DOWN))

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

}