/**
 * CutFileAction.js
 *
 * @author realor
 */

import { FileExplorer } from "./FileExplorer.js";
import { FileAction } from "./FileAction.js";

class CutFileAction extends FileAction
{
  constructor(fileExplorer)
  {
    super(fileExplorer);
  }

  getLabel()
  {
    return "action.cut";
  }

  isEnabled()
  {
    const fileExplorer = this.fileExplorer;
    return fileExplorer.isDirectoryList() && fileExplorer.isEntrySelected();
  }

  perform()
  {
    this.fileExplorer.cut();
  }
}

export { CutFileAction };