import {ItemValue} from "./ItemValue"

export class ResourceItem {

  constructor(
    readonly name: string, readonly namespace?: string,
    readonly creationTimestamp?: string,
    readonly highlightedItemSpecificValues: ItemValue[] = [],
    readonly secondaryItemSpecificValues: ItemValue[] = [],
    readonly htmlSafeId: string = "", // TODO: required?
  ) { }

}