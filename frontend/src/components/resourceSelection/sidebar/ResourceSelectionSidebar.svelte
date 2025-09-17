<script lang="ts">
  import {ResourcesState} from "../../../ts/ui/state/ResourcesState.svelte"
  import {KubernetesResource} from "../../../ts/model/KubernetesResource"
  import ResourceListItem from "./ResourceListItem.svelte"
  import ResourceListSectionHeader from "./ResourceListSectionHeader.svelte"

  let { resourcesState }: { resourcesState: ResourcesState } = $props()

  const highlightedResourceKinds = [ "Pod", "Service", "Ingress", "Deployment", "ConfigMap", "Secret", "Node", "PersistentVolume", "PersistentVolumeClaim" ]

  let highlightedResources: KubernetesResource[] = $state([])
  let standardResources: KubernetesResource[] = $state([])
  let customResourceDefinitions: KubernetesResource[] = $state([])

  $effect(() => {
    highlightedResources = resourcesState.resourceTypes
      .filter(res => highlightedResourceKinds.includes(res.kind))
      .sort(res => highlightedResourceKinds.indexOf(res.kind))

    standardResources = resourcesState.resourceTypes
      .filter(res => res.isCustomResourceDefinition == false)
      .sort(res => res.name)

    customResourceDefinitions = resourcesState.resourceTypes
      .filter(res => res.isCustomResourceDefinition)
      .sort(res => res.identifier)
  })
</script>


<aside class="w-[275px] h-full mr-2 p-2 bg-white text-zinc-500 shadow-md overflow-x-hidden overflow-y-auto">
  <nav>
    <ul>
      {#each highlightedResources as resource}
        <ResourceListItem {resource} />
      {/each}


      <ResourceListSectionHeader title="Standard resources" />
      {#each standardResources as resource}
        <ResourceListItem {resource} />
      {/each}


      <ResourceListSectionHeader title="CRDs" />
      {#each customResourceDefinitions as crd}
        <ResourceListItem resource={crd} title={`${crd.displayName} ${crd.group}.${crd.version}`} />
      {/each}
    </ul>
  </nav>
</aside>