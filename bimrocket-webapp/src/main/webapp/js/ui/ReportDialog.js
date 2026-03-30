/*
 * ReportDialog.js
 *
 * @author realor
 */

import { Dialog } from "./Dialog.js";
import { Toast } from "../ui/Toast.js";
import { Report } from "../reports/Report.js";
import { ReportType } from "../reports/ReportType.js";
import "../lib/codemirror.js";
import * as THREE from "three";

class ReportDialog extends Dialog
{
  constructor(application, saveAction)
  {
    super("title.report_editor");
    this.application = application;
    this.setI18N(this.application.i18n);
    this.reportTypeName = ReportType.getDefaultReportTypeName();
    this.reportPanel = null;
    this._savedReportSource = "";
    this._changed = false;

    this.setSize(760, 600);
    this.bodyElem.classList.add("flex");
    this.bodyElem.classList.add("flex_column");

    this.nameField = this.addTextField("name", "tool.report.name", "",
      "report_name");
    this.nameField.setAttribute("spellcheck", "false");

    this.editorView = this.addCodeEditor("editor",
      "tool.report.rules", "", { "className" : "flex_grow_1" });

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

    this.runButton = this.addButton("run", "button.run", () =>
    {
      this.hide();
      this.run();
    });

    this.saveButton = this.addButton("save", "button.save", () =>
    {
      if (this.validate())
      {
        this.addExtension();
        saveAction(this.reportName, this.reportSource);
        this._savedReportSource = this.reportSource;
        this._changed = false;
        this.updateButtons();
      }
    });

    this.discardButton = this.addButton("cancel", "button.discard", () =>
    {
      this.reportSource = this._savedReportSource;
      this._changed = false;
      this.updateButtons();

      Toast.create("message.changes_discarded")
        .setI18N(application.i18n).show();
    });

    this.closeButton = this.addButton("cancel", "button.close", () =>
    {
      this.hide();
    });

    this.nameField.addEventListener("input", () =>
    {
      this._changed = true;
      this.updateButtons();
    });
  }

  get reportName()
  {
    return this.nameField.value;
  }

  set reportName(reportName)
  {
    this.nameField.value = reportName;
  }

  get reportSource()
  {
    return this.editorView.state.doc.toString();
  }

  set reportSource(source)
  {
    this._savedReportSource = source;
    const state = this.editorView.state;
    const tx = state.update(
      { changes: { from: 0, to: state.doc.length, insert: source } });
    this.editorView.dispatch(tx);
  }

  hasUnsavedChanges()
  {
    return this.reportSource !== this._savedReportSource;
  }

  onShow()
  {
    if (this.reportName === "")
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

  validate()
  {
    try
    {
      const reportType = ReportType.getReportType(this.reportTypeName);
      if (reportType)
      {
        reportType.parse(this.reportSource);
      }
      return true;
    }
    catch (ex)
    {
      MessageDialog.create("ERROR", ex)
        .setClassName("error")
        .setI18N(this.application.i18n)
        .show();
    }
    return false;
  }

  run()
  {
    const reportPanel = this.reportPanel;
    if (reportPanel)
    {
      reportPanel.execute(this.reportName, this.reportSource, this.reportTypeName);
    }
  }

  addExtension()
  {
    const reportTypeName = this.reportTypeName;
    const reportName = this.reportName.toLowerCase();

    if (!reportName.endsWith("." + reportTypeName))
    {
      this.reportName += "." + reportTypeName;
    }
  }
}

export { ReportDialog };
