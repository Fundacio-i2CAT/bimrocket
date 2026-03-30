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
      fileExplorer.scriptDialog = new ScriptDialog(fileExplorer.application,
        (name, code) => this.onSave(name, code));
    }
  }

  isEnabled()
  {
    const fileExplorer = this.fileExplorer;

    const type = fileExplorer.getSelectedFileExtension();

    return type === "js";
  }

  showScriptDialog()
  {
    const fileExplorer = this.fileExplorer;
    const application = fileExplorer.application;
    const scriptDialog = fileExplorer.scriptDialog;
    const entryName = fileExplorer.selectedEntry?.name || "";

    if (scriptDialog.scriptName !== entryName)
    {
      MessageDialog.create("title.unsaved_changes",
        "message.inform_unsaved_changes", scriptDialog.scriptName)
        .setAction(() => scriptDialog.show())
        .setAcceptLabel("button.accept")
        .setClassName("info")
        .setI18N(application.i18n).show();
    }
    else
    {
      scriptDialog.show();
    }
  }

  setScript(url, code, run = false)
  {
    const fileExplorer = this.fileExplorer;
    const scriptDialog = fileExplorer.scriptDialog;

    const index = url.lastIndexOf("/");
    let name = url.substring(index + 1);

    scriptDialog.scriptName = name;
    scriptDialog.scriptCode = code;
    scriptDialog.saved = true;
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

  onSave(name, code)
  {
    const fileExplorer = this.fileExplorer;
    const scriptDialog = fileExplorer.scriptDialog;

    if (fileExplorer.service)
    {
      fileExplorer.save(name, code, () => scriptDialog.saved = true);
    }
    else
    {
      MessageDialog.create("ERROR", "message.select_directory")
        .setClassName("error")
        .setI18N(this.application.i18n).show();
    }
  }

}

export { ScriptAction };
