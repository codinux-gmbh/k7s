<script lang="ts">
  import ComboBox from "../../common/form/ComboBox.svelte"

  let { label, options, selectedOption, selectedOptionChanged }:
    { label: string, options: string[], selectedOption?: string, selectedOptionChanged: (option: string) => void } = $props()

</script>


<div class="flex grow justify-center items-center w-[25vw] max-w-[15.5rem] h-[4rem] m-[0.15rem] px-0.5 py-1.5 bg-primary rounded-xl select-none">
  <ComboBox {label} {options} {selectedOption} {selectedOptionChanged} />
</div>