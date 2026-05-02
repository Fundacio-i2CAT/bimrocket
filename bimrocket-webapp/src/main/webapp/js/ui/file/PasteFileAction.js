/**
 * PasteFileAction.js
 *
 * @author realor
 */

import { FileExplorer } from "./FileExplorer.js";
import { FileAction } from "./FileAction.js";

class PasteFileAction extends FileAction
{
  constructor(fileExplorer)
  {
    super(fileExplorer);
  }

  getLabel()
  {
    return "action.paste";
  }

  isEnabled()
  {
    return this.fileExplorer.isPasteEnabled();
  }

  perform()
  {
    this.fileExplorer.confirmPaste();
  }
}

export { PasteFileAction };