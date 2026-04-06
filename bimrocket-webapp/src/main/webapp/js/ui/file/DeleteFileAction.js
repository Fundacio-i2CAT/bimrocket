/**
 * DeleteFileAction.js
 *
 * @author realor
 */

import { FileExplorer } from "./FileExplorer.js";
import { FileAction } from "./FileAction.js";
import { ConfirmDialog } from "../ConfirmDialog.js";
import { Metadata } from "../../io/FileService.js";

class DeleteFileAction extends FileAction
{
  constructor(fileExplorer)
  {
    super(fileExplorer);
  }

  getLabel()
  {
    return "action.delete";
  }

  isEnabled()
  {
    return this.fileExplorer.isEntrySelected();
  }

  perform()
  {
    const fileExplorer = this.fileExplorer;
    const application = fileExplorer.application;
    let name = fileExplorer.selectedEntry.name;
    if (fileExplorer.service === null)
    {
      ConfirmDialog.create("title.delete_cloud_service",
        "question.delete_service", name)
        .setAction(() =>
        {
          let service = application.services[fileExplorer.group][name];
          application.removeService(service, fileExplorer.group);
          fileExplorer.refreshServices();
        })
        .setAcceptLabel("button.delete")
        .setI18N(application.i18n).show();
    }
    else
    {
      let question = fileExplorer.isFileEntrySelected() ?
        "question.delete_file" : "question.delete_folder";

      ConfirmDialog.create("title.delete_from_cloud", question, name)
        .setAction(() => fileExplorer.remove())
        .setAcceptLabel("button.delete")
        .setI18N(application.i18n).show();
    }
  }
}

export { DeleteFileAction };
