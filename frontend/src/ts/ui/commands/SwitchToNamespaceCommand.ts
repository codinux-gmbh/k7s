import type {Command} from "./Command"

export class SwitchToNamespaceCommand implements Command {

  constructor(readonly command: string, readonly namespace?: string) { }

}