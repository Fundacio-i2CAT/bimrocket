/**
 * RenameFileAction.js
 *
 * @author realor
 */

import { FileExplorer } from "./FileExplorer.js";
import { FileAction } from "./FileAction.js";
import { Metadata } from "../../io/FileService.js";

class RenameFileAction extends FileAction
{
  constructor(fileExplorer)
  {
    super(fileExplorer);
  }

  getLabel()
  {
    return "action.rename";
  }

  isEnabled()
  {
    return this.fileExplorer.isEntrySelected() &&
           this.fileExplorer.isDirectoryList();
  }

  perform()
  {
    const fileExplorer = this.fileExplorer;
    const application = fileExplorer.application;
    const entryName = fileExplorer.selectedEntry.name;

    const dialog = new Dialog("title.rename_file");
    dialog.setSize(250, 130);
    dialog.setI18N(application.i18n);

    let nameElem = dialog.addTextField("file_name", "label.file_name");
    nameElem.setAttribute("spellcheck", "false");
    nameElem.value = entryName;

    dialog.addButton("rename_accept", "button.accept", () => dialog.onAccept());
    dialog.addButton("rename_cancel", "button.cancel", () => dialog.onCancel());

    dialog.onAccept = () =>
    {
      if (nameElem.value)
      {
        fileExplorer.rename(entryName, nameElem.value);
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

export { RenameFileAction };
