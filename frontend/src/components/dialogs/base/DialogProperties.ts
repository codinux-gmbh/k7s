import type { Snippet } from "svelte"

export interface DialogProperties {

    children: Snippet,

    show: boolean,

    title: string,
    titleBarVisible: boolean,

    dismissButtonText: string,
    disallowCancellingDialog: boolean,

    confirmButtonText: string,
    confirmButtonVisible: boolean,
    confirmButtonEnabled: boolean,

    onClose: () => void,
    onConfirm: (() => boolean | Promise<boolean>) | undefined,

}