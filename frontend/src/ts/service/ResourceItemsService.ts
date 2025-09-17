import {K7sApiClient} from "../clients/k7sApi/k7sApiClient"
import {ResourcesState} from "../ui/state/ResourcesState.svelte"
import {KubernetesResource} from "../model/KubernetesResource"

export class ResourceItemsService {

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
    this.client.getResourceItems(this.resourcesState.toResourceParameter(resource))
      .then(items => {
        this.resourcesState.selectedResource = resource
        this.resourcesState.selectedResourceItems = items
      })
  }

}