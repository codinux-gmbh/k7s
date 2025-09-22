<script lang="ts">
  import KubernetesImage from "../../../assets/icons/kubernetes-icon-color.svg"
  import {UiState} from "../../../ts/ui/state/UiState.svelte"

  let { uiState }: { uiState: UiState } = $props()

  function buttonClicked() {
    uiState.showResourceSelectionPanel = !!!uiState.showResourceSelectionPanel
  }
</script>

<button id="toggleButton" class="fixed w-12 h-12 bottom-1 right-1 lg:right-[1px] p-[0.2rem] !bg-primary rounded-lg shadow-md select-none z-[999]"
     onclick={buttonClicked}>
  <!-- Disable context menu so that no context menu to view / download image gets displayed -->
  <img alt="Toggle resources panel" src={KubernetesImage} oncontextmenu={() => false}>
</button>