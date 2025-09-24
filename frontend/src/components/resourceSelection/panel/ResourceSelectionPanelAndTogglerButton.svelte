<script lang="ts">
  import ResourceSelectionPanel from "./ResourceSelectionPanel.svelte"
  import ResourceSelectionPanelTogglerButton from "./ResourceSelectionPanelTogglerButton.svelte"
  import { clickOutside } from "../../../ts/ui/clickOutside"
  import {UiState} from "../../../ts/ui/state/UiState.svelte"
  import {ResourcesState} from "../../../ts/ui/state/ResourcesState.svelte"

  let { uiState, resourcesState }: { uiState: UiState, resourcesState: ResourcesState } = $props()


  function closePanel() {
    uiState.showResourceSelectionPanel = false
  }

  function clickOutsideOfPanel(event: Event) {
    const target = event.target as Element
    const hasToggleButtonBeenClicked = target?.id == "resourceSelectionPanelToggleButton" || target?.parentElement?.id == "resourceSelectionPanelToggleButton"

    if (hasToggleButtonBeenClicked == false) { // do not handle clicks on resourceSelectionPanelToggleButton, it does already the right thing
      closePanel()
    }
  }
</script>

<ResourceSelectionPanelTogglerButton {uiState} />

<div class={[ uiState.showResourceSelectionPanel ? "" : "hidden" ]} use:clickOutside={clickOutsideOfPanel}>
  <ResourceSelectionPanel {uiState} {resourcesState} />
</div>