<script lang="ts">
  import {KubernetesResource} from "../../../ts/model/KubernetesResource"

  let { resource, title = resource.displayName }: { resource: KubernetesResource, title?: string } = $props()
</script>

<li class="min-h-8 p-1 pl-6 pr-0 leading-6 cursor-pointer lg:hover:bg-zinc-200">
  <button class="text-start">
    { title }
  </button>
</li>