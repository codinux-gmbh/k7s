<script lang="ts">
  import ResourceItemsListContextMenu from "./ResourceItemsListContextMenu.svelte"
  import {ResourceItem} from "../../ts/model/ResourceItem"
  import {DI} from "../../ts/service/DI"
  import {KubernetesResource} from "../../ts/model/KubernetesResource"
  import ResourceItemContextMenu from "./ResourceItemContextMenu.svelte"

  let { item, resource, showNamespace, index }: { item: ResourceItem, resource: KubernetesResource, showNamespace: boolean, index: number } = $props()

  let showContextMenu = $state(false)

  let contextMenuTop: string | undefined = $state(undefined)
  let contextMenuLeft: string | undefined = $state(undefined)

  const itemsFormatter = DI.itemsFormatter


  function getItemStyle(item: ResourceItem): string {
    return itemsFormatter.getItemStyle(item) ?? ""
  }

  function openContextMenu(event: MouseEvent) {
    event.preventDefault() // block the browser’s default menu

    contextMenuTop = `${event.clientY}px`
    contextMenuLeft = `${event.clientX}px`

    showContextMenu = true
  }
</script>


<div class={[ "w-full flex items-stretch min-h-[3.25rem] box-border border-b first:border-t border-zinc-200 even:bg-zinc-100/50 hover:bg-zinc-200/50",
               getItemStyle(item) ]} role="option" tabindex={index} aria-selected="false" oncontextmenu={e => openContextMenu(e)}>
  <div class="grow p-2 pr-0 flex flex-col justify-center overflow-hidden">
    <div class="flex items-center">
      {#if showNamespace}<div class="flex-none max-w-[5.5rem] md:max-w-[12rem] mr-1 font-medium truncate">{item.namespace}</div>{/if}
      <div class="flex-1 truncate">{item.name}</div>

      {#each item.highlightedItemSpecificValues as value}
        {#if value.showOnMobile}
          <div class="flex-none max-w-[8rem] ml-1 truncate">{#if value.mobileValue}{value.mobileValue}{:else}{value.name}: {value.value}{/if}</div>
        {/if}
      {/each}
    </div>

    {#if item.secondaryItemSpecificValues}
      <div class="w-full flex items-center mt-2 text-xs box-border">
        {#each item.secondaryItemSpecificValues as value}
          {#if value.showOnMobile}
            <div class={[ value.useRemainingSpace ? "flex-1" : "flex-none max-w-[8rem]", "mr-1 truncate" ]}>
              {#if value.mobileValue}{value.mobileValue}
              {:else}<span class="font-medium">{value.name}</span>: {value.value}{/if}
            </div>
          {/if}
        {/each}
      </div>
    {/if}
  </div>

  <div class="shrink-0 w-9 ml-1 p-0 flex items-center">
    <ResourceItemsListContextMenu {item} {resource} />
  </div>

  <ResourceItemContextMenu {item} {resource} bind:showContextMenu={showContextMenu} top={contextMenuTop} left={contextMenuLeft} />
</div>