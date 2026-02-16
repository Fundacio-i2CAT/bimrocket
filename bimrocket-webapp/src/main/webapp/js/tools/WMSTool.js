/*
 * WMSTool.js
 *
 * @author nexus
 */

import { Tool } from "./Tool.js";
import { WMSDialog } from "../ui/WMSDialog.js";

class WMSTool extends Tool
{
  constructor(application, options)
  {
    super(application);
    this.name = "wms";
    this.label = "tool.wms.label";
    this.help = "tool.wms.help";
    this.className = "wms";
    this.setOptions(options);
    application.addTool(this);

    const dialog = new WMSDialog(application);
    this.dialog = dialog;

    dialog.onHide = () => this.application.useTool(null);

  }

  activate()
  {
    this.dialog.visible = true;
  }

  deactivate()
  {
    this.dialog.visible = false;
  }
}

export { WMSTool };
