/**
 * ReportAction.js
 *
 * @author realor
 */

import { FileExplorer } from "./FileExplorer.js";
import { FileAction } from "./FileAction.js";
import { MessageDialog } from "../MessageDialog.js";
import { ConfirmDialog } from "../ConfirmDialog.js";
import { Metadata } from "../../io/FileService.js";
import { ReportType } from "../../reports/ReportType.js";
import { ReportPanel } from "../ReportPanel.js";
import { ReportDialog } from "../ReportDialog.js";

class ReportAction extends FileAction
{
  constructor(fileExplorer, options)
  {
    super(fileExplorer, options);

    const application = fileExplorer.application;

    if (!fileExplorer.reportPanel)
    {
      fileExplorer.reportPanel = new ReportPanel(application);
      application.panelManager.addPanel(fileExplorer.reportPanel);
    }

    if (!fileExplorer.reportDialog)
    {
      fileExplorer.reportDialog = new ReportDialog(application,
        (name, code) => this.onSave(name, code));
      fileExplorer.reportDialog.reportPanel = fileExplorer.reportPanel;
    }
  }

  isEnabled()
  {
    const fileExplorer = this.fileExplorer;

    const type = fileExplorer.getSelectedFileExtension();

    if (!type) return false;

    return Boolean(ReportType.types[type]);
  }

  showReportDialog()
  {
    const fileExplorer = this.fileExplorer;
    const application = fileExplorer.application;
    const reportDialog = fileExplorer.reportDialog;
    const entryName = fileExplorer.selectedEntry?.name || "";

    if (reportDialog.reportName !== entryName)
    {
      MessageDialog.create("title.unsaved_changes",
        "message.inform_unsaved_changes", reportDialog.reportName)
        .setAction(() => reportDialog.show())
        .setAcceptLabel("button.accept")
        .setClassName("info")
        .setI18N(application.i18n).show();
    }
    else
    {
      reportDialog.show();
    }
  }

  setReport(url, source, reportTypeName = null, run = false)
  {
    const fileExplorer = this.fileExplorer;
    const reportDialog = fileExplorer.reportDialog;
    const reportPanel = fileExplorer.reportPanel;

    let index = url.lastIndexOf("/");
    let reportName = index === -1 ? url : url.substring(index + 1);

    if (reportTypeName === null)
    {
      index = reportName.lastIndexOf(".");
      reportTypeName = index === -1 ?
        ReportType.getDefaultReportTypeName() :
        reportName.substring(index + 1).toLowerCase();
    }

    if (run)
    {
      reportPanel.execute(reportName, source, reportTypeName);
    }
    else
    {
      reportDialog.reportName = reportName;
      reportDialog.reportSource = source;
      reportDialog.reportTypeName = reportTypeName;
      reportDialog.show();
    }
  }

  onSave(name, code)
  {
    const fileExplorer = this.fileExplorer;
    const application = fileExplorer.application;
    const reportDialog = fileExplorer.reportDialog;

    if (fileExplorer.service)
    {
      fileExplorer.save(name, code, () => reportDialog.saved = true);
    }
    else
    {
      MessageDialog.create("ERROR", "message.select_directory")
        .setClassName("error")
        .setI18N(application.i18n).show();
    }
  }
}

export { ReportAction };
