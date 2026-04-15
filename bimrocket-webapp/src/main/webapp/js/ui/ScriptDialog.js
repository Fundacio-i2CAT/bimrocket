/*
 * ScriptDialog.js
 *
 * @author realor
 */

import { Dialog } from "./Dialog.js";
import { Toast } from "./Toast.js";
import { MessageDialog } from "./MessageDialog.js";
import { ConfirmDialog } from "./ConfirmDialog.js";
import "../lib/codemirror.js";
import * as THREE from "three";

class ScriptDialog extends Dialog
{
  constructor(fileExplorer)
  {
    super("title.script_editor");
    this.fileExplorer = fileExplorer;
    this._changed = false;
    this._scriptName = null;

    const application = fileExplorer.application;

    this.setI18N(application.i18n);

    this.setSize(760, 600);
    this.bodyElem.classList.add("flex");
    this.bodyElem.classList.add("flex_column");

    this.nameField = this.addTextField("name", "tool.script.name", "",
      "script_name");
    this.nameField.setAttribute("spellcheck", "false");
    this.nameField.addEventListener("input", () =>
    {
      this.saveButton.disabled = !this.nameField.value.trim();
    });

    this.editorView = this.addCodeEditor("editor",
      "label.formula.expression", "",
      { language : "javascript", className : "flex_grow_1" });

    this.consoleElem = document.createElement("div");
    this.consoleElem.className = "console";
    this.bodyElem.appendChild(this.consoleElem);

    this.saveButton = this.addButton("save",
      "button.save", () => this.onSave());

    this.runButton = this.addButton("run", "button.run",
      () => this.run());

    this.closeButton = this.addButton("close",
      "button.close", () => this.hide());
  }

  get scriptName()
  {
    return this.nameField.value;
  }

  set scriptName(scriptName)
  {
    this.nameField.value = scriptName;
    this.saveButton.disabled = !scriptName?.trim();
  }

  get scriptCode()
  {
    return this.editorView.state.doc.toString();
  }

  set scriptCode(code)
  {
    this._changed = false;

    if (!code || code !== this.scriptCode)
    {
      Controls.setCodeEditorDocument(this.editorView, code,
      { language : "javascript" });

      const { EditorView } = CM["@codemirror/view"];
      const { StateEffect } = CM["@codemirror/state"];

      const changeListener = EditorView.updateListener.of(update =>
      {
        if (update.docChanged && this.visible)
        {
          this._changed = true;
        }
      });

      this.editorView.dispatch(
      {
        effects: StateEffect.appendConfig.of(changeListener)
      });
    }
  }

  onShow()
  {
    if (this.scriptName === "")
    {
      this.nameField.focus();
    }
    else
    {
      this.editorView.focus();
    }
    this._scriptName = this.scriptName;
  }

  hide()
  {
    if (this._changed)
    {
      const application = this.fileExplorer.application;

      ConfirmDialog.create("title.confirm_save",
        "question.discard_changes", this.scriptName)
        .setAction(() =>
        {
          this.scriptCode = "";
          this._changed = false;
          this._scriptName = null;
          this.clearConsole();
          super.hide();

          Toast.create("message.changes_discarded")
            .setI18N(application.i18n).show();
        })
        .setI18N(application.i18n)
        .setAcceptLabel("button.yes")
        .setCancelLabel("button.no")
        .show();
    }
    else
    {
      super.hide();
    }
  }

  async onSave()
  {
    const fileExplorer = this.fileExplorer;
    const application = fileExplorer.application;

    this.completeName();

    if (fileExplorer.service)
    {
      const isNew = this.scriptName !== this._scriptName;
      if (isNew)
      {
        if (!await fileExplorer.confirmSave(this.scriptName)) return;
      }

      fileExplorer.save(this.scriptName, this.scriptCode, () =>
      {
        this._changed = false;
        this._scriptName = this.scriptName;
      });
    }
    else
    {
      MessageDialog.create("ERROR", "message.select_directory")
        .setClassName("error")
        .setI18N(application.i18n).show();
    }
  }

  clearConsole()
  {
    this.consoleElem.innerHTML = "";
  }

  run()
  {
    const application = this.fileExplorer.application;
    let error = null;
    this.enterConsole();
    try
    {
      this.consoleElem.innerHTML = "";
      const fn = new Function(this.scriptCode);
      let t0 = Date.now();
      let result = fn();
      let t1 = Date.now();
      if (result instanceof Dialog)
      {
        result.show();
      }
      else
      {
        this.log("info", "Execution completed in " + (t1 - t0) + " ms.");
        if (result !== undefined) this.log("info", "Result: " + result);
        Toast.create("message.script_executed")
          .setI18N(application.i18n).show();
      }
    }
    catch (ex)
    {
      this.log("error", ex);
      error = ex;
    }
    finally
    {
      this.exitConsole();
    }
    return error;
  }

  log(className, ...args)
  {
    for (let arg of args)
    {
      let message = document.createElement("div");
      message.className = className;
      message.textContent = String(arg);
      this.consoleElem.appendChild(message);
    }
  }

  enterConsole()
  {
    this.console = console;

    window.console = {
      log : (...args) => this.log("info", ...args),
      info : (...args) => this.log("info", ...args),
      warn : (...args) => this.log("warn", ...args),
      error : (...args) => this.log("error", ...args)
    };
  }

  exitConsole()
  {
    window.console = this.console;
  }

  completeName()
  {
    let scriptName = this.scriptName.trim();
    if (!scriptName.endsWith(".js"))
    {
      scriptName += ".js";
    }
    this.scriptName = scriptName;
  }
}

export { ScriptDialog };
