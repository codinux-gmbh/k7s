<script lang="ts">
  import {ResourcesState} from "../../ts/ui/state/ResourcesState.svelte"
  import {DI} from "../../ts/service/DI"
  import {KubernetesResource} from "../../ts/model/KubernetesResource"
  import ResourceItemsListDesktopListItem from "./ResourceItemsListDesktopListItem.svelte"
  import {ResourceItem} from "../../ts/model/ResourceItem"
  import {onMount} from "svelte"
  import {UiState} from "../../ts/ui/state/UiState.svelte"

  let { resource, resourcesState, uiState, showNamespace }:
    { resource: KubernetesResource, resourcesState: ResourcesState, uiState: UiState, showNamespace: boolean } = $props()

  let items: ResourceItem[] = $state(resourcesState.selectedResourceItems.items)

  const resourceItemsService = DI.resourceItemsService
  const itemsFormatter = DI.itemsFormatter

  onMount(() => {
    resourceItemsService.addResourceItemsChangedListener((resource, newItems) => {
      items = newItems
    })
  })


  function getColumnNames(): string[] {
    return itemsFormatter.getResourceSpecificColumnNames(resource)
  }

  function getGridColumns(): string {
    return itemsFormatter.getGridColumns(resource, showNamespace)
  }
</script>


<div class="w-full h-full flex flex-col">
  <div class="shrink-0 flex w-full h-10 mb-2 p-2 bg-white shadow-sm">
    <div class="flex-1 h-full">
      <div><span class="inline-block w-15">Context:</span>{ resourcesState.context ?? resourcesState.defaultContext }</div>
    </div>

    <div class="flex-2 h-full flex flex-col items-center">
<!--      <div class="h-4 mb-1"></div>-->
<!--      <div class="h-4 mb-1"></div>-->
      <div class="text-primary">{ resource.displayName } ({#if resource.isNamespaced && resourcesState.namespace}{resourcesState.namespace}{:else}all{/if}) [{resourcesState.selectedResourceItems.items.length}]</div>
    </div>

    <div class="flex-1"></div>
  </div>

  <div class="grow overflow-hidden w-full grid grid-rows-[auto_1fr] bg-white shadow-md">
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

    <div class="tbody overflow-y-auto" onscroll={() => uiState.isScrollingResourceItemsList = true} onscrollend={() => uiState.scrollingResourceItemsListEnded()}>
      {#each items as item}
        <ResourceItemsListDesktopListItem {item} {resource} {showNamespace} {index} isSelected={index === selectedIndex} />
      {/each}
    </div>
  </div>
</div>