import {Verb} from "./Verb"

export class KubernetesResource {

  constructor(readonly group: string | undefined, readonly storageVersion: string,
              readonly name: string, readonly kind: string, readonly isNamespaced: boolean,
              readonly isCustomResourceDefinition: boolean,
              readonly singularName: string | undefined, readonly shortNames: string[] = [],
              readonly verbs: Verb[] = [], readonly servedVersions: string[]
  ) { }

}