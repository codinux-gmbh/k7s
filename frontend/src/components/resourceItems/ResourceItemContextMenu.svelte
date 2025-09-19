<script lang="ts">
  import ScaleIcon from "../../assets/icons/scale_24dp_zinc-700.svg"
  import DeleteIcon from "../../assets/icons/delete_24dp_red-700.svg"
  import MenuItem from "../common/menu/MenuItem.svelte"
  import MenuItemSeparator from "../common/menu/MenuItemSeparator.svelte"
  import {ResourceItem} from "../../ts/model/ResourceItem"
  import {KubernetesResource} from "../../ts/model/KubernetesResource"
  import { clickOutside } from "../../ts/ui/clickOutside"
  import {DI} from "../../ts/service/DI"

  let { item, resource }: { item: ResourceItem, resource: KubernetesResource } = $props()

  let showContextMenu = $state(false)

  let placeAbove = $state(false)

  let triggerButtonRef: HTMLElement
  let menuRef: HTMLElement | undefined = $state()


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

  function handleContextMenuTriggerClick(event: MouseEvent) {
    preventFurtherActions(event)

    showContextMenu = !showContextMenu

    if (showContextMenu) {
      // Wait for menu to render
      requestAnimationFrame(() => {
        if (menuRef) { // menuRef should now be set
          const buttonRect = triggerButtonRef.getBoundingClientRect()
          const menuHeight = menuRef.offsetHeight
          const spaceBelow = window.innerHeight - buttonRect.bottom
          const spaceAbove = buttonRect.top

          placeAbove = spaceBelow < menuHeight && spaceAbove >= menuHeight
        }
      })
    }
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


<div class="w-full h-full relative inline-block">
  <button bind:this={triggerButtonRef} class="dots-button w-full h-full flex items-center justify-center text-3xl text-zinc-500"
          onclick={handleContextMenuTriggerClick}>
    &#xFE19;
  </button>


  {#if showContextMenu}
    <ul bind:this={menuRef} role="menu" class="context-menu absolute right-0 w-48 bg-white shadow-2xl z-100"
         class:top-[100%]={!placeAbove} class:bottom-[100%]={placeAbove} use:clickOutside={closeMenu}>

      {#if resource.isLoggable}<MenuItem label="Logs" onClick={showLogs} />{/if}

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
</div>