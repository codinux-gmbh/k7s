import type {WebClient} from "../clients/web/WebClient"
import {FetchWebClient} from "../clients/web/FetchWebClient"
import {LogService} from "./LogService"

export class DI {

  static readonly log: LogService = new LogService()

  static readonly webClient: WebClient = new FetchWebClient(DI.getBaseUrl(), DI.log, `Access Log Visualizer client; ${navigator.userAgent}`)


  private static getBaseUrl(): string {
    let baseUrl = DI.determineBackendHost()
    const port = import.meta.env.VITE_BACKEND_PORT

    if (port) {
      baseUrl += ":" + port
    }

    baseUrl += "/access-log/api/v1"

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