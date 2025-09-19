<script lang="ts">
  import type { Snippet } from "svelte"

  let { label, onClick, classes, children }:
      { label: string, onClick: (e: MouseEvent) => void, classes?: string, children?: Snippet } = $props()
</script>

<li role="menuitem">
  <button class={[ "menu-item w-full flex items-center px-2 py-2 select-none text-nowrap hover:!bg-zinc-200", classes ]} onclick={onClick}>
    <span class="w-6 h-6 max-w-6 max-h-6">
      {#if children}
        {@render children()}
      {/if}
    </span>

    <span class="ml-2">{ label }</span>
  </button>
</li>