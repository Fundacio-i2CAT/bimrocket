/**
 * EditACLAction.js
 *
 * @author realor
 */

import { FileExplorer } from "./FileExplorer.js";
import { FileAction } from "./FileAction.js";
import { ACLEditorDialog } from "../ACLEditorDialog.js";

class EditACLAction extends FileAction
{
  constructor(fileExplorer)
  {
    super(fileExplorer);
  }

  getLabel()
  {
    return "action.edit_acl";
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
    const aclFilePath = fileExplorer.getFullPath(entryName);
    const dialog = new ACLEditorDialog(application, fileExplorer.service,
      aclFilePath, fileExplorer);
    dialog.load();
  }
}

export { EditACLAction };
