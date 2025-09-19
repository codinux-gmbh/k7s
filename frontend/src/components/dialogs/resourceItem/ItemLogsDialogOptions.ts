import {ResourceItem} from "../../../ts/model/ResourceItem"
import {KubernetesResource} from "../../../ts/model/KubernetesResource"

export class ItemLogsDialogOptions {

  constructor(readonly logs: string[], readonly item: ResourceItem,
              readonly resource: KubernetesResource) { }

}