package net.dankito.k8s.api.templates

import io.quarkus.qute.TemplateExtension
import jakarta.ws.rs.core.UriBuilder
import net.dankito.k8s.api.dto.ResourceItemsViewData
import net.dankito.k8s.domain.model.KubernetesResource

@TemplateExtension
object ResourceItemTemplateExtensions {

    @JvmStatic
    fun getAdditionalValuesNames(resource: KubernetesResource): List<String> =
        when (resource.kind) {
            "Pod" -> listOf("IP", "HostIP", "CPU", "Mem")
            "Service" -> listOf("Type", "ClusterIP", "ExternalIPs", "Ports")
            "Ingress" -> listOf("Class", "Hosts", "Address", "Ports")
            "Deployment" -> listOf("Ready", "Up-to-date", "Available")
            "ConfigMap" -> listOf("Data")
            "Secret" -> listOf("Type", "Data")
            "Node" -> listOf("Status", "Taints", "Version", "Kernel", "CPU", "%CPU", "CPU/A", "Mem", "%Mem", "Mem/A", "Images")
            else -> emptyList()
        }

    @JvmStatic
    fun createWatchResourcePath(data: ResourceItemsViewData): String {
        val resource = data.resource
        val uriBuilder = UriBuilder.fromPath("watch/resources/${resource.group ?: "null"}/${resource.name}")

        if (data.selectedContext != null) {
            uriBuilder.queryParam("context", data.selectedContext)
        }
        if (data.selectedNamespace != null) {
            uriBuilder.queryParam("namespace", data.selectedNamespace)
        }

        return uriBuilder.toTemplate()
    }

}