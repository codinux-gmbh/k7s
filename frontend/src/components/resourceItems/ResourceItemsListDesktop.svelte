<script lang="ts">
  import {ResourcesState} from "../../ts/ui/state/ResourcesState.svelte"
  import {ResourceItem} from "../../ts/model/ResourceItem"
  import {ItemValue} from "../../ts/model/ItemValue"
  import {DI} from "../../ts/service/DI"

  let { resourcesState, showNamespace }: { resourcesState: ResourcesState, showNamespace: boolean } = $props()

  const itemsFormatter = DI.itemsFormatter


  function getColumnNames(): string[] {
    return itemsFormatter.getResourceSpecificColumnNames(resourcesState.selectedResource)
  }

  function getGridColumns(): string {
    return itemsFormatter.getGridColumns(resourcesState.selectedResource, showNamespace)
  }

  function itemSpecificValues(item: ResourceItem): ItemValue[] {
    return [ ...item.highlightedItemSpecificValues, ...item.secondaryItemSpecificValues ]
      .filter(value => value.showOnDesktop)
  }
</script>


<div class="w-full h-full grid grid-rows-[auto_1fr]">
  <div class="thead sticky top-0 z-2">
    <div class="grid items-center h-12 px-2 font-bold bg-zinc-200 text-zinc-500 border-b border-zinc-500" style={getGridColumns()}>
      {#if showNamespace}
        <div>Namespace</div>
      {/if}
      <div class="min-w-[17.25rem]">Name</div>

      {#each getColumnNames() as headerName}
        <div class="ml-1">{ headerName }</div>
      {/each}
    </div>
  </div>

  <div class="tbody overflow-y-auto">
    {#each resourcesState.selectedResourceItems.items as item}
      <div class="grid items-center h-12 px-2 border-b border-zinc-200 even:bg-zinc-100/50 hover:bg-zinc-200/50" style={getGridColumns()}>
        {#if showNamespace}
          <div class="truncate" title={ item.namespace }>{ item.namespace }</div>
        {/if}
        <div class="truncate" title={ item.name }>{ item.name }</div>

        {#each itemSpecificValues(item) as value}
          <div class="ml-1 truncate" title={ value.value }>{ value.value }</div>
        {/each}
      </div>
    {/each}
  </div>
</div>