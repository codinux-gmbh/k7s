import type {WebClient} from "../web/WebClient"
import {KubernetesResource} from "../../model/KubernetesResource"
import {ResourceItems} from "../../model/ResourceItems"
import {ResourcesState} from "../../ui/state/ResourcesState.svelte"

export class K7sApiClient {

  constructor(private readonly webClient: WebClient) { }


  async getAllAvailableResourceTypes(context?: string): Promise<KubernetesResource[]> {
    return this.webClient.get("/resources" + this.createQueryParams(context))
      .then(jsObject => (jsObject as KubernetesResource[])
        .map(res => KubernetesResource.fromJsObject(res)))
  }

  async getResourceItems(state: ResourcesState): Promise<ResourceItems> {
    const url = "/resources" + this.createPathParams(state.selectedResource.group, state.selectedResource.kind)
      + this.createQueryParams(state.context, state.namespace)

    return this.webClient.get(url)
  }


  private createPathParams(group: string | undefined, kind: string) {
    return `/${group ?? "null"}/${kind}`
  }

  private createQueryParams(context?: string, namespace?: string) {
    let query = ""

    if (context) {
      query = `?context=${context}`
    }

    if (namespace) {
      query += query.length ? "&" : "?"
      query += `namespace=${namespace}`
    }

    return query
  }

}