/**
 * CreateFolderAction.js
 *
 * @author realor
 */

import { FileExplorer } from "./FileExplorer.js";
import { FileAction } from "./FileAction.js";
import { Dialog } from "../Dialog.js";

class CreateFolderAction extends FileAction
{
  constructor(fileExplorer)
  {
    super(fileExplorer);
  }

  getLabel()
  {
    return "action.create_folder";
  }

  isEnabled()
  {
    return this.fileExplorer.isDirectoryList();
  }

  perform()
  {
    const fileExplorer = this.fileExplorer;
    const application = fileExplorer.application;
    const dialog = new Dialog("title.create_folder_in_cloud");
    dialog.setSize(250, 130);
    dialog.setI18N(application.i18n);
    let nameElem = dialog.addTextField("folder_name", "label.folder_name");
    nameElem.setAttribute("spellcheck", "false");

    dialog.addButton("folder_accept", "button.create", () => dialog.onAccept());
    dialog.addButton("folder_cancel", "button.cancel", () => dialog.onCancel());

    dialog.onAccept = () =>
    {
      const folderName = nameElem.value;
      if (folderName)
      {
        fileExplorer.makeFolder(folderName);
        dialog.hide();
      }
    };
    dialog.onCancel = () =>
    {
      dialog.hide();
    };
    dialog.onShow = () =>
    {
      nameElem.focus();
    };
    dialog.show();
  }
}

export { CreateFolderAction };
