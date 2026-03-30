/**
 * OpenFileAction.js
 *
 * @author realor
 */

import { FileExplorer } from "./FileExplorer.js";
import { FileAction } from "./FileAction.js";
import { IOManager } from "../../io/IOManager.js";

class OpenFileAction extends FileAction
{
  constructor(fileExplorer)
  {
    super(fileExplorer);
  }

  getLabel()
  {
    return "action.open";
  }

  isDefaultAction()
  {
    return true;
  }

  isEnabled()
  {
    const name = this.fileExplorer.selectedEntry?.name;
    const formatInfo = IOManager.getFormatInfo(name);
    if (formatInfo)
    {
      return !formatInfo.loader;
    }
    return false;
  }

  perform()
  {
    this.fileExplorer.openSelectedEntry((url, result) =>
    {
      const formatInfo = IOManager.getFormatInfo(result.path);
      if (formatInfo)
      {
        const blob = new Blob([result.data], { type: formatInfo.mimeType });
        const objectUrl = URL.createObjectURL(blob);

        window.open(objectUrl, '_blank');
      }
    });
  }
}

export { OpenFileAction };