<script lang="ts">
  import {UiState} from "../../../ts/ui/state/UiState.svelte"
  import {ResourcesState} from "../../../ts/ui/state/ResourcesState.svelte"
  import ResourcePanelItem from "./ResourcePanelItem.svelte"
  import ResourcePanelPlaceholder from "./ResourcePanelPlaceholder.svelte"
  import ResourcePanelOptionsItem from "./ResourcePanelOptionsItem.svelte"
  import {DI} from "../../../ts/service/DI"
  import {Option} from "../../common/form/Option"
  import {KubernetesResource} from "../../../ts/model/KubernetesResource"
  import {ResourceConstants} from "../../../ts/service/ResourceConstants"

  let { uiState, resourcesState }: { uiState: UiState, resourcesState: ResourcesState } = $props()

  let selectedResourceJson: string = $state("")

  $effect(() => {
    selectedResourceJson = resourceJson(resourcesState.selectedResource)
  })


  function namespaceOptions(): Option[] {
    return [
      ...resourcesState.namespaces.map(ns => new Option(ns)),
      new Option(undefined, "all"),
    ]
  }

  function selectedNamespaceChanged(newNamespace: string | undefined) {
    DI.resourceItemsService.selectedNamespaceChanged(newNamespace)
    uiState.showResourceSelectionPanel = false
  }

  function contextOptions(): Option[] {
    return [ ...resourcesState.contexts.map(ctx => new Option(ctx)) ]
  }

  function selectedContextChanged(newContext: string | undefined) {
    DI.resourceItemsService.selectedContextChanged(newContext)
    uiState.showResourceSelectionPanel = false
  }

  function standardResourceOptions(): Option[] {
    return [ ...resourcesState.standardResources.map(res => new Option(resourceJson(res), res.displayName)) ]
  }

  function customResourceDefinitionsOptions(): Option[] {
    return [ ...resourcesState.customResourceDefinitions.map(res => new Option(resourceJson(res), res.displayName)) ]
  }

  function selectedResourceChanged(newResourceJson: string | undefined) {
    uiState.showResourceSelectionPanel = false

    if (newResourceJson) {
      const groupAndKind = JSON.parse(newResourceJson)
      const newResource = resourcesState.resourceTypes.find(res => res.group == groupAndKind.group && res.kind == groupAndKind.kind)
      if (newResource) {
        DI.resourceItemsService.selectedResourceChanged(newResource)
      } else {
        DI.log.warn(`Could not find resource for group=${groupAndKind.group} and kind=${groupAndKind.kind}`)
      }
    }
  }

  function resourceJson(resource: KubernetesResource): string {
    if (resource.group) {
      return `{ "group": "${resource.group}", "kind": "${resource.kind}" }`
    } else {
      return `{ "kind": "${resource.kind}" }`
    }
  }
</script>


<div class="fixed bottom-[3.5rem] right-1 lg:right-[1px] max-md:left-1 md:max-w-[764px] bg-zinc-900/20 backdrop-blur-xs text-white rounded-2xl shadow-md z-[998]">

  <div class="flex justify-evenly flex-wrap px-[0.125rem] py-[0.25rem]">
    <!-- no context selection -> one row -->
    {#if resourcesState.contexts.length < 2}
      <ResourcePanelOptionsItem label="CRD" options={customResourceDefinitionsOptions()} selectedOption={selectedResourceJson}
                                selectedOptionChanged={selectedResourceChanged} />
      <ResourcePanelOptionsItem label="Std Res" options={standardResourceOptions()} selectedOption={selectedResourceJson}
                                selectedOptionChanged={selectedResourceChanged} />
      <ResourcePanelOptionsItem label="ns" options={namespaceOptions()} selectedOption={resourcesState.namespace}
                                selectedOptionChanged={selectedNamespaceChanged} />
    {:else}
      <!-- first row: Custom Resource Definitions and Standard Resources -->
      <ResourcePanelPlaceholder />
      <ResourcePanelOptionsItem label="CRD" options={customResourceDefinitionsOptions()} selectedOption={selectedResourceJson}
                                selectedOptionChanged={selectedResourceChanged} />
      <ResourcePanelOptionsItem label="Std Res" options={standardResourceOptions()} selectedOption={selectedResourceJson}
                                selectedOptionChanged={selectedResourceChanged} />
      <!-- second row: namespace and context -->
      <ResourcePanelPlaceholder />
      <ResourcePanelOptionsItem label="ctx" options={contextOptions()} selectedOption={resourcesState.context}
                                selectedOptionChanged={selectedContextChanged} />
      <ResourcePanelOptionsItem label="ns" options={namespaceOptions()} selectedOption={resourcesState.namespace}
                                selectedOptionChanged={selectedNamespaceChanged} />
    {/if}

    <!-- highlighted default resources -->
    <ResourcePanelItem group={ResourceConstants.RoleBasedAccessGroup} kind="RoleBinding" {uiState} {resourcesState} />
    <ResourcePanelItem group={ResourceConstants.RoleBasedAccessGroup} kind="ClusterRole" {uiState} {resourcesState} />
    <ResourcePanelItem group={ResourceConstants.RoleBasedAccessGroup} kind="Role" {uiState} {resourcesState} />

    <ResourcePanelItem group={null} kind="ServiceAccount" {uiState} {resourcesState} />
    <ResourcePanelItem group={null} kind="Secret" {uiState} {resourcesState} />
    <ResourcePanelItem group={null} kind="ConfigMap" {uiState} {resourcesState} />

    <ResourcePanelItem group={null} kind="PersistentVolume" title="PV" {uiState} {resourcesState} />
    <ResourcePanelItem group={null} kind="PersistentVolumeClaim" title="PVC" {uiState} {resourcesState} />
    <ResourcePanelItem group={null} kind="Node" {uiState} {resourcesState} />

    <ResourcePanelItem group={ResourceConstants.AppsGroup} kind="DaemonSet" {uiState} {resourcesState} />
    <ResourcePanelItem group={ResourceConstants.AppsGroup} kind="StatefulSet" {uiState} {resourcesState} />
    <ResourcePanelItem group={ResourceConstants.AppsGroup} kind="Deployment" {uiState} {resourcesState} />

    <ResourcePanelItem group={ResourceConstants.NetworkingGroup} kind="Ingress" {uiState} {resourcesState} />
    <ResourcePanelItem group={null} kind="Service" {uiState} {resourcesState} />
    <ResourcePanelItem group={null} kind="Pod" {uiState} {resourcesState} />
  </div>
</div>