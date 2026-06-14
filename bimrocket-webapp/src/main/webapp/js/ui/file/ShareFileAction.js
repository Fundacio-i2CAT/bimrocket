/**
 * ShareFileAction.js
 *
 * @author realor
 */

import { FileExplorer } from "./FileExplorer.js";
import { FileAction } from "./FileAction.js";
import { WebdavService } from "../../io/WebdavService.js";
import { ShareFileDialog } from "../ShareFileDialog.js";

class ShareFileAction extends FileAction
{
  constructor(fileExplorer)
  {
    super(fileExplorer);
  }

  getLabel()
  {
    return "action.share";
  }

  isDefaultAction()
  {
    return true;
  }

  isEnabled()
  {
    const fileExplorer = this.fileExplorer;
    const service = fileExplorer.service;
    return fileExplorer.isFileEntrySelected() &&
           service instanceof WebdavService;
  }

  perform()
  {
    const fileExplorer = this.fileExplorer;
    const path = fileExplorer.getSelectedPath();

    const dialog = new ShareFileDialog();
    dialog.setPath(path);
    dialog.setI18N(fileExplorer.application.i18n);
    dialog.show();
  }
}

export { ShareFileAction };
