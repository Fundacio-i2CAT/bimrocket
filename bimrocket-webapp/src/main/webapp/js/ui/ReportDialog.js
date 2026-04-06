/*
 * ReportDialog.js
 *
 * @author realor
 */

import { Dialog } from "./Dialog.js";
import { Controls } from "../ui/Controls.js";
import { Toast } from "../ui/Toast.js";
import { MessageDialog } from "./MessageDialog.js";
import { ConfirmDialog } from "../ui/ConfirmDialog.js";
import { Report } from "../reports/Report.js";
import { ReportType } from "../reports/ReportType.js";
import "../lib/codemirror.js";
import * as THREE from "three";

class ReportDialog extends Dialog
{
  constructor(fileExplorer)
  {
    super("title.report_editor");
    this.fileExplorer = fileExplorer;
    this.reportTypeName = ReportType.getDefaultReportTypeName();
    this.reportPanel = null;
    this._changed = false;
    this._reportName = null;

    const application = fileExplorer.application;

    this.setI18N(application.i18n);

    this.setSize(760, 600);
    this.bodyElem.classList.add("flex");
    this.bodyElem.classList.add("flex_column");

    this.nameField = this.addTextField("name", "tool.report.name", "",
      "report_name");
    this.nameField.setAttribute("spellcheck", "false");
    this.nameField.addEventListener("input", () =>
    {
      this.saveButton.disabled = !this.nameField.value.trim();
    });

    this.editorView = this.addCodeEditor("editor",
      "tool.report.rules", "", { "className" : "flex_grow_1" });

    this.saveButton = this.addButton("save",
      "button.save", () => this.onSave());

    this.runButton = this.addButton("run",
      "button.run", () => this.run());

    this.closeButton = this.addButton("close",
      "button.close", () => this.hide());
  }

  get reportName()
  {
    return this.nameField.value;
  }

  set reportName(reportName)
  {
    this.nameField.value = reportName;
    this.saveButton.disabled = !reportName?.trim();
  }

  get reportSource()
  {
    return this.editorView.state.doc.toString();
  }

  set reportSource(source)
  {
    this._changed = false;

    if (!source || source !== this.reportSource)
    {
      const reportType = ReportType.getReportType(this.reportTypeName);

      Controls.setCodeEditorDocument(this.editorView, source,
      { language : reportType.getSourceLanguage() });

      const { EditorView } = CM["@codemirror/view"];
      const { StateEffect } = CM["@codemirror/state"];

      const changeListener = EditorView.updateListener.of(update =>
      {
        if (update.docChanged && this.visible)
        {
          this._changed = true;
          this.runButton.disabled = true;
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
    if (this.reportName === "")
    {
      this.nameField.focus();
    }
    else
    {
      this.editorView.focus();
    }
    this._reportName = this.reportName;
  }

  hide()
  {
    if (this._changed)
    {
      const application = this.fileExplorer.application;

      ConfirmDialog.create("title.confirm_save",
        "question.discard_changes", this.reportName)
        .setAction(() =>
        {
          this.reportSource = "";
          this._changed = false;
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

    if (this.validate())
    {
      this.completeName();

      if (fileExplorer.service)
      {
        const isNew = this.reportName !== this._reportName;
        if (isNew)
        {
          if (!await fileExplorer.confirmSave(this.reportName)) return;
        }

        fileExplorer.save(this.reportName, this.reportSource, () =>
        {
          this._changed = false;
          this._reportName = this.reportName;
          this.runButton.disabled = false;
        });
      }
      else
      {
        MessageDialog.create("ERROR", "message.select_directory")
          .setClassName("error")
          .setI18N(application.i18n).show();
      }
    }
  }

  run()
  {
    super.hide();
    const reportPanel = this.reportPanel;
    if (reportPanel)
    {
      reportPanel.execute(this.reportName, this.reportSource, this.reportTypeName);
    }
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

  completeName()
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
