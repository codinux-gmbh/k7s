import {KubernetesResource} from "../../model/KubernetesResource"
import {ResourceItems} from "../../model/ResourceItems"
import {ResourceParameter} from "../../clients/k7sApi/ResourceParameter"

export class ResourcesState {

  static state = new ResourcesState()


  contexts: string[] = $state([])

  defaultContext?: string = $state(undefined)

  namespaces: string[] = $state([])

  resourceTypes: KubernetesResource[] = $state([])

  selectedResource: KubernetesResource = $state(new KubernetesResource(null, "v1", "pods", "Pod", true, false, "Pod", "pods"))

  selectedResourceItems: ResourceItems = $state(new ResourceItems(undefined, []))

  context: string | undefined = $state(undefined)

  namespace: string | undefined = $state(undefined)


  toResourceParameter(resource: KubernetesResource): ResourceParameter {
    return new ResourceParameter(resource.group, resource.kind,
      this.context, this.namespace)
  }

  toResourceParameterForNamespace(namespace: string | undefined): ResourceParameter {
    return new ResourceParameter(this.selectedResource.group, this.selectedResource.kind,
      this.context, namespace)
  }

}