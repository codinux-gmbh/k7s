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

  let expandStandardResources: boolean = $state(false)
  let expandCustomResourceDefinitions: boolean = $state(false)

  $effect(() => {
    highlightedResources = resourcesState.resourceTypes
      .filter(res => highlightedResourceKinds.includes(res.kind))
      .sort((a, b) => highlightedResourceKinds.indexOf(a.kind) - highlightedResourceKinds.indexOf(b.kind))

    standardResources = resourcesState.resourceTypes
      .filter(res => res.isCustomResourceDefinition == false)
      .sort((a, b) => a.name.localeCompare(b.name))

    customResourceDefinitions = resourcesState.resourceTypes
      .filter(res => res.isCustomResourceDefinition)
      .sort((a, b) => a.identifier.localeCompare(b.identifier))
  })
</script>


<aside class="w-[275px] h-full mr-2 p-2 bg-white text-zinc-500 shadow-md overflow-x-hidden overflow-y-auto">
  <nav>
    <ul>
      {#each highlightedResources as resource}
        <ResourceListItem {resource} />
      {/each}


      <ResourceListSectionHeader title="Standard resources" bind:isExpanded={expandStandardResources} />
      {#if expandStandardResources}
        {#each standardResources as resource}
          <ResourceListItem {resource} />
        {/each}
      {/if}


      <ResourceListSectionHeader title="CRDs" bind:isExpanded={expandCustomResourceDefinitions} />
      {#if expandCustomResourceDefinitions}
        {#each customResourceDefinitions as crd}
          <ResourceListItem resource={crd} title={`${crd.displayName} ${crd.group}.${crd.version}`} />
        {/each}
      {/if}
    </ul>
  </nav>
</aside>