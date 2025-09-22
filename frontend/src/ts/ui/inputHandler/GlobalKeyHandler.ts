import {UiState} from "../state/UiState.svelte"

export class GlobalKeyHandler {

  constructor(private readonly uiState: UiState) {
    this.init()
  }


  private init() {
    document.addEventListener("keydown", event => this.handleDocumentKeydown(event), true)
  }


  private handleDocumentKeydown(event: KeyboardEvent) {
    if (this.isNotInputElement(event.target)) {
      if ((event.key === ":" || event.key === "ö") // 'ö' for German keyboards
        && this.isNoModifierExceptShiftPressed(event)) {
        event.preventDefault()
        this.uiState.showCommandInputPanel = true
      }
    }
  }

  private isNoModifierExceptShiftPressed(event: KeyboardEvent): boolean {
    return event.ctrlKey == false && event.altKey == false && event.metaKey == false
  }

  private isNotInputElement(target: EventTarget | null): boolean {
    return this.isInputElement(target) == false
  }

  private isInputElement(target: EventTarget | null): boolean {
    if (target != null && target instanceof HTMLElement) {
      const tag = target.tagName.toLowerCase()
      return tag == "input" || tag == "textarea" || target.isContentEditable
    }

    return false
  }

}