import type {Command} from "./Command"
import {KubernetesResource} from "../../model/KubernetesResource"

export class DisplayResourceItemsCommand implements Command {

  constructor(readonly command: string, readonly resource: KubernetesResource) { }

}