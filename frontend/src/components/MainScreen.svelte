<script lang="ts">
  import {DI} from "../ts/service/DI"
  import {onMount} from "svelte"
  import {ResourcesState} from "../ts/ui/state/ResourcesState.svelte"
  import ResourceSelectionSidebar from "./resourceSelection/sidebar/ResourceSelectionSidebar.svelte"
  import ResourceItemsList from "./resourceItems/ResourceItemsList.svelte"
  import {Constants} from "../ts/service/Constants"
  import {UiState} from "../ts/ui/state/UiState.svelte"
  import ResourceSelectionPanelTogglerButton from "./resourceSelection/panel/ResourceSelectionPanelTogglerButton.svelte"
  import ResourceSelectionPanel from "./resourceSelection/panel/ResourceSelectionPanel.svelte"
  import { clickOutside } from "../ts/ui/clickOutside"
  import Dialogs from "./dialogs/Dialogs.svelte"
  import CommandInputPanel from "./resourceSelection/commandInput/CommandInputPanel.svelte"
  import {GlobalKeyHandler} from "../ts/ui/inputHandler/GlobalKeyHandler"

  let uiState = UiState.state
  let resourcesState = ResourcesState.state

  const keyHandler = new GlobalKeyHandler(uiState)
  const service = DI.resourceItemsService

  onMount(() => service.selectedContextChanged(undefined))

  function closePanel() {
    uiState.showResourceSelectionPanel = false
  }
</script>


<div class="w-full h-dvh p-2 flex">
  {#if Constants.isDesktop}
    <div class="shrink-0">
      <ResourceSelectionSidebar {resourcesState} />
    </div>
  {/if}

  <div class="flex-1 w-full max-w-full h-full max-h-full">
    <ResourceItemsList {resourcesState} />
  </div>


  <ResourceSelectionPanelTogglerButton {uiState} />

  <div class={[ uiState.showResourceSelectionPanel ? "" : "hidden" ]} use:clickOutside={closePanel}>
    <ResourceSelectionPanel {uiState} {resourcesState} />
  </div>

  <CommandInputPanel {uiState} {resourcesState} />
</div>


<Dialogs {uiState} />