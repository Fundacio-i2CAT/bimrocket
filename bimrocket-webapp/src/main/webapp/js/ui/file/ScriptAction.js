/**
 * ScriptAction.js
 *
 * @author realor
 */

import { FileExplorer } from "./FileExplorer.js";
import { FileAction } from "./FileAction.js";
import { ScriptDialog } from "../ScriptDialog.js";
import { MessageDialog } from "../MessageDialog.js";
import { Metadata } from "../../io/FileService.js";

class ScriptAction extends FileAction
{
  constructor(fileExplorer, options)
  {
    super(fileExplorer, options);

    if (!fileExplorer.scriptDialog)
    {
      fileExplorer.scriptDialog = new ScriptDialog(fileExplorer);
    }
  }

  isEnabled()
  {
    const fileExplorer = this.fileExplorer;

    const type = fileExplorer.getSelectedFileExtension();

    return type === "js";
  }

  setScript(url, code, run = false)
  {
    const fileExplorer = this.fileExplorer;
    const scriptDialog = fileExplorer.scriptDialog;

    const index = url.lastIndexOf("/");
    let name = url.substring(index + 1);

    scriptDialog.scriptName = name;
    scriptDialog.scriptCode = code;
    scriptDialog.clearConsole();
    if (run)
    {
      let error = scriptDialog.run();
      if (error)
      {
        scriptDialog.show();
      }
    }
    else
    {
      scriptDialog.show();
    }
  }
}

export { ScriptAction };
