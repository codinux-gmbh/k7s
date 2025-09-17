import {KubernetesResource} from "../../model/KubernetesResource"

export class ResourcesState {

  static state = new ResourcesState()


  resourceTypes: KubernetesResource[] = $state([])

  context: string | undefined = $state(undefined)

}