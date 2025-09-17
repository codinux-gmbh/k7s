import {KubernetesResource} from "../../model/KubernetesResource"
import {ResourceItems} from "../../model/ResourceItems"

export class ResourcesState {

  static state = new ResourcesState()


  resourceTypes: KubernetesResource[] = $state([])

  selectedResource: KubernetesResource = $state(new KubernetesResource(null, "v1", "pods", "Pod", true, false, "Pod", "pods"))

  selectedResourceItems: ResourceItems = $state(new ResourceItems(undefined, []))

  context: string | undefined = $state(undefined)

  namespace: string | undefined = $state(undefined)

}