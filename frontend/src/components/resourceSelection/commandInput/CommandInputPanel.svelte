<script lang="ts">
  import {UiState} from "../../../ts/ui/state/UiState.svelte"
  import { clickOutside } from "../../../ts/ui/clickOutside"
  import {ResourcesState} from "../../../ts/ui/state/ResourcesState.svelte"
  import type {Command} from "../../../ts/ui/commands/Command"
  import {DisplayResourceItemsCommand} from "../../../ts/ui/commands/DisplayResourceItemsCommand"
  import {KubernetesResource} from "../../../ts/model/KubernetesResource"
  import {SwitchToContextCommand} from "../../../ts/ui/commands/SwitchToContextCommand"
  import {SwitchToNamespaceCommand} from "../../../ts/ui/commands/SwitchToNamespaceCommand"
  import {DI} from "../../../ts/service/DI"

  let { uiState, resourcesState }: { uiState: UiState, resourcesState: ResourcesState } = $props()

  let previousShowCommandInputPanelValue = $state(false)
  let previousContext: string | undefined = resourcesState.context
  let previousResources: KubernetesResource[] = resourcesState.resourceTypes

  let commands: Command[] = $state(createCommands(resourcesState))

  let commandInput: HTMLInputElement

  let previousFocusedElement: Element | null = null

  const resourceItemsService = DI.resourceItemsService

  $effect(() => {
    if (uiState.showCommandInputPanel != previousShowCommandInputPanelValue) {
      previousShowCommandInputPanelValue = uiState.showCommandInputPanel
      if (uiState.showCommandInputPanel) {
        commandInputBecameVisible()
      }
    }
  })

  $effect(() => {
    if (previousContext != resourcesState.context || previousResources != resourcesState.resourceTypes) {
      previousContext = resourcesState.context
      previousResources = resourcesState.resourceTypes

      commands = createCommands(resourcesState)
    }
  })


  function commandInputBecameVisible() {
    previousFocusedElement = document.activeElement // store current focused element to restore focus when hiding commandInput

    commandInput.focus()
  }

  function closePanel() {
    uiState.showCommandInputPanel = false

    if (commandInput) {
      commandInput.blur()
      commandInput.value = ""
    }

    if (previousFocusedElement instanceof HTMLElement) {
      previousFocusedElement.focus()
    }
    previousFocusedElement = null
  }


  function commandInputChanged(event: Event) {
    event.preventDefault()

    const hasFocus = document.activeElement == commandInput
    if (hasFocus == false) { // this method also gets fired when commandInput just gets hidden, e.g. when pressing Escape ->
      return
    }

    const commandToExecute = commands.find(command => command.command == commandInput.value)
    if (commandToExecute) {
      if (commandToExecute instanceof SwitchToContextCommand) {
        resourceItemsService.selectedContextChanged(commandToExecute.context)
      } else if (commandToExecute instanceof SwitchToNamespaceCommand) {
        resourceItemsService.selectedNamespaceChanged(commandToExecute.namespace)
      } else if (commandToExecute instanceof DisplayResourceItemsCommand) {
        resourceItemsService.selectedResourceChanged(commandToExecute.resource)
      }
    } else {
      DI.log.warn(`No command found for command input ${commandInput.value}`)
      return // do not close panel then
    }

    closePanel()
  }

  function createCommands(resourcesState: ResourcesState): Command[] {
    let displayResourceItemsCommands = resourcesState.resourceTypes.sort((a, b) => a.kind.localeCompare(b.kind)).flatMap(res => {
      return [ new DisplayResourceItemsCommand(res.singularName ?? res.kind, res),
        ...res.shortNames.sort().map(shortName => new DisplayResourceItemsCommand(shortName, res)) ]
    })
    // custom shortcuts
    const deployment = resourcesState.resourceTypes.find(res => res.isDeployment)
    if (deployment) {
      displayResourceItemsCommands.push(new DisplayResourceItemsCommand("dp", deployment))
    }
    const roleBinding = resourcesState.resourceTypes.find(res => res.group == "rbac.authorization.k8s.io" && res.kind == "RoleBinding")
    if (roleBinding) {
      displayResourceItemsCommands.push(new DisplayResourceItemsCommand("rb", roleBinding))
    }

    const switchToNamespaceCommands = [
      new SwitchToNamespaceCommand("ns:all", undefined),
      ...resourcesState.namespaces.sort().map(ns => new SwitchToNamespaceCommand(`ns:${ns}`, ns))
      ]

    return [
      // sort. So that Contexts get displayed first, then namespaces, then resources
      ...resourcesState.contexts.sort().map(ctx => new SwitchToContextCommand(`ctx:${ctx}`, ctx)),
      ...switchToNamespaceCommands,
      ...displayResourceItemsCommands,
    ]
  }
</script>

<div class={[ "fixed flex items-center w-[20rem] h-[3.5rem] p-2 top-2 right-1 shadow-2xl z-[999] bg-primary",
            uiState.showCommandInputPanel && uiState.isScrollingResourceItemsList === false ? "" : "hidden" ]}
     use:clickOutside={closePanel}>
  <input bind:this={commandInput} type="search" list="availableCommands" class="w-full h-full p-2 px-1 bg-white rounded-3xl focus:outline-none"
         onchange={commandInputChanged} />

  <datalist id="availableCommands">
    {#each commands as command}
    <option value="{command.command}"></option>
    {/each}
  </datalist>
</div>