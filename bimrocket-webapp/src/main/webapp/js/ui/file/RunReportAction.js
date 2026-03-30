/**
 * RunReportAction.js
 *
 * @author realor
 */

import { FileExplorer } from "./FileExplorer.js";
import { ReportAction } from "./ReportAction.js";

class RunReportAction extends ReportAction
{
  constructor(fileExplorer, options)
  {
    super(fileExplorer, options);
  }

  getLabel()
  {
    return "action.run_report";
  }

  isDefaultAction()
  {
    return true;
  }

  perform()
  {
    const fileExplorer = this.fileExplorer;
    const reportDialog = fileExplorer.reportDialog;

    if (reportDialog.hasUnsavedChanges())
    {
      this.showReportDialog();
    }
    else
    {
      this.fileExplorer.openSelectedEntry((url, result) =>
      {
        this.setReport(url, result.data, null, true);
      });
    }
  }
}

export { RunReportAction };
