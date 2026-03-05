/**
 * 
 * author: i2CAT
 */

import { Tool } from "./Tool.js";

class HidePanelsTool extends Tool
{
  constructor(application, options)
  {
    super(application);
    this.name = "hide_panels";
    this.label = "hide_panels";
    this.className = "hide_panels";
    this.setOptions(options);
    application.addTool(this);
    this.immediate = true;
  }

  execute()
  {
    this.application.hidePanels();
  }
}

export { HidePanelsTool }