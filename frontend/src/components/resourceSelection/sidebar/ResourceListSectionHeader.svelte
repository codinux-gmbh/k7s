<script lang="ts">
  let { title, isExpanded = $bindable(false) }: { title: string, isExpanded: boolean } = $props()
</script>

<h2 class="flex items-baseline mt-6 text-zinc-400 font-semibold cursor-pointer select-none" onclick={() => isExpanded = !isExpanded}>
  <span class="inline-block w-[0.85rem] mr-2 text-center text-2xl font-bold text-primary leading-none relative top-[2px]">
    {#if isExpanded}
      -
    {:else}
      +
    {/if}
  </span>
  <span class="">{ title }</span>
</h2>