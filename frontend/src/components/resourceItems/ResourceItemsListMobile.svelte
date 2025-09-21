<script lang="ts">
  import {ResourcesState} from "../../ts/ui/state/ResourcesState.svelte"
  import ResourceItemsListMobileListItem from "./ResourceItemsListMobileListItem.svelte"

  let { resourcesState, showNamespace }: { resourcesState: ResourcesState, showNamespace: boolean } = $props()
</script>


<div class="w-full h-full overflow-y-auto">
  {#each resourcesState.selectedResourceItems.items as item}
    <ResourceItemsListMobileListItem {item} resource={resourcesState.selectedResource} {showNamespace} />
  {/each}
</div>