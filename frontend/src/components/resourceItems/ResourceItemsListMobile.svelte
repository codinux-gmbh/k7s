<script lang="ts">
  import {ResourcesState} from "../../ts/ui/state/ResourcesState.svelte"

  let { resourcesState, showNamespace }: { resourcesState: ResourcesState, showNamespace: boolean } = $props()

</script>


<div class="w-full h-full overflow-y-auto">
  {#each resourcesState.selectedResourceItems.items as item}
    <div class="flex flex-col justify-center min-h-[3.25rem] px-2 border-b first:border-t border-zinc-200 even:bg-zinc-100/50 hover:bg-zinc-200/50">
      <div class="flex items-center">
        {#if showNamespace}<div class="flex-none max-w-[5.5rem] md:max-w-[12rem] mr-1 font-medium truncate">{item.namespace}</div>{/if}
        <div class="flex-1 truncate">{item.name}</div>
      </div>
    </div>
  {/each}
</div>