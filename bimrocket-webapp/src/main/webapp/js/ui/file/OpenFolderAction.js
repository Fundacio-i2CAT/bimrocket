/**
 * OpenFolderAction.js
 *
 * @author realor
 */

import { FileExplorer } from "./FileExplorer.js";
import { FileAction } from "./FileAction.js";

class OpenFolderAction extends FileAction
{
  constructor(fileExplorer)
  {
    super(fileExplorer);
  }

  getLabel()
  {
    return "action.open";
  }

  isDefaultAction()
  {
    return true;
  }

  isEnabled()
  {
    const fileExplorer = this.fileExplorer;
    return fileExplorer.isCollectionEntrySelected() ||
           fileExplorer.isServiceEntrySelected();
  }

  perform()
  {
    this.fileExplorer.openSelectedEntry();
  }
}

export { OpenFolderAction };