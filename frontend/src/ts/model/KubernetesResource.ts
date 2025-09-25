import {Verb} from "./Verb"
import {ResourceConstants} from "../service/ResourceConstants"

export class KubernetesResource {

  constructor(readonly group: string | undefined, readonly storageVersion: string,
              readonly name: string, readonly kind: string, readonly isNamespaced: boolean,
              readonly isCustomResourceDefinition: boolean,
              readonly displayName: string, readonly identifier: string,
              readonly singularName: string | undefined, readonly shortNames: string[] = [],
              readonly verbs: Verb[] = [], readonly servedVersions: string[] = []
  ) { }


  get version(): string { // to be better readable
    return this.storageVersion
  }

  get isPod(): boolean {
    return this.group === undefined && this.kind === "Pod"
  }

  get isDeployment(): boolean {
    return this.group === ResourceConstants.AppsGroup && this.kind === "Deployment"
  }

  get isStatefulSet(): boolean {
    return this.group === ResourceConstants.AppsGroup && this.kind === "StatefulSet"
  }

  get isLoggable(): boolean {
    return KubernetesResource.LoggableResourceKinds.includes(this.kind)
  }

  get isScalable(): boolean {
    return this.isDeployment || this.isStatefulSet
  }

  get isDeletable(): boolean {
    return this.containsVerb(Verb.delete)
  }

  get isWatchable(): boolean {
    return this.containsVerb(Verb.watch)
  }

  get allowDeletingWithoutConfirmation(): boolean {
    return this.isPod
  }

  containsVerb(verb: Verb): boolean {
    return this.verbs.includes(verb)
  }


  // Kubernetes API says DaemonSet is loggable, but Fabric8 Kubernetes Client hasn't implemented it as Loggable
  static LoggableResourceKinds = [
    "Pod", "Deployment", "StatefulSet", "ReplicaSet", "Job"
  ]

  static ResourcesWithStats = [
    "Pod", "Node", "PersistentVolumeClaim"
  ]

  // obj has only the shape of KubernetesResource, but not e.g. its methods, getters and setters.
  static fromJsObject(obj: KubernetesResource): KubernetesResource {
    return new KubernetesResource(obj.group, obj.storageVersion, obj.name, obj.kind, obj.isNamespaced,
      obj.isCustomResourceDefinition, obj.displayName, obj.identifier, obj.singularName, obj.shortNames,
      // @ts-ignore
      (obj.verbs as string[]).map((v: string) => Verb[v as keyof typeof Verb]),
      obj.servedVersions)
  }

}