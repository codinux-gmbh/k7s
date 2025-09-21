<script lang="ts">
  import LogsIcon from "../../assets/icons/subject_24dp_zinc-700.svg"
  import ScaleIcon from "../../assets/icons/scale_24dp_zinc-700.svg"
  import DeleteIcon from "../../assets/icons/delete_24dp_red-700.svg"
  import MenuItem from "../common/menu/MenuItem.svelte"
  import MenuItemSeparator from "../common/menu/MenuItemSeparator.svelte"
  import {ResourceItem} from "../../ts/model/ResourceItem"
  import {KubernetesResource} from "../../ts/model/KubernetesResource"
  import { clickOutside } from "../../ts/ui/clickOutside"
  import {DI} from "../../ts/service/DI"

  let { item, resource, showContextMenu = $bindable(false), top, bottom, left, right }:
    { item: ResourceItem, resource: KubernetesResource, showContextMenu: boolean, top?: string, bottom?: string, left?: string, right?: string } = $props()


  function getStyle(): string {
    return [
      top ? `top:${top};` : '',
      bottom ? `bottom:${bottom};` : '',
      left ? `left:${left};` : '',
      right ? `right:${right};` : ''
    ].join(" ")
  }

  function showLogs(event: MouseEvent) {
    preventFurtherActionsAndCloseMenu(event)

    DI.resourceItemsService.showLogs(item, resource)
  }

  function showYaml(event: MouseEvent) {
    preventFurtherActionsAndCloseMenu(event)

    DI.resourceItemsService.showYaml(item, resource)
  }

  function scaleItem(event: MouseEvent) {
    preventFurtherActionsAndCloseMenu(event)

    DI.resourceItemsService.scaleItem(item, resource)
  }

  function deleteItem(event: MouseEvent) {
    preventFurtherActionsAndCloseMenu(event)

    DI.resourceItemsService.deleteItem(item, resource)
  }

  function killItem(event: MouseEvent) {
    preventFurtherActionsAndCloseMenu(event)

    DI.resourceItemsService.killItem(item, resource)
  }

  function preventFurtherActions(event: Event) {
    event.stopPropagation()
  }

  function preventFurtherActionsAndCloseMenu(event: Event) {
    preventFurtherActions(event)
    closeMenu()
  }

  function closeMenu() {
    showContextMenu = false
  }
</script>


{#if showContextMenu}
  <ul role="menu" use:clickOutside={closeMenu} class={[ "context-menu absolute w-48 bg-white shadow-2xl z-100" ]} style={getStyle()} >

    {#if resource.isLoggable}
      <MenuItem label="Logs" onClick={showLogs}>
        <img class="h-[24px]" alt="Show resource's logs" src={LogsIcon} />
      </MenuItem>
    {/if}

    {#if resource.isScalable}
      <MenuItem label="Scale" onClick={scaleItem}>
        <img class="h-[24px]" alt="Scale resource" src={ScaleIcon} />
      </MenuItem>
    {/if}

    <MenuItem label="YAML" onClick={showYaml} />

    <MenuItemSeparator />

    {#if resource.isDeletable}
      <MenuItem label="Delete" classes="text-red-700 hover:!bg-red-200" onClick={deleteItem}>
        <img class="h-[24px]" alt="Delete resource" src={DeleteIcon} />
      </MenuItem>

      {#if resource.isPod}
        <MenuItem label="Kill" classes="text-red-700 hover:!bg-red-200" onClick={killItem}>
          <img class="h-[24px]" alt="Delete resource" src={DeleteIcon} />
        </MenuItem>
      {/if}
    {/if}
  </ul>
{/if}