import type {WebClient} from "../web/WebClient"
import type {KubernetesResource} from "../../model/KubernetesResource"

export class K7sApiClient {

  constructor(private readonly webClient: WebClient) { }


  async getAllAvailableResourceTypes(context?: string): Promise<KubernetesResource[]> {
    return this.webClient.get("/resources" + (context ? `?context=${context}` : ""))
  }

}