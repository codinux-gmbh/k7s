import {ShowYamlDialogOptions} from "../../../components/dialogs/resourceItem/ShowYamlDialogOptions"
import {ItemLogsDialogOptions} from "../../../components/dialogs/resourceItem/ItemLogsDialogOptions"

export class UiState {

  static state = new UiState()


  showResourceSelectionPanel: boolean = $state(false)

  showCommandInputPanel: boolean = $state(false)

  showLogsDialog: ItemLogsDialogOptions | undefined = $state()

  showYamlDialog: ShowYamlDialogOptions | undefined = $state()

}