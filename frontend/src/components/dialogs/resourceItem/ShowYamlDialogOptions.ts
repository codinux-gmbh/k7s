import {ResourceItem} from "../../../ts/model/ResourceItem"
import {KubernetesResource} from "../../../ts/model/KubernetesResource"

export class ShowYamlDialogOptions {

  constructor(readonly yaml: string, readonly item: ResourceItem,
              readonly resource: KubernetesResource) { }

}