<script lang="ts">
  import {ResourceItem} from "../../ts/model/ResourceItem"
  import {KubernetesResource} from "../../ts/model/KubernetesResource"
  import ResourceItemContextMenu from "./ResourceItemContextMenu.svelte"

  let { item, resource }: { item: ResourceItem, resource: KubernetesResource } = $props()

  let showContextMenu = $state(false)

  let contextMenuTop: string | undefined = $state(undefined)
  let contextMenuBottom: string | undefined = $state(undefined)

  let triggerButton: HTMLElement
  let menuContainer: HTMLElement | undefined = $state()


  function handleContextMenuTriggerClick(event: MouseEvent) {
    preventFurtherActions(event)

    showContextMenu = !showContextMenu

    if (showContextMenu) {
      // Wait for menu to render
      requestAnimationFrame(() => {
        if (menuContainer) { // menuRef should now be set
          const buttonRect = triggerButton.getBoundingClientRect()
          const menuHeight = menuContainer.firstElementChild ? (menuContainer.firstElementChild as HTMLElement).offsetHeight : 0
          const spaceBelow = window.innerHeight - buttonRect.bottom
          const spaceAbove = buttonRect.top

          const placeAbove = spaceBelow < menuHeight && spaceAbove >= menuHeight
          if (placeAbove) {
            contextMenuTop = undefined
            contextMenuBottom = "100%"
          } else {
            contextMenuTop = "100%"
            contextMenuBottom = undefined
          }
        }
      })
    }
  }

  function preventFurtherActions(event: Event) {
    event.stopPropagation()
  }
</script>


<div class="w-full h-full relative inline-block">
  <button bind:this={triggerButton} class="dots-button w-full h-full flex items-center justify-center text-3xl text-zinc-500"
          onclick={handleContextMenuTriggerClick}>
    &#xFE19;
  </button>


  <div bind:this={menuContainer} class="h-full">
    <ResourceItemContextMenu {item} {resource} bind:showContextMenu={showContextMenu} top={contextMenuTop} bottom={contextMenuBottom} />
  </div>
</div>