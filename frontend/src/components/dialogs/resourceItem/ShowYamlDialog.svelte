<script lang="ts">
  import BaseDialog from "../base/BaseDialog.svelte"
  import {ShowYamlDialogOptions} from "./ShowYamlDialogOptions"
  import {UiState} from "../../../ts/ui/state/UiState.svelte"

  let { options }: { options: ShowYamlDialogOptions } = $props()
</script>


<BaseDialog show={true} title={ `${options.resource.kind} ${options.item.name}` } confirmButtonVisible={false}
            dismissButtonText="Close" onClose={() => UiState.state.showYamlDialog = undefined}>
  <div class="w-full h-full p-3 whitespace-pre-wrap">
    { options.yaml }
  </div>
</BaseDialog>