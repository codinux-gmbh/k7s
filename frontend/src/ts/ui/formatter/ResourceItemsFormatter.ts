import {KubernetesResource} from "../../model/KubernetesResource"
import {ResourceItem} from "../../model/ResourceItem"

export class ResourceItemsFormatter {

  static PendingStateStyle = "text-orange-500 "

  static CompletedStateStyle = "text-gray-400 "

  static ErrorStateStyle = "text-red-700 "


  constructor() { }


  getGridColumns(resource: KubernetesResource, showNamespace: boolean): string {
    const gridColumnsBase = `grid-template-columns: ${showNamespace ? "9rem " : ""}1fr `

    return gridColumnsBase + this.getResourceSpecificColumnWidths(resource) + " 2.75rem;"
  }

  private getResourceSpecificColumnWidths(resource: KubernetesResource): string {
    return ResourceItemsFormatter.ResourceSpecificColumnsWidths[resource.kind] ?? ""
  }


  getResourceSpecificColumnNames(resource: KubernetesResource): string[] {
    return ResourceItemsFormatter.ResourceSpecificColumns[resource.kind] ?? []
  }


  getItemStyle(item: ResourceItem): string | undefined {
    switch (item.status) {
      case "Creating":
      case "PodInitializing":
      case "ContainerCreating":
      case "Pending":
        return ResourceItemsFormatter.PendingStateStyle;

      case "Succeeded":
      case "Completed":
        return ResourceItemsFormatter.CompletedStateStyle;

      case "Failed":
      case "Error":
      case "CrashLoopBackOff":
      case "ImagePullBackOff":
      case "CreateContainerConfigError":
      case "InvalidImageName":
      case "ErrImageNeverPull":
      case "CreateContainerError":
      case "OOMKilled":
      case "ContainerCannotRun":
      case "DeadlineExceeded":
        return ResourceItemsFormatter.ErrorStateStyle;

      case "Running":
        const countReadyContainers = item.container.filter(container => container.ready).length
        if (countReadyContainers != item.container.length) {
          return ResourceItemsFormatter.ErrorStateStyle
        } else {
          return undefined
        }

      default:
        return undefined
    }
  }
  
  
  private static ResourceSpecificColumns: Record<string, string[]> = {
    Pod: ["Ready", "Status", "CPU", "Mem", "IP", "Host"],
    Service: ["Type", "ClusterIP", "ExternalIPs", "Ports"],
    Ingress: ["Class", "Hosts", "Ports", "Address"],
    Deployment: ["Ready", "Up-to-date", "Available"],
    ConfigMap: ["Data"],
    Secret: ["Type", "Data"],
    PersistentVolume: ["Status", "Access Modes", "Capacity", "StorageClass", "Claim", "Reclaim Policy", "Reason"],
    PersistentVolumeClaim: ["Status", "Access Modes", "StorageClass", "Volume", "Used Mi", "Used %", "Capacity"],
    Node: ["Status", "CPU", "%CPU", "CPU/A", "Mem", "%Mem", "Mem/A", "Pods", "Images", "Taints", "Version", "Kernel"]
  }

  private static ResourceSpecificColumnsWidths: Record<string, string> = {
    Pod: "53px 70px 48px 48px 120px 250px",
    Service: "70px 120px 86px 270px",
    Ingress: "70px 270px 120px 180px",
    Deployment: "60px 80px 65px",
    ConfigMap: "60px",
    Secret: "70px 60px",
    PersistentVolume: "64px 105px 64px 170px 220px 104px 85px",
    PersistentVolumeClaim: "64px 105px 170px 300px 68px 60px 64px",
    Node: "70px 60px 60px 60px 60px 60px 60px 60px 60px 60px 100px 130px",
  }

}