import {K7sApiClient} from "../clients/k7sApi/k7sApiClient"
import {ResourcesState} from "../ui/state/ResourcesState.svelte"
import {KubernetesResource} from "../model/KubernetesResource"
import {ResourceItems} from "../model/ResourceItems"
import {TimeBasedCache} from "./cache/TimeBasedCache"
import {ResourceItem} from "../model/ResourceItem"
import {UiState} from "../ui/state/UiState.svelte"
import {ShowYamlDialogOptions} from "../../components/dialogs/resourceItem/ShowYamlDialogOptions"

export class ResourceItemsService {

  private itemsCache = new TimeBasedCache<KubernetesResource, ResourceItems>(5 * 60 * 1000) // remove cached resource items after 5 min

  constructor(private readonly resourcesState: ResourcesState,
              private readonly client: K7sApiClient) { }


  selectedContextChanged(context?: string) {
    this.client.getAllAvailableResourceTypes(context)
      .then(response => {
        const resources = response.resources
        const state = this.resourcesState

        state.contexts = response.contexts
        state.defaultContext = response.defaultContext
        state.namespaces = response.namespaces
        state.resourceTypes = resources
        state.context = context

        // we switched context, now load the default resource (pods)
        const pods = response.resources.find(res => res.isPod)
        if (pods) {
          this.selectedResourceChanged(pods)
        }

        state.standardResources = resources
          .filter(res => res.isCustomResourceDefinition == false)
          .sort((a, b) => a.name.localeCompare(b.name))

        state.customResourceDefinitions = resources
          .filter(res => res.isCustomResourceDefinition)
          .sort((a, b) => a.identifier.localeCompare(b.identifier))
    })
  }

  selectedResourceChanged(resource: KubernetesResource) {
    const previousItems = this.itemsCache.get(resource)
    if (previousItems) { // so there's no delay in showing selected resource's items, show previously fetched items if available
      this.resourcesState.selectedResource = resource
      this.resourcesState.selectedResourceItems = previousItems
    }

    this.client.getResourceItems(this.resourcesState.toResourceParameter(resource))
      .then(items => {
        this.resourcesState.selectedResource = resource
        this.resourcesState.selectedResourceItems = items
        this.itemsCache.put(resource, items)
      })
  }

  selectedNamespaceChanged(namespace: string | undefined) {
    this.client.getResourceItems(this.resourcesState.toResourceParameterForNamespace(namespace))
      .then(items => {
        this.resourcesState.namespace = namespace
        this.resourcesState.selectedResourceItems = items
        this.itemsCache.put(this.resourcesState.selectedResource, items)
      })
  }


  showLogs(item: ResourceItem, resource: KubernetesResource) {
    this.client.getItemLogs(item, resource, this.resourcesState.context)
      .then(logs => {
        UiState.state.showLogsDialog = new ItemLogsDialogOptions(logs, item, resource)
      })
  }

  showYaml(item: ResourceItem, resource: KubernetesResource) {
    this.client.getItemYaml(item, resource, this.resourcesState.context)
      .then(yaml => {
        UiState.state.showYamlDialog = new ShowYamlDialogOptions(yaml, item, resource)
      })
  }

  scaleItem(item: ResourceItem, resource: KubernetesResource) {
    const countReplicaString = window.prompt(`Scale ${resource.kind} ${item.namespace}/${item.name}? Replicas:`)
    const countReplica = Number.parseInt(countReplicaString ?? "")

    if (Number.isNaN(countReplica) === false) {
      this.client.scaleItem(item, resource, this.resourcesState.context, countReplica)
        .then(success => {
          console.log("Successfully scaled item?", success)
        })
    }
  }

  deleteItem(item: ResourceItem, resource: KubernetesResource) {
    this.client.deleteItem(item, resource, this.resourcesState.context)
      .then(success => {
        console.log("Successfully deleted item?", success)
      })
  }

  killItem(item: ResourceItem, resource: KubernetesResource) {
    this.client.deleteItem(item, resource, this.resourcesState.context, 0)
      .then(success => {
        console.log("Successfully killed item?", success)
      })
  }

}