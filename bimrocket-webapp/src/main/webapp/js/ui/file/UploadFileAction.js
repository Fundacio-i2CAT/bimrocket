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
    inputFile.multiple = true;

    inputFile.addEventListener("change",
      event => this.uploadFiles(inputFile.files));
    inputFile.click();
  }

  uploadFiles(files)
  {
    const fileExplorer = this.fileExplorer;
    const queue = [...files];

    const uploadFile = async () =>
    {
      let file = queue.shift();
      if (file)
      {
        if (await fileExplorer.confirmSave(file.name))
        {
          this.fileExplorer.upload(file, uploadFile);
        }
        else
        {
          uploadFile();
        }
      }
    };

    uploadFile();
  }
}

export { UploadFileAction };
