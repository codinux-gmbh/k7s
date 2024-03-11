package net.dankito.k8s.api.templates

import io.quarkus.qute.TemplateExtension
import jakarta.ws.rs.core.UriBuilder
import net.dankito.k8s.api.dto.ResourceItemsViewData
import net.dankito.k8s.domain.model.KubernetesResource
import net.dankito.k8s.domain.model.PodResourceItem
import net.dankito.k8s.domain.model.ResourceItem

@TemplateExtension
object ResourceItemTemplateExtensions {

    const val PendingStateStyle = "text-orange-500 "

    const val SucceededStateStyle = "text-gray-400 "

    const val ErrorStateStyle = "text-red-700 "

    @JvmStatic
    fun getItemStyle(item: ResourceItem): String {
        if (item is PodResourceItem) {
            return when (item.status) {
                "Creating", "PodInitializing", "ContainerCreating", "Pending" -> PendingStateStyle
                "Succeeded", "Completed" -> SucceededStateStyle
                "Failed", "Error", "CrashLoopBackOff", "ImagePullBackOff", "CreateContainerConfigError", "InvalidImageName",
                "ErrImageNeverPull", "CreateContainerError", "OOMKilled", "ContainerCannotRun", "DeadlineExceeded" -> ErrorStateStyle
                "Running" -> {
                    if (item.countReadyContainers != item.container.size) {
                        ErrorStateStyle
                    } else {
                        ""
                    }
                }
                else -> ""
            }
        }

        return ""
    }

    @JvmStatic
    fun getItemSpecificValuesNames(resource: KubernetesResource): List<String> =
        when (resource.kind) {
            "Pod" -> listOf("Ready", "Status", "CPU", "Mem", "IP", "Host")
            "Service" -> listOf("Type", "ClusterIP", "ExternalIPs", "Ports")
            "Ingress" -> listOf("Class", "Hosts", "Ports", "Address")
            "Deployment" -> listOf("Ready", "Up-to-date", "Available")
            "ConfigMap" -> listOf("Data")
            "Secret" -> listOf("Type", "Data")
            "PersistentVolume" -> listOf("Status", "Access Modes", "StorageClass", "Claim", "Capacity", "Reclaim Policy", "Reason")
            "PersistentVolumeClaim" -> listOf("Status", "Access Modes", "StorageClass", "Volume", "Used Mi", "Used %", "Capacity")
            "Node" -> listOf("Status", "CPU", "%CPU", "CPU/A", "Mem", "%Mem", "Mem/A", "Pods", "Images", "Taints", "Version", "Kernel")
            else -> emptyList()
        }

    @JvmStatic
    fun createWatchResourcePath(data: ResourceItemsViewData): String {
        val resource = data.resource
        val uriBuilder = UriBuilder.fromPath("/k7s/watch/resources/${resource.group ?: "null"}/${resource.name}")

        if (data.selectedContext != null) {
            uriBuilder.queryParam("context", data.selectedContext)
        }
        if (data.selectedNamespace != null) {
            uriBuilder.queryParam("namespace", data.selectedNamespace)
        }
        if (data.resourceVersion != null) {
            uriBuilder.queryParam("resourceVersion", data.resourceVersion)
        }

        return uriBuilder.toTemplate()
    }

}