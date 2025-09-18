<script lang="ts">
  import {ResourcesState} from "../../ts/ui/state/ResourcesState.svelte"
  import ResourceItemsListDesktop from "./ResourceItemsListDesktop.svelte"
  import ResourceItemsListMobile from "./ResourceItemsListMobile.svelte"

  let { resourcesState }: { resourcesState: ResourcesState } = $props()

  let showNamespace: boolean = $state(false)

  $effect(() => {
    showNamespace = resourcesState.selectedResource.isNamespaced || resourcesState.namespace != undefined
  })

</script>


<div class="h-full bg-white text-xs sm:text-sm shadow-md">

  <ResourceItemsListDesktop {resourcesState} {showNamespace} />

  <ResourceItemsListMobile {resourcesState} {showNamespace} />

</div>