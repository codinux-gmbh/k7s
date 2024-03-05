package net.dankito.k8s.api.templates

import io.quarkus.qute.TemplateExtension
import net.dankito.k8s.domain.model.KubernetesResource

@TemplateExtension
object ResourceItemTemplateExtensions {

    @JvmStatic
    fun getAdditionalValuesNames(resource: KubernetesResource): List<String> =
        when (resource.kind) {
            "Pod" -> listOf("IP", "HostIP")
            "Service" -> listOf("Type", "ClusterIP", "ExternalIPs", "Ports")
            "Ingress" -> listOf("Class", "Hosts", "Address", "Ports")
            "Deployment" -> listOf("Ready", "Up-to-date", "Available")
            "ConfigMap" -> listOf("Data")
            "Secret" -> listOf("Type", "Data")
            "Node" -> listOf("Status", "Taints", "Version", "Kernel", "CPU/A" , "Mem/A", "Images")
            else -> emptyList()
        }

}