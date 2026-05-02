/**
 * CopyFileAction.js
 *
 * @author realor
 */

import { FileExplorer } from "./FileExplorer.js";
import { FileAction } from "./FileAction.js";

class CopyFileAction extends FileAction
{
  constructor(fileExplorer)
  {
    super(fileExplorer);
  }

  getLabel()
  {
    return "action.copy";
  }

  isEnabled()
  {
    const fileExplorer = this.fileExplorer;
    return fileExplorer.isDirectoryList() && fileExplorer.isFileEntrySelected();
  }

  perform()
  {
    this.fileExplorer.copy();
  }
}

export { CopyFileAction };