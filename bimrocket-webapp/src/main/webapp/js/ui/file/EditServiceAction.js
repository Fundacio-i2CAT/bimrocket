/**
 * EditServiceAction.js
 *
 * @author realor
 */

import { FileExplorer } from "./FileExplorer.js";
import { FileAction } from "./FileAction.js";
import { ServiceDialog } from "../ServiceDialog.js";
import { ServiceManager } from "../../io/ServiceManager.js";
import { FileService } from "../../io/FileService.js";

class EditServiceAction extends FileAction
{
  constructor(fileExplorer)
  {
    super(fileExplorer);
  }

  getLabel()
  {
    return "action.edit_service";
  }

  perform()
  {
    this.showEditServiceDialog();
  }

  isEnabled()
  {
    const fileExplorer = this.fileExplorer;
    return fileExplorer.isServiceList() && fileExplorer.isEntrySelected();
  }

  showEditServiceDialog()
  {
    const fileExplorer = this.fileExplorer;
    const application = fileExplorer.application;
    const entryName = fileExplorer.selectedEntry.name;
    const service = application.services[fileExplorer.group][entryName];

    const serviceTypes = ServiceManager.getTypesOf(FileService);
    let dialog = new ServiceDialog("title.edit_cloud_service",
      serviceTypes, service.constructor.name, service);
    fileExplorer.addProxyFields(dialog, service);

    dialog.setI18N(application.i18n);
    dialog.serviceTypeSelect.disabled = true;
    dialog.nameElem.readOnly = true;
    dialog.onSave = (serviceType, parameters) =>
    {
      fileExplorer.setServiceParameters(dialog, service, parameters);
    };
    dialog.show();
  }
}

export { EditServiceAction };