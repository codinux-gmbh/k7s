import {KubernetesResource} from "./KubernetesResource"

export class KubeContextResources {

  constructor(
    readonly resources: KubernetesResource[],
    readonly namespaces: string[],
    readonly contexts: string[],
    readonly defaultContext?: string,
  ) { }

}