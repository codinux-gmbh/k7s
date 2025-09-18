<script lang="ts">
  import ComboBox from "../../common/form/ComboBox.svelte"
  import {Option} from "../../common/form/Option"

  let { label, options, selectedOption, selectedOptionChanged }:
    { label: string, options: Option[], selectedOption?: string, selectedOptionChanged: (option: string | undefined) => void } = $props()

</script>


<div class="flex grow justify-center items-center w-[25vw] max-w-[15.5rem] h-[4rem] m-[0.15rem] p-1.5 bg-primary rounded-xl select-none box-border">
  <ComboBox {label} {options} {selectedOption} {selectedOptionChanged} />
</div>