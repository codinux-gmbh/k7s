<script lang="ts">
  import {ResourcesState} from "../../ts/ui/state/ResourcesState.svelte"
  import ResourceItemsListDesktop from "./ResourceItemsListDesktop.svelte"
  import ResourceItemsListMobile from "./ResourceItemsListMobile.svelte"
  import {Constants} from "../../ts/service/Constants"
  import {KubernetesResource} from "../../ts/model/KubernetesResource"
  import {UiState} from "../../ts/ui/state/UiState.svelte"

  let { uiState, resourcesState }: { uiState: UiState, resourcesState: ResourcesState } = $props()

  let resource: KubernetesResource = $state(resourcesState.selectedResource)

  let showNamespace: boolean = $state(false)


  $effect(() => {
    resource = resourcesState.selectedResource
  })

  $effect(() => {
    showNamespace = resourcesState.selectedResource.isNamespaced || resourcesState.namespace != undefined
  })

</script>


<div class="h-full text-sm">

  {#if Constants.isDesktop}
    <ResourceItemsListDesktop {uiState} {resource} {resourcesState} {showNamespace} />
  {/if}

  {#if Constants.isMobile}
    <ResourceItemsListMobile {uiState} {resource} {resourcesState} {showNamespace} />
  {/if}

</div>