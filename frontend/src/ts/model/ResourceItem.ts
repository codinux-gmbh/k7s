import {ItemValue} from "./ItemValue"
import {ContainerStatus} from "./ContainerStatus"

export class ResourceItem {

  constructor(
    readonly name: string, readonly namespace?: string,
    readonly creationTimestamp?: string,
    readonly highlightedItemSpecificValues: ItemValue[] = [],
    readonly secondaryItemSpecificValues: ItemValue[] = [],
    readonly htmlSafeId: string = "", // TODO: required?

    // Pod only fields
    readonly status?: string,
    readonly podIP?: string,
    readonly container: ContainerStatus[] = [],
  ) { }

}