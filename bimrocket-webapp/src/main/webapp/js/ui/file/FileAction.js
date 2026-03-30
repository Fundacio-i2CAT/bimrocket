/**
 * FileAction.js
 *
 * @author realor
 */

import { Action } from "../Action.js";
import { FileExplorer } from "./FileExplorer.js";

class FileAction extends Action
{
  constructor(fileExplorer, options = {})
  {
    super();
    this.fileExplorer = fileExplorer;
    this.options = options;
  }

  isDefaultAction()
  {
    return false;
  }
}

export { FileAction };

