import type {Command} from "./Command"

export class SwitchToContextCommand implements Command {

  constructor(readonly command: string, readonly context: string) { }

}