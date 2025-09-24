import type {WebClient} from "../clients/web/WebClient"
import {FetchWebClient} from "../clients/web/FetchWebClient"
import {LogService} from "./LogService"
import {K7sApiClient} from "../clients/k7sApi/k7sApiClient"
import {ResourceItemsService} from "./ResourceItemsService"
import {ResourcesState} from "../ui/state/ResourcesState.svelte"
import {ResourceItemsFormatter} from "../ui/formatter/ResourceItemsFormatter"

export class DI {

  static readonly log: LogService = new LogService()

  static readonly webClient: WebClient = new FetchWebClient(DI.getBaseUrl(), DI.log, `k7s client; ${navigator.userAgent}`)

  static readonly apiClient: K7sApiClient = new K7sApiClient(DI.webClient)

  static readonly resourceItemsService: ResourceItemsService = new ResourceItemsService(ResourcesState.state, DI.apiClient, DI.log)

  static readonly itemsFormatter: ResourceItemsFormatter = new ResourceItemsFormatter()


  private static getBaseUrl(): string {
    let baseUrl = DI.determineBackendHost()
    const port = import.meta.env.VITE_BACKEND_PORT

    if (port) {
      baseUrl += ":" + port
    }

    baseUrl += "/k7s/api/v1"

    return baseUrl
  }

  private static determineBackendHost(): string {
    const configuredHost = import.meta.env.VITE_BACKEND_HOST
    if (configuredHost) {
      return configuredHost
    }

    const fileUrl = new URL(import.meta.url)

    return `${fileUrl.protocol}//${fileUrl.hostname}`
  }

}