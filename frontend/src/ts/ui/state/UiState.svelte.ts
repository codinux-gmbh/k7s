import {ShowYamlDialogOptions} from "../../../components/dialogs/resourceItem/ShowYamlDialogOptions"

export class UiState {

  static state = new UiState()


  showResourceSelectionPanel: boolean = $state(false)

  showYamlDialog: ShowYamlDialogOptions | undefined = $state()

}