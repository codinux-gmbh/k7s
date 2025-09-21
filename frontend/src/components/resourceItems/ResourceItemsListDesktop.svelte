<script lang="ts">
  import {ResourcesState} from "../../ts/ui/state/ResourcesState.svelte"
  import {DI} from "../../ts/service/DI"
  import {KubernetesResource} from "../../ts/model/KubernetesResource"
  import ResourceItemsListDesktopListItem from "./ResourceItemsListDesktopListItem.svelte"

  let { resourcesState, showNamespace }: { resourcesState: ResourcesState, showNamespace: boolean } = $props()

  let resource: KubernetesResource = $state(resourcesState.selectedResource)

  const itemsFormatter = DI.itemsFormatter

  $effect(() => {
    resource = resourcesState.selectedResource
  })


  function getColumnNames(): string[] {
    return itemsFormatter.getResourceSpecificColumnNames(resource)
  }

  function getGridColumns(): string {
    return itemsFormatter.getGridColumns(resource, showNamespace)
  }
</script>


<div class="w-full h-full grid grid-rows-[auto_1fr]">
  <div class="thead sticky top-0 z-2">
    <div class="grid items-center h-12 pl-2 font-bold bg-zinc-200 text-zinc-500 border-b border-zinc-500" style={getGridColumns()}>
      {#if showNamespace}
        <div>Namespace</div>
      {/if}
      <div class="min-w-[17.25rem]">Name</div>

      {#each getColumnNames() as headerName}
        <div class="ml-1">{ headerName }</div>
      {/each}

      <div></div>
    </div>
  </div>

  <div class="tbody overflow-y-auto">
    {#each resourcesState.selectedResourceItems.items as item}
      <ResourceItemsListDesktopListItem {item} {resource} {showNamespace} />
    {/each}
  </div>
</div>