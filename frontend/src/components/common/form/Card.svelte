<script lang="ts">
  import type { Snippet } from "svelte"

  let { children, classes = null }: { children: Snippet, classes: string | null } = $props()
</script>

<div class={[ "bg-white rounded-xl shadow-xl", classes ]}>
  {@render children()}
</div>