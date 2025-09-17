import {ResourceItem} from "./ResourceItem"

export class ResourceItems {

  constructor(readonly resourceVersion?: string, readonly items: ResourceItem[] = []) { }

}