import {ShowYamlDialogOptions} from "../../../components/dialogs/resourceItem/ShowYamlDialogOptions"
import {ItemLogsDialogOptions} from "../../../components/dialogs/resourceItem/ItemLogsDialogOptions"

export class UiState {

  static state = new UiState()


  isScrollingResourceItemsList: boolean = $state(false)

  showResourceSelectionPanel: boolean = $state(false)

  showCommandInputPanel: boolean = $state(false)

  showLogsDialog: ItemLogsDialogOptions | undefined = $state()

  showYamlDialog: ShowYamlDialogOptions | undefined = $state()


  scrollingResourceItemsListEnded() {
    // sometimes after scrollend event there's another false(!) scroll event -> so debounce
    setTimeout(() => this.isScrollingResourceItemsList = false, 100)
  }

}