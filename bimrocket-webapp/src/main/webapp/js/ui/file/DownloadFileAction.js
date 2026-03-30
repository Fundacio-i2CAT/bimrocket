/**
 * DownloadFileAction.js
 *
 * @author realor
 */

import { FileExplorer } from "./FileExplorer.js";
import { FileAction } from "./FileAction.js";

class DownloadFileAction extends FileAction
{
  constructor(fileExplorer)
  {
    super(fileExplorer);
  }

  getLabel()
  {
    return "action.download_file";
  }

  isEnabled()
  {
    const fileExplorer = this.fileExplorer;
    return fileExplorer.isDirectoryList() && fileExplorer.isFileEntrySelected();
  }

  perform()
  {
    const fileExplorer = this.fileExplorer;
    const entryName = fileExplorer.selectedEntry.name;
    fileExplorer.download(entryName);
  }
}

export { DownloadFileAction };