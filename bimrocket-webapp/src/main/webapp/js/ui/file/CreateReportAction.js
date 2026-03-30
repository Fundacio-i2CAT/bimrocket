/**
 * CreateReportAction.js
 *
 * @author realor
 */

import { FileExplorer } from "./FileExplorer.js";
import { ReportAction } from "./ReportAction.js";
import { ReportTypeDialog } from "../ReportTypeDialog.js";
import { ReportType } from "../../reports/ReportType.js";

class CreateReportAction extends ReportAction
{
  constructor(fileExplorer, options)
  {
    super(fileExplorer, options);
  }

  getLabel()
  {
    return this.options.label || "action.create_report";
  }

  isEnabled()
  {
    return this.fileExplorer.isDirectoryList();
  }

  perform()
  {
    const fileExplorer = this.fileExplorer;
    const application = fileExplorer.application;
    const reportDialog = fileExplorer.reportDialog;

    if (reportDialog.hasUnsavedChanges())
    {
      this.showReportDialog();
    }
    else
    {
      const typeDialog = new ReportTypeDialog(application, reportTypeName =>
      {
        const reportType = ReportType.types[reportTypeName];
        const source = reportType.getDefaultSource();
        this.setReport("", source, reportTypeName, false);
      });
      typeDialog.show();
    }
  }
}

export { CreateReportAction };
