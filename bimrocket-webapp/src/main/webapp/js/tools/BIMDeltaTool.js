/*
 * BIMDeltaTool.js
 *
 * @author realor
 */

import { Tool } from "./Tool.js";
import { Tree } from "../ui/Tree.js";
import { Panel } from "../ui/Panel.js";
import { TabbedPane } from "../ui/TabbedPane.js";
import { MessageDialog } from "../ui/MessageDialog.js";
import { FileExplorer } from "../ui/file/FileExplorer.js";
import { ObjectUtils } from "../utils/ObjectUtils.js";
import { ModelSnapshot } from "../utils/ModelSnapshot.js";
import { CompareSnapshotAction } from "../ui/file/CompareSnapshotAction.js";
import { SaveSnapshotAction } from "../ui/file/SaveSnapshotAction.js";
import { I18N } from "../i18n/I18N.js";

class BIMDeltaTool extends Tool
{
  constructor(application, options)
  {
    super(application);
    this.name = "bim_delta";
    this.label = "bim|tool.bim_delta.label";
    this.className = "bim_delta";
    this.decimals = 6;
    this.setOptions(options);
    application.addTool(this);

    ModelSnapshot.decimals = this.decimals;

    const fileExplorer = new FileExplorer(application);
    this.fileExplorer = fileExplorer;
    fileExplorer.title = "bim|title.bim_delta_snapshots";
    fileExplorer.group = "ifc_snapshots";

    application.panelManager.addPanel(fileExplorer);

    const contextMenu = fileExplorer.contextMenu;
    const action = fileExplorer.createContextAction;

    contextMenu.addMenuItem(action(CompareSnapshotAction), "default");
    contextMenu.addMenuItem(action(SaveSnapshotAction), "save");

    fileExplorer.openFile = (url, data) => this.openFile(url, data);
    fileExplorer.onClose = () => this.application.useTool(null);
  }

  activate()
  {
    this.fileExplorer.visible = true;
    if (this.fileExplorer.service === null)
    {
      this.fileExplorer.goHome();
    }
  }

  deactivate()
  {
    this.fileExplorer.visible = false;
  }
}

export { BIMDeltaTool };