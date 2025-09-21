<script lang="ts">
  import {ResourceItem} from "../../ts/model/ResourceItem"
  import {ItemValue} from "../../ts/model/ItemValue"
  import {DI} from "../../ts/service/DI"
  import {KubernetesResource} from "../../ts/model/KubernetesResource"
  import ResourceItemsListContextMenu from "./ResourceItemsListContextMenu.svelte"

  let { item, resource, showNamespace }: { item: ResourceItem, resource: KubernetesResource, showNamespace: boolean } = $props()

  const itemsFormatter = DI.itemsFormatter


  function getGridColumns(): string {
    return itemsFormatter.getGridColumns(resource, showNamespace)
  }

  function itemSpecificValues(item: ResourceItem): ItemValue[] {
    return [ ...item.highlightedItemSpecificValues, ...item.secondaryItemSpecificValues ]
      .filter(value => value.showOnDesktop)
  }

  function getItemStyle(item: ResourceItem): string {
    return itemsFormatter.getItemStyle(item) ?? ""
  }
</script>


<div class={[ "grid items-center h-12 pl-2 border-b border-zinc-200 even:bg-zinc-100/50 hover:bg-zinc-200/50",
      getItemStyle(item) ]} style={getGridColumns()}>
  {#if showNamespace}
    <div class="truncate mr-1" title={ item.namespace }>{ item.namespace }</div>
  {/if}
  <div class="truncate" title={ item.name }>{ item.name }</div>

  {#each itemSpecificValues(item) as value}
    <div class="ml-1 truncate" title={ value.value }>{ value.value }</div>
  {/each}


  <div class="h-full ml-1 p-0">
    <ResourceItemsListContextMenu {item} {resource} />
  </div>
</div>