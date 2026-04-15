/**
 * EditScriptAction.js
 *
 * @author realor
 */

import { FileExplorer } from "./FileExplorer.js";
import { ScriptAction } from "./ScriptAction.js";

class EditScriptAction extends ScriptAction
{
  constructor(fileExplorer, options)
  {
    super(fileExplorer, options);
  }

  getLabel()
  {
    return "action.edit_script";
  }

  perform()
  {
    this.fileExplorer.open((url, result) =>
    {
      this.setScript(url, result.data, false);
    });
  }
}

export { EditScriptAction };
