<script lang="ts">
  import BaseDialog from "../base/BaseDialog.svelte"
  import {UiState} from "../../../ts/ui/state/UiState.svelte"
  import {ItemLogsDialogOptions} from "./ItemLogsDialogOptions"

  let { options }: { options: ItemLogsDialogOptions } = $props()
</script>


<BaseDialog show={true} title={ `${options.resource.kind} ${options.item.name}` } confirmButtonVisible={false}
            dismissButtonText="Close" onClose={() => UiState.state.showLogsDialog = undefined}>
  <div class="w-full max-w-[1240px] h-full bg-white text-[15px] overflow-y-auto">
    {#each options.logs as entry}
      <div class="w-full px-2 py-2 border-b first:border-t border-zinc-200 even:bg-zinc-100/50 hover:bg-zinc-200/50">
        { entry }
      </div>
    {/each}
  </div>
</BaseDialog>