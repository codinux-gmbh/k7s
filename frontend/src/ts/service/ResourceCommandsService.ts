import {ResourcesState} from "../ui/state/ResourcesState.svelte"
import type {Command} from "../ui/commands/Command"
import {DisplayResourceItemsCommand} from "../ui/commands/DisplayResourceItemsCommand"
import {SwitchToNamespaceCommand} from "../ui/commands/SwitchToNamespaceCommand"
import {SwitchToContextCommand} from "../ui/commands/SwitchToContextCommand"
import {DI} from "./DI"
import {ResourceItemsService} from "./ResourceItemsService"

export class ResourceCommandsService {

  constructor(private readonly resourceItemsService: ResourceItemsService) { }


  executeCommand(commands: Command[], enteredCommand: string): boolean {
    const commandToExecute = commands.find(command => command.command == enteredCommand)

    if (commandToExecute) {
      if (commandToExecute instanceof SwitchToContextCommand) {
        this.resourceItemsService.selectedContextChanged(commandToExecute.context)
      } else if (commandToExecute instanceof SwitchToNamespaceCommand) {
        this.resourceItemsService.selectedNamespaceChanged(commandToExecute.namespace)
      } else if (commandToExecute instanceof DisplayResourceItemsCommand) {
        this.resourceItemsService.selectedResourceChanged(commandToExecute.resource)
      }

      return true
    } else {
      DI.log.warn(`No command found for command input ${enteredCommand}`)
      return false // do not close panel then
    }
  }


  createCommands(resourcesState: ResourcesState): Command[] {
    const displayResourceItemsCommands = this.createDisplayResourceItemsCommands(resourcesState)

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

  private createDisplayResourceItemsCommands(resourcesState: ResourcesState): DisplayResourceItemsCommand[] {
    const displayResourceItemsCommands = resourcesState.resourceTypes
      .sort((a, b) => a.kind.localeCompare(b.kind)).flatMap(res => {
        return [ new DisplayResourceItemsCommand(res.singularName ?? res.kind, res),
          ...res.shortNames.sort().map(shortName => new DisplayResourceItemsCommand(shortName, res)) ]
      })

    // custom shortcuts
    const deployment = resourcesState.resourceTypes.find(res => res.isDeployment)
    if (deployment) {
      displayResourceItemsCommands.push(new DisplayResourceItemsCommand("dp", deployment))
    }
    const roleBinding = resourcesState.resourceTypes
      .find(res => res.group == "rbac.authorization.k8s.io" && res.kind == "RoleBinding")
    if (roleBinding) {
      displayResourceItemsCommands.push(new DisplayResourceItemsCommand("rb", roleBinding))
    }

    return displayResourceItemsCommands
  }

}