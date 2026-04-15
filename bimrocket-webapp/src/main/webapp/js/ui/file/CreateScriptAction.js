/**
 * CreateScriptAction.js
 *
 * @author realor
 */

import { FileExplorer } from "./FileExplorer.js";
import { ScriptAction } from "./ScriptAction.js";

class CreateScriptAction extends ScriptAction
{
  constructor(fileExplorer, options)
  {
    super(fileExplorer, options);
  }

  getLabel()
  {
    return this.options.label || "action.create_script";
  }

  isEnabled()
  {
    return this.fileExplorer.isDirectoryList();
  }

  perform()
  {
    this.setScript("", "", false);
  }
}

export { CreateScriptAction };
