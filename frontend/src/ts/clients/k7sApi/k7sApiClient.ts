import type {WebClient} from "../web/WebClient"
import {KubernetesResource} from "../../model/KubernetesResource"
import {ResourceItems} from "../../model/ResourceItems"
import {ResourceParameter} from "./ResourceParameter"
import {KubeContextResources} from "../../model/KubeContextResources"
import {ResourceItem} from "../../model/ResourceItem"
import {WebRequest} from "../web/WebRequest"

export class K7sApiClient {

  constructor(private readonly webClient: WebClient) { }


  async getAllAvailableResourceTypes(context?: string): Promise<KubeContextResources> {
    return this.webClient.get("/resources" + this.createQueryParams(context))
      .then(jsObject => (jsObject as KubeContextResources))
      .then(obj => new KubeContextResources(
          obj.resources.map(res => KubernetesResource.fromJsObject(res)),
          obj.namespaces,
          obj.contexts, obj.defaultContext
        )
      )
  }

  async getResourceItems(params: ResourceParameter): Promise<ResourceItems> {
    const url = "/resources" + this.createPathParams(params.group, params.kind)
      + this.createQueryParams(params.context, params.namespace)

    return this.webClient.get(url)
  }


  async getItemLogs(item: ResourceItem, resource: KubernetesResource, context?: string): Promise<string[]> {
    const url = this.createResourceItemUrl(item, resource, context, "/logs")

    return this.webClient.get(url)
  }

  async getItemYaml(item: ResourceItem, resource: KubernetesResource, context?: string): Promise<string> {
    const url = this.createResourceItemUrl(item, resource, context, "/yaml")

    return this.webClient.get(new WebRequest(url, null, null, "application/yaml"))
  }

  async scaleItem(item: ResourceItem, resource: KubernetesResource, context: string | undefined, scaleTo: number): Promise<boolean> {
    let url = this.createResourceItemUrl(item, resource, context)
    if (url.includes("?")) {
      url += `&scaleTo=${scaleTo}`
    } else {
      url += `?scaleTo=${scaleTo}`
    }

    console.log("Calling URL", url)

    return this.webClient.patch(url)
  }

  async deleteItem(item: ResourceItem, resource: KubernetesResource, context?: string, gracePeriodSeconds?: number): Promise<boolean> {
    let url = this.createResourceItemUrl(item, resource, context)
    if (gracePeriodSeconds) {
      if (url.includes("?")) {
        url += `&gracePeriod=${gracePeriodSeconds}`
      } else {
        url += `?gracePeriod=${gracePeriodSeconds}`
      }
    }

    return this.webClient.delete(url)
  }


  private createResourceItemUrl(item: ResourceItem, resource: KubernetesResource, context?: string, pathSuffix?: string) {
    let path = `/resources/${this.createPathParams(resource.group, resource.kind)}/${item.namespace ?? "null"}/${item.name}`

    if (pathSuffix) {
      path += pathSuffix
    }

    return path + this.createQueryParams(context)
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