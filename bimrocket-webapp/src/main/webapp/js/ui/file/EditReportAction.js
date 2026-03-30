/**
 * EditReportAction.js
 *
 * @author realor
 */

import { FileExplorer } from "./FileExplorer.js";
import { ReportAction } from "./ReportAction.js";

class EditReportAction extends ReportAction
{
  constructor(fileExplorer, options)
  {
    super(fileExplorer, options);
  }

  getLabel()
  {
    return "action.edit_report";
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
        this.setReport(url, result.data, null, false);
      });
    }
  }
}

export { EditReportAction };
