/**
 * UploadFileAction.js
 *
 * @author realor
 */

import { FileExplorer } from "./FileExplorer.js";
import { FileAction } from "./FileAction.js";

class UploadFileAction extends FileAction
{
  constructor(fileExplorer)
  {
    super(fileExplorer);
  }

  getLabel()
  {
    return "action.upload_file";
  }

  isEnabled()
  {
    return this.fileExplorer.isDirectoryList();
  }

  perform()
  {
    const fileExplorer = this.fileExplorer;
    const inputFile = document.createElement("input");

    inputFile.type = "file";

    inputFile.addEventListener("change",
      event => fileExplorer.upload(inputFile.files));
    inputFile.click();
  }
}

export { UploadFileAction };
