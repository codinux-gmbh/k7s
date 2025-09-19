<script lang="ts">
  import {UiState} from "../../ts/ui/state/UiState.svelte"
  import ShowYamlDialog from "./resourceItem/ShowYamlDialog.svelte"

  let { uiState }: { uiState: UiState } = $props()

</script>


{#if uiState.showYamlDialog}
  <ShowYamlDialog options={uiState.showYamlDialog} />
{/if}
