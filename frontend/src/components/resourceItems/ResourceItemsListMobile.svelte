<script lang="ts">
  import {ResourcesState} from "../../ts/ui/state/ResourcesState.svelte"

  let { resourcesState, showNamespace }: { resourcesState: ResourcesState, showNamespace: boolean } = $props()

</script>


<div class="overflow-y-auto lg:hidden">
  {#each resourcesState.selectedResourceItems.items as item}
    <div class="flex flex-col justify-center min-h-[3.25rem] px-2 border-b first:border-t border-zinc-200 even:bg-zinc-100/50 hover:bg-zinc-200/50">
      <div class="flex items-center">
        {#if showNamespace}<div class="max-w-[5.5rem] md:max-w-[12rem] mr-1 flex-[0_0_auto] font-medium truncate">{item.namespace}</div>{/if}
        <div class="grow truncate">{item.name}</div>
      </div>
    </div>
  {/each}
</div>