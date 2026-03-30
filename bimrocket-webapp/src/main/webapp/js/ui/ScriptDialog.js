/*
 * ScriptDialog.js
 *
 * @author realor
 */

import { Dialog } from "./Dialog.js";
import { Toast } from "../ui/Toast.js";
import "../lib/codemirror.js";
import * as THREE from "three";

class ScriptDialog extends Dialog
{
  constructor(application, saveAction)
  {
    super("title.script_editor");
    this.application = application;
    this.setI18N(this.application.i18n);
    this._savedScriptCode = "";
    this._changed = false;

    this.setSize(760, 600);
    this.bodyElem.classList.add("flex");
    this.bodyElem.classList.add("flex_column");

    this.nameField = this.addTextField("name", "tool.script.name", "",
      "script_name");

    this.editorView = this.addCodeEditor("editor",
      "label.formula.expression", "",
      { language : "javascript", className : "flex_grow_1" });

    const { EditorView } = CM["@codemirror/view"];
    const { StateEffect } = CM["@codemirror/state"];

    const changeListener = EditorView.updateListener.of(update =>
    {
      if (update.docChanged && this.visible)
      {
        this._changed = true;
        this.updateButtons();
      }
    });

    this.editorView.dispatch(
    {
      effects: StateEffect.appendConfig.of(changeListener)
    });

    this.consoleElem = document.createElement("div");
    this.consoleElem.className = "console";
    this.bodyElem.appendChild(this.consoleElem);

    this.runButton = this.addButton("run", "button.run", () =>
    {
      this.run();
    });

    this.saveButton = this.addButton("save", "button.save", () =>
    {
      if (!this.scriptName.endsWith(".js"))
      {
        this.scriptName += ".js";
      }
      saveAction(this.scriptName, this.scriptCode);
      this._savedScriptCode = this.scriptCode;
      this._changed = false;
      this.updateButtons();
    });

    this.saveButton.style.display = saveAction ? "" : "none";

    this.discardButton = this.addButton("cancel", "button.discard", () =>
    {
      this.scriptCode = this._savedScriptCode;
      this._changed = false;
      this.updateButtons();
      this.clearConsole();

      Toast.create("message.changes_discarded")
        .setI18N(application.i18n).show();
    });

    this.closeButton = this.addButton("close", "button.close", () =>
    {
      this.hide();
    });

    this.nameField.addEventListener("input", () =>
    {
      this._changed = true;
      this.updateButtons();
    });
  }

  get scriptName()
  {
    return this.nameField.value;
  }

  set scriptName(scriptName)
  {
    this.nameField.value = scriptName;
  }

  get scriptCode()
  {
    return this.editorView.state.doc.toString();
  }

  set scriptCode(code)
  {
    this._savedScriptCode = code;
    const state = this.editorView.state;
    const tx = state.update(
      { changes: { from: 0, to: state.doc.length, insert: code } });
    this.editorView.dispatch(tx);
  }

  hasUnsavedChanges()
  {
    return this.scriptCode !== this._savedScriptCode;
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
    this._changed = this.hasUnsavedChanges();
    this.updateButtons();
  }

  updateButtons()
  {
    const disabled = !this._changed;
    this.saveButton.disabled = disabled;
    this.discardButton.disabled = disabled;
  }

  clearConsole()
  {
    this.consoleElem.innerHTML = "";
  }

  run()
  {
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
          .setI18N(this.application.i18n).show();
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

  endEdition()
  {
    this.scriptName = this.nameField.value;
    this.scriptCode = this.editorView.state.doc.toString();
  };

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
}

export { ScriptDialog };
