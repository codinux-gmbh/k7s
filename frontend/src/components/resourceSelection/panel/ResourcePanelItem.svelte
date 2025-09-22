<script lang="ts">
  import {UiState} from "../../../ts/ui/state/UiState.svelte"
  import {KubernetesResource} from "../../../ts/model/KubernetesResource"
  import {ResourcesState} from "../../../ts/ui/state/ResourcesState.svelte"
  import {DI} from "../../../ts/service/DI"

  let { group, kind, title, uiState, resourcesState }:
    { group: string | null, kind: string, title?: string, uiState: UiState, resourcesState: ResourcesState } = $props()

  let resource: KubernetesResource | undefined = $state(undefined)

  $effect(() => {
    resource = resourcesState.resourceTypes.find(res => res.kind == kind && res.group == (group ? group : undefined))
  })

  function resourceSelected() {
    uiState.showResourceSelectionPanel = false
    if (resource) { // should always be true
      DI.resourceItemsService.selectedResourceChanged(resource)
    }
  }
</script>


<button class="flex grow justify-center items-center w-[25vw] max-w-[15.5rem] h-[3.5rem] m-[0.15rem] px-3 !bg-primary rounded-xl select-none"
        onclick={resourceSelected}>
  { title ?? resource?.displayName ?? "" }
</button>