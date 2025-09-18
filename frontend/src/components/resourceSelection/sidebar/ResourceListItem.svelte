<script lang="ts">
  import {KubernetesResource} from "../../../ts/model/KubernetesResource"
  import {DI} from "../../../ts/service/DI"

  let { resource, title = resource.displayName }: { resource: KubernetesResource, title?: string } = $props()

  const service = DI.resourceItemsService
</script>

<li class="min-h-8 p-1 pl-6 pr-0 leading-6 cursor-pointer lg:hover:bg-zinc-200">
  <button class="w-full text-start" onclick={() => service.selectedResourceChanged(resource)}>
    { title }
  </button>
</li>