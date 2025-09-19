<script lang="ts">
  import {UiState} from "../../ts/ui/state/UiState.svelte"
  import ShowYamlDialog from "./resourceItem/ShowYamlDialog.svelte"
  import ItemLogsDialog from "./resourceItem/ItemLogsDialog.svelte"

  let { uiState }: { uiState: UiState } = $props()

</script>


{#if uiState.showLogsDialog}
  <ItemLogsDialog options={uiState.showLogsDialog} />
{/if}

{#if uiState.showYamlDialog}
  <ShowYamlDialog options={uiState.showYamlDialog} />
{/if}
