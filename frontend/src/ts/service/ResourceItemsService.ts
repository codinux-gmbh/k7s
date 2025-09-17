import {K7sApiClient} from "../clients/k7sApi/k7sApiClient"
import {ResourcesState} from "../ui/state/ResourcesState.svelte"
import {KubernetesResource} from "../model/KubernetesResource"
import {Cache} from "./cache/Cache"
import {ResourceItems} from "../model/ResourceItems"

export class ResourceItemsService {

  private itemsCache = new Cache<ResourceItems>()

  constructor(private readonly resourcesState: ResourcesState,
              private readonly client: K7sApiClient) { }


  selectedContextChanged(context?: string) {
    this.client.getAllAvailableResourceTypes(context)
      .then(response => {
        this.resourcesState.context = context
        this.resourcesState.resourceTypes = response

        // we switched context, now load the default resource (pods)
        const pods = response.find(res => res.isPod)
        if (pods) {
          this.selectedResourceChanged(pods)
        }
    })
  }

  selectedResourceChanged(resource: KubernetesResource) {
    const previousItems = this.itemsCache.get(resource.identifier)
    if (previousItems) { // so there's no delay in showing selected resource's items, show previously fetched items if available
      this.resourcesState.selectedResource = resource
      this.resourcesState.selectedResourceItems = previousItems
    }

    this.client.getResourceItems(this.resourcesState.toResourceParameter(resource))
      .then(items => {
        this.resourcesState.selectedResource = resource
        this.resourcesState.selectedResourceItems = items
        this.itemsCache.put(resource.identifier, items)
      })
  }

}