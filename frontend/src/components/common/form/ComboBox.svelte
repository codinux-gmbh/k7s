<script lang="ts">
  let { label, options, selectedOption, selectedOptionChanged }
    : { label: string, options: string[], selectedOption?: string, selectedOptionChanged: (option: string) => void } = $props()

  function selectedItemChanged(event: Event) {
    const selectedItem = (event.target as HTMLSelectElement).value
    selectedOptionChanged(selectedItem)
  }
</script>


<div class="flex items-center h-full">
  <label for={label} class="shrink-0 min-w-6 mr-1">{ label }</label>
  <select id={label} class="flex-1 min-w-0 h-full px-1 truncate bg-white text-primary rounded-lg" onchange={e => selectedItemChanged(e)}>
    {#each options as option}
      <option value={option} selected={option === selectedOption}>{ option }</option>
    {/each}
  </select>
</div>