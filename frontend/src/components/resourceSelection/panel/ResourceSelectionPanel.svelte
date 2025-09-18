<script lang="ts">
  import {UiState} from "../../../ts/ui/state/UiState.svelte"
  import {ResourcesState} from "../../../ts/ui/state/ResourcesState.svelte"
  import ResourcePanelItem from "./ResourcePanelItem.svelte"
  import ResourcePanelPlaceholder from "./ResourcePanelPlaceholder.svelte"
  import ResourcePanelOptionsItem from "./ResourcePanelOptionsItem.svelte"
  import {DI} from "../../../ts/service/DI"

  let { uiState, resourcesState }: { uiState: UiState, resourcesState: ResourcesState } = $props()

  function selectedNamespaceChanged(newNamespace: string) {
    DI.resourceItemsService.selectedNamespaceChanged(newNamespace)
  }
</script>


<div class="fixed bottom-[3.5rem] right-1 lg:right-[1px] max-md:left-1 md:max-w-[764px] bg-zinc-300 text-white rounded-2xl shadow-md z-[998]">

  <div class="flex justify-evenly flex-wrap p-[0.125rem]">
    <ResourcePanelPlaceholder />
    {#if resourcesState.contexts.length < 2}
      <ResourcePanelPlaceholder />
    {/if}
    <ResourcePanelOptionsItem label="ns" options={resourcesState.namespaces} selectedOption={resourcesState.namespace}
                              selectedOptionChanged={selectedNamespaceChanged} />


    <ResourcePanelItem group={null} kind="PersistentVolume" title="PV" {uiState} {resourcesState} />
    <ResourcePanelItem group={null} kind="PersistentVolumeClaim" title="PVC" {uiState} {resourcesState} />
    <ResourcePanelItem group={null} kind="Node" {uiState} {resourcesState} />

    <ResourcePanelItem group="rbac.authorization.k8s.io" kind="RoleBinding" {uiState} {resourcesState} />
    <ResourcePanelItem group="rbac.authorization.k8s.io" kind="ClusterRole" {uiState} {resourcesState} />
    <ResourcePanelItem group="rbac.authorization.k8s.io" kind="Role" {uiState} {resourcesState} />

    <ResourcePanelItem group={null} kind="ServiceAccount" {uiState} {resourcesState} />
    <ResourcePanelItem group={null} kind="Secret" {uiState} {resourcesState} />
    <ResourcePanelItem group={null} kind="ConfigMap" {uiState} {resourcesState} />

    <ResourcePanelItem group="apps" kind="DaemonSet" {uiState} {resourcesState} />
    <ResourcePanelItem group="apps" kind="StatefulSet" {uiState} {resourcesState} />
    <ResourcePanelItem group="apps" kind="Deployment" {uiState} {resourcesState} />

    <ResourcePanelItem group="networking.k8s.io" kind="Ingress" {uiState} {resourcesState} />
    <ResourcePanelItem group={null} kind="Service" {uiState} {resourcesState} />
    <ResourcePanelItem group={null} kind="Pod" {uiState} {resourcesState} />
  </div>
</div>