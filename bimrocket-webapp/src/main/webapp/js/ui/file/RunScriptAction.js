/**
 * RunScriptAction.js
 *
 * @author realor
 */

import { FileExplorer } from "./FileExplorer.js";
import { ScriptAction } from "./ScriptAction.js";

class RunScriptAction extends ScriptAction
{
  constructor(fileExplorer, options)
  {
    super(fileExplorer, options);
  }

  getLabel()
  {
    return "action.run_script";
  }

  isDefaultAction()
  {
    return true;
  }

  perform()
  {
    const fileExplorer = this.fileExplorer;
    const scriptDialog = fileExplorer.scriptDialog;

    if (scriptDialog.hasUnsavedChanges())
    {
      this.showScriptDialog();
    }
    else
    {
      this.fileExplorer.openSelectedEntry((url, result) =>
      {
        this.setScript(url, result.data, true);
      });
    }
  }
}

export { RunScriptAction };
