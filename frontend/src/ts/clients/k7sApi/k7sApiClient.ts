import type {WebClient} from "../web/WebClient"
import {KubernetesResource} from "../../model/KubernetesResource"
import {ResourceItems} from "../../model/ResourceItems"
import {ResourceParameter} from "./ResourceParameter"

export class K7sApiClient {

  constructor(private readonly webClient: WebClient) { }


  async getAllAvailableResourceTypes(context?: string): Promise<KubernetesResource[]> {
    return this.webClient.get("/resources" + this.createQueryParams(context))
      .then(jsObject => (jsObject as KubernetesResource[])
        .map(res => KubernetesResource.fromJsObject(res)))
  }

  async getResourceItems(params: ResourceParameter): Promise<ResourceItems> {
    const url = "/resources" + this.createPathParams(params.group, params.kind)
      + this.createQueryParams(params.context, params.namespace)

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