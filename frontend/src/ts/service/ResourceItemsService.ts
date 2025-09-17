import {K7sApiClient} from "../clients/k7sApi/k7sApiClient"
import {LogService} from "./LogService"
import {ResourcesState} from "../ui/state/ResourcesState.svelte"
import {KubernetesResource} from "../model/KubernetesResource"

export class ResourceItemsService {

  constructor(private readonly resourcesState: ResourcesState,
              private readonly client: K7sApiClient, private readonly log: LogService) { }


  selectedResourceChanged(resource: KubernetesResource) {
    this.resourcesState.selectedResource = resource

    this.client.getResourceItems(this.resourcesState)
      .then(items => {
        this.resourcesState.selectedResourceItems = items
      })
  }

}