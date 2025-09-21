
type Callback = (event: Event) => void

const registered = new Map<HTMLElement, Callback>()

function handleDocumentClick(event: MouseEvent) {
  for (const [node, callback] of registered) {
    if (!node.contains(event.target as Node)) {
      callback(event)
    }
  }
}

function handleDocumentKeydown(event: KeyboardEvent) {
  if (event.key === "Escape") {
    for (const callback of registered.values()) {
      callback(event)
    }
  }
}

function handleOnContextMenu(event: MouseEvent) {
  for (const callback of registered.values()) {
    callback(event)
  }
}

// Register listeners once
let initialized = false
function init() {
  if (initialized) return
  document.addEventListener("mousedown", handleDocumentClick, true)
  document.addEventListener("keydown", handleDocumentKeydown, true)
  document.addEventListener("contextmenu", handleOnContextMenu, true)
  initialized = true
}

export function clickOutside(node: HTMLElement, callback: Callback) {
  init()
  registered.set(node, callback)

  return {
    destroy() {
      registered.delete(node)
      if (registered.size === 0) {
        document.removeEventListener("mousedown", handleDocumentClick, true)
        document.removeEventListener("keydown", handleDocumentKeydown, true)
        document.removeEventListener("contextmenu", handleOnContextMenu, true)
        initialized = false
      }
    }
  }
}
