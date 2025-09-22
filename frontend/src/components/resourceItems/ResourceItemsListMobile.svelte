<script lang="ts">
  import {ResourcesState} from "../../ts/ui/state/ResourcesState.svelte"
  import ResourceItemsListMobileListItem from "./ResourceItemsListMobileListItem.svelte"
  import {KubernetesResource} from "../../ts/model/KubernetesResource"

  let { resource, resourcesState, showNamespace }: { resource: KubernetesResource, resourcesState: ResourcesState, showNamespace: boolean } = $props()
</script>


<div class="w-full h-full flex flex-col">
  <div class="shrink-0 w-full h-8 mb-2 p-2 flex flex-row text-xs bg-white shadow-sm">
    <div class="flex-1 text-primary truncate">{resource.displayName} ({#if resource.isNamespaced && resourcesState.namespace}{resourcesState.namespace}{:else}all{/if}) [{resourcesState.selectedResourceItems.items.length}]</div>
    {#if resourcesState.contexts.length > 1}<div class="ml-2">Context: {resourcesState.context ?? resourcesState.defaultContext}</div>{/if}
  </div>

  <div role="listbox" class="grow w-full h-full bg-white shadow-md overflow-y-auto">
    {#each resourcesState.selectedResourceItems.items as item, index}
      <ResourceItemsListMobileListItem {item} resource={resourcesState.selectedResource} {showNamespace} {index} />
    {/each}
  </div>
</div>