<script lang="ts">
  let { title, onClick }: { title: string, onClick: () => void } = $props()
</script>

<h2 class="flex items-baseline mt-6 text-zinc-400 font-semibold cursor-pointer select-none" onclick={() => onClick()}>
  <span class="inline-block w-[0.85rem] mr-2 text-center text-2xl font-bold text-primary leading-none relative top-[2px]">+</span>
  <span class="">{ title }</span>
</h2>