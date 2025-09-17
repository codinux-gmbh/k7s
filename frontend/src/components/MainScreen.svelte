<script lang="ts">

  import {DI} from "../ts/service/DI"
  import {onMount} from "svelte"
  import {ResourcesState} from "../ts/ui/state/ResourcesState.svelte"
  import ResourceSelectionSidebar from "./resourceSelection/sidebar/ResourceSelectionSidebar.svelte"

  let resourcesState = ResourcesState.state

  const apiClient = DI.apiClient

  onMount(updateResourceTypes)

  function updateResourceTypes() {
    apiClient.getAllAvailableResourceTypes(resourcesState.context)
      .then(response => resourcesState.resourceTypes = response)
  }
</script>


<div class="w-full h-dvh p-2 flex">
  <div class="shrink-0 max-md:hidden">
    <ResourceSelectionSidebar {resourcesState} />
  </div>
</div>