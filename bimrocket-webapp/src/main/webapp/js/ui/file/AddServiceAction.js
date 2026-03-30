/**
 * AddServiceAction.js
 *
 * @author realor
 */

import { FileExplorer } from "./FileExplorer.js";
import { FileAction } from "./FileAction.js";
import { ServiceDialog } from "../ServiceDialog.js";
import { ServiceManager } from "../../io/ServiceManager.js";
import { FileService } from "../../io/FileService.js";

class AddServiceAction extends FileAction
{
  constructor(fileExplorer)
  {
    super(fileExplorer);
  }

  getLabel()
  {
    return "action.add_service";
  }

  isEnabled()
  {
    return this.fileExplorer.isServiceList();
  }

  perform()
  {
    const fileExplorer = this.fileExplorer;
    const application = fileExplorer.application;
    const serviceTypes = ServiceManager.getTypesOf(FileService);
    let dialog = new ServiceDialog("title.add_cloud_service", serviceTypes);
    dialog.services = application.services[this.group];
    fileExplorer.addProxyFields(dialog);

    dialog.setI18N(application.i18n);
    dialog.onSave = (serviceType, parameters) =>
    {
      const service = new ServiceManager.classes[serviceType];
      fileExplorer.setServiceParameters(dialog, service, parameters);
    };
    dialog.show();
  }
}

export { AddServiceAction };