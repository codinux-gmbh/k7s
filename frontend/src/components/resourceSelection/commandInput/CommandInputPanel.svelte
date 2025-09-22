<script lang="ts">
  import {UiState} from "../../../ts/ui/state/UiState.svelte"
  import { clickOutside } from "../../../ts/ui/clickOutside"

  let { uiState }: { uiState: UiState } = $props()

  let previousShowCommandInputPanelValue = $state(false)

  let commandInput: HTMLInputElement

  let previousFocusedElement: Element | null = null

  $effect(() => {
    if (uiState.showCommandInputPanel != previousShowCommandInputPanelValue) {
      previousShowCommandInputPanelValue = uiState.showCommandInputPanel
      if (uiState.showCommandInputPanel) {
        commandInputBecameVisible()
      }
    }
  })


  function commandInputBecameVisible() {
    previousFocusedElement = document.activeElement // store current focused element to restore focus when hiding commandInput

    commandInput.focus()
  }

  function closePanel() {
    uiState.showCommandInputPanel = false

    commandInput.blur()
    commandInput.value = ""

    if (previousFocusedElement instanceof HTMLElement) {
      previousFocusedElement.focus()
    }
    previousFocusedElement = null
  }


  function commandInputLostFocus() {
    closePanel()
  }

  function commandInputChanged(event: Event) {

  }
</script>

{#if uiState.showCommandInputPanel}
  <div class={[ "fixed flex items-center w-[20rem] h-[3.5rem] p-2 top-2 right-1 shadow-2xl z-[999] bg-primary", uiState.showCommandInputPanel ? "" : "hidden" ]}
       use:clickOutside={closePanel}>
    <input bind:this={commandInput} type="search" list="availableCommands" class="w-full h-full p-2 px-1 bg-white rounded-3xl focus:outline-none"
           onchange={commandInputChanged} onblur={commandInputLostFocus} />
  </div>
{/if}