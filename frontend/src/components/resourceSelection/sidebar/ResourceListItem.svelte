<script lang="ts">
  import {KubernetesResource} from "../../../ts/model/KubernetesResource"
  import {ResourcesState} from "../../../ts/ui/state/ResourcesState.svelte"

  let { resourcesState, resource, title = resource.displayName }: { resourcesState: ResourcesState, resource: KubernetesResource, title?: string } = $props()
</script>

<li class="min-h-8 p-1 pl-6 pr-0 leading-6 cursor-pointer lg:hover:bg-zinc-200">
  <button class="text-start" onclick={() => resourcesState.selectedResource = resource}>
    { title }
  </button>
</li>