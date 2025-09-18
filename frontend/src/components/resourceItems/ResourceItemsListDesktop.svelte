<script lang="ts">

  import {ResourcesState} from "../../ts/ui/state/ResourcesState.svelte"

  let { resourcesState, showNamespace }: { resourcesState: ResourcesState, showNamespace: boolean } = $props()

</script>


<div class="grid grid-rows-[auto_1fr] w-full h-full max-md:hidden">
  <div class="thead sticky top-0 z-2">
    <div class={[ "row grid h-12 font-bold bg-zinc-200 text-zinc-500 border-b border-zinc-500",
                    showNamespace ? "grid-cols-[9rem_1fr]" : "grid-cols-[1fr]"]}>
      {#if showNamespace}
        <div class="">Namespace</div>
      {/if}
      <div class="min-w-[17.25rem]">Name</div>

      <!--{#each resourcesState.selectedResource as headerName}-->
      <!--{/each}-->
    </div>
  </div>

  <div class="tbody overflow-y-auto">
    {#each resourcesState.selectedResourceItems.items as item}
      <div class={[ "row grid h-12 border-b border-zinc-200 even:bg-zinc-100/50 hover:bg-zinc-200/50",
                    showNamespace ? "grid-cols-[9rem_1fr]" : "grid-cols-[1fr]" ]}>
        {#if showNamespace}
          <div class="truncate" title={ item.namespace }>{ item.namespace }</div>
        {/if}
        <div class="truncate" title={ item.name }>{ item.name }</div>
      </div>
    {/each}
  </div>
</div>