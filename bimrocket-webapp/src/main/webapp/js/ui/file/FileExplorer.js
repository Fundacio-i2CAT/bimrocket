/**
 * FileExplorer.js
 *
 * @author realor
 */

import { Panel } from "../Panel.js";
import { Controls } from "../Controls.js";
import { Action } from "../Action.js";
import { ContextMenu, MenuItem } from "../Menu.js";
import { LoginDialog } from "../LoginDialog.js";
import { MessageDialog } from "../MessageDialog.js";
import { ConfirmDialog } from "../ConfirmDialog.js";
import { Toast } from "../Toast.js";
import { ServiceManager } from "../../io/ServiceManager.js";
import { FileService, Metadata, Result, ACL } from "../../io/FileService.js";
import { IOManager } from "../../io/IOManager.js";
import { WebUtils } from "../../utils/WebUtils.js";
import { I18N } from "../../i18n/I18N.js";
import { FileAction } from "./FileAction.js";
import { AddServiceAction } from "./AddServiceAction.js";
import { CreateFolderAction } from "./CreateFolderAction.js";
import { RenameFileAction } from "./RenameFileAction.js";
import { DeleteFileAction } from "./DeleteFileAction.js";
import { DownloadFileAction } from "./DownloadFileAction.js";
import { EditACLAction } from "./EditACLAction.js";
import { EditServiceAction } from "./EditServiceAction.js";
import { OpenFolderAction } from "./OpenFolderAction.js";
import { UploadFileAction } from "./UploadFileAction.js";

class FileExplorer extends Panel
{
  constructor(application, addContextActions = true)
  {
    super(application);
    this.id = "file_explorer";
    this.title = "title.file_explorer";
    this.position = "left";
    this.group = "model"; // service group
    this.minimumHeight = 200;

    /** @type {FileService} */
    this.service = null; // current service
    this.basePath = "/";
    this.selectedEntry = null;
    this._nextEntryName = null;

    this.showFileSize = true;

    this.serviceElem = document.createElement("div");
    this.serviceElem.className = "fileexplorer_panel";

    this.headerElem = document.createElement("div");
    this.headerElem.className = "header";

    this.homeButtonElem = Controls.addImageButton(this.headerElem,
      "home", "button.home", event => this.goHome(), "image_button home");

    this.backButtonElem = Controls.addImageButton(this.headerElem,
      "back", "button.back", event => this.goBack(), "image_button back");

    this.directoryElem = document.createElement("div");
    this.directoryElem.className = "directory";

    this.entriesElem = document.createElement("ul");
    this.entriesElem.className = "path_entries";

    this.footerElem = document.createElement("div");
    this.footerElem.className = "footer";

    this.buttonsPanelElem = document.createElement("div");
    this.buttonsPanelElem.className = "buttons_panel";

    this.bodyElem.appendChild(this.serviceElem);

    this.serviceElem.appendChild(this.headerElem);
    this.serviceElem.appendChild(this.entriesElem);
    this.serviceElem.appendChild(this.footerElem);

    this.headerElem.appendChild(this.homeButtonElem);
    this.headerElem.appendChild(this.backButtonElem);
    this.headerElem.appendChild(this.directoryElem);

    this.footerElem.appendChild(this.buttonsPanelElem);
    this.showButtonsPanel();

    this.contextMenu = new ContextMenu(this.application);
    this.createContextAction = (contextActionClass, options) => {
      return new contextActionClass(this, options);
    };

    if (addContextActions)
    {
      this.addContextActions();
    }

    this.entriesElem.addEventListener("click", event =>
    {
      this.onClick(event);
    });

    this.entriesElem.addEventListener("dblclick", event =>
    {
      this.onDoubleClick(event);
    });

    this.entriesElem.addEventListener("contextmenu", event =>
    {
      this.onContextMenu(event);
    });
  }

  addContextActions()
  {
    const contextMenu = this.contextMenu;
    const action = this.createContextAction;

    contextMenu.addSeparator("default");
    contextMenu.addMenuItem(action(OpenFolderAction));

    contextMenu.addSeparator("edit");

    contextMenu.addSeparator("general");
    contextMenu.addMenuItem(action(RenameFileAction));
    contextMenu.addMenuItem(action(DeleteFileAction));

    contextMenu.addSeparator("acl");
    contextMenu.addMenuItem(action(EditACLAction));

    contextMenu.addSeparator("upload_download");
    contextMenu.addMenuItem(action(UploadFileAction));
    contextMenu.addMenuItem(action(DownloadFileAction));

    contextMenu.addSeparator("services");
    contextMenu.addMenuItem(action(EditServiceAction));
    contextMenu.addMenuItem(action(AddServiceAction));

    contextMenu.addSeparator("create");
    contextMenu.addMenuItem(action(CreateFolderAction));
    contextMenu.addMenu("menu.file.create");

    contextMenu.addSeparator("save");
  }

  isServiceList()
  {
    return this.service === null;
  }

  isDirectoryList()
  {
    return this.service !== null;
  }

  isEntrySelected()
  {
    return this.selectedEntry !== null;
  }

  isServiceEntrySelected()
  {
    return this.selectedEntry?.type === 0;
  }

  isFileEntrySelected()
  {
    return this.selectedEntry?.type === Metadata.FILE;
  }

  isCollectionEntrySelected()
  {
    return this.selectedEntry?.type === Metadata.COLLECTION;
  }

  getBasePathName()
  {
    const service = this.service;
    const basePath = this.basePath;
    const serviceName = service.description || service.name;

    if (basePath === "/") // service home
    {
      return serviceName;
    }
    else
    {
      return serviceName + basePath;
    }
  }

  getSelectedPath()
  {
    if (this.selectedEntry)
    {
      return this.getAbsolutePath(this.selectedEntry.name);
    }
    return null;
  }

  getSelectedFileExtension()
  {
    if (this.isFileEntrySelected())
    {
      const entryName = this.selectedEntry.name;
      const index = entryName.lastIndexOf(".");
      if (index !== -1)
      {
        return entryName.substring(index + 1).toLowerCase();
      }
    }
    return null;
  }

  goHome()
  {
    this.selectedEntry = null;
    this.service = null;
    this.refreshServices();
  };

  goBack()
  {
    if (this.service === null) return;

    this.selectedEntry = null;

    if (this.basePath === "/")
    {
      this.service = null;
      this.refreshServices();
    }
    else
    {
      const index = this.basePath.lastIndexOf("/");
      if (index > 0)
      {
        this.basePath = this.basePath.substring(0, index);
      }
      else
      {
        this.basePath = "/";
      }
      this.list();
    }
  }

  list(onSuccess)
  {
    this.showProgressBar("Reading directory...");
    this.service.find(this.basePath, null,
      result => this.handleOpenResult("", result, onSuccess),
      data => this.setProgress(data.progress, data.message));
  }

  open(onSuccess)
  {
    const entryName = this.selectedEntry?.name;
    if (!entryName) throw "No entry selected.";

    if (this.service === null)
    {
      this.service = this.application.services[this.group][entryName];
      this.basePath = "/";
      this.list(onSuccess);
    }
    else
    {
      const path = this.getFullPath(entryName);
      if (this.selectedEntry.type === Metadata.COLLECTION)
      {
        this.showProgressBar("Reading directory...");
        this.service.find(path, null,
          result => this.handleOpenResult(entryName, result, onSuccess),
          data => this.setProgress(data.progress, data.message));
      }
      else
      {
        this.showProgressBar("Reading file...");
        this.service.read(path,
          result => this.handleOpenResult(entryName, result, onSuccess),
          data => this.setProgress(data.progress, data.message));
      }
    }
  }

  save(entryName, data, onSuccess)
  {
    this._nextEntryName = entryName;
    const path = this.getFullPath(entryName);
    this.showProgressBar("Saving...");
    this.service.write(path, data,
      result => this.handleSaveResult(entryName, data, result, onSuccess),
      data => this.setProgress(data.progress, data.message));
  }

  remove(onSuccess)
  {
    const entryName = this.selectedEntry?.name;
    if (!entryName) throw "No entry selected.";

    const path = this.getFullPath(entryName);
    this.showProgressBar("Deleting...");
    this.service.remove(path,
      result => this.handleRemoveResult(entryName, result, onSuccess));
  }

  makeFolder(entryName, onSuccess)
  {
    this._nextEntryName = entryName;
    const path = this.getFullPath(entryName);
    this.showProgressBar("Creating folder...");
    this.service.makeCollection(path,
      result => this.handleMakeFolderResult(entryName, result, onSuccess));
  }

  rename(newEntryName, onSuccess)
  {
    const entryName = this.selectedEntry?.name;
    if (!entryName) throw "No entry selected.";

    this._nextEntryName = newEntryName;
    const sourcePath = this.getFullPath(entryName);
    const destinationPath = this.getFullPath(newEntryName);

    this.showProgressBar("Renaming...");
    this.service.move(sourcePath, destinationPath,
      result => this.handleRenameResult(entryName, newEntryName,
        result, onSuccess));
  }

  download(onSuccess)
  {
    const entryName = this.selectedEntry?.name;
    if (!entryName) throw "No entry selected.";

    const path = this.getFullPath(entryName);
    this.showProgressBar("Downloading file...");
    this.service.read(path,
      result => this.handleDownloadResult(entryName, result, onSuccess),
      data => this.setProgress(data.progress));
  }

  upload(file, onSuccess)
  {
    const application = this.application;

    let reader = new FileReader();
    reader.onload = event =>
    {
      const data = event.target.result;
      const path = this.getFullPath(file.name);
      this._nextEntryName = file.name;
      this.showProgressBar("Uploading file...");
      this.service.write(path, data, result =>
      {
        this.handleUploadResult(file, result, onSuccess);
      },
      data => this.setProgress(data.progress));
    };

    let formatInfo = IOManager.getFormatInfo(file.name);
    if (formatInfo?.dataType === "text")
    {
      reader.readAsText(file);
    }
    else
    {
      reader.readAsArrayBuffer(file);
    }
  }

  confirmSave(entryName)
  {
    return new Promise(async resolve =>
    {
      const exists = await this.exists(entryName);
      if (exists)
      {
        ConfirmDialog.create("title.overwrite", "question.overwrite_file", entryName)
          .setAcceptLabel("button.yes")
          .setCancelLabel("button.no")
          .setAcceptAction(() => resolve(true))
          .setCancelAction(() => resolve(false))
          .setI18N(this.application.i18n)
          .show();
      }
      else resolve(true);
    });
  }

  exists(entryName, onSuccess)
  {
    return new Promise(resolve =>
    {
      const path = this.getFullPath(entryName);
      this.service.find(path, { depth : "0" }, result =>
      {
        const exists = result?.status !== Result.NOT_FOUND;
        resolve(exists);

        onSuccess?.(exists);
      });
    });
  }

  handleOpenResult(entryName, result, onSuccess)
  {
    this.showButtonsPanel();

    if (result.status === Result.OK)
    {
      if (result.entries)
      {
        this.selectedEntry = null;
        this.showDirectory(entryName, result);
      }

      onSuccess?.(this.getAbsolutePath(entryName), result);
    }
    else
    {
      this.handleError(result, false,
        () => {
                if (entryName === "") this.list(onSuccess);
                else this.open(onSuccess);
              },
        () => { if (entryName === "") this.service = null; });
    }
  }

  handleSaveResult(entryName, data, result, onSuccess)
  {
    const application = this.application;
    this.showButtonsPanel();

    if (result.status === Result.OK)
    {
      Toast.create("message.file_saved")
        .setI18N(application.i18n).show();

      this.selectedEntry = null;

      onSuccess?.(this.getAbsolutePath(entryName), result);

      this.list();
    }
    else
    {
      this.handleError(result, true,
        () => this.save(entryName, data, onSuccess));
    }
  }

  handleRemoveResult(entryName, result, onSuccess)
  {
    const application = this.application;
    this.showButtonsPanel();

    if (result.status === Result.OK)
    {
      if (this.selectedEntry.type === Metadata.COLLECTION)
      {
        Toast.create("message.folder_deleted")
          .setI18N(application.i18n).show();
      }
      else
      {
        Toast.create("message.file_deleted")
          .setI18N(application.i18n).show();
      }
      this.selectedEntry = null;

      onSuccess?.(this.getAbsolutePath(entryName), result);

      this.list();
    }
    else
    {
      this.handleError(result, true, () => this.remove(onSuccess));
    }
  }

  handleMakeFolderResult(entryName, result, onSuccess)
  {
    const application = this.application;
    this.showButtonsPanel();

    if (result.status === Result.OK)
    {
      onSuccess?.(this.getAbsolutePath(entryName), result);

      Toast.create("message.folder_created")
        .setI18N(application.i18n).show();

      this.selectedEntry = null;

      this.list();
    }
    else
    {
      this.handleError(result, true, () => this.makeFolder(entryName, onSuccess));
    }
  }

  handleRenameResult(entryName, newEntryName, result, onSuccess)
  {
    const application = this.application;
    this.showButtonsPanel();

    if (result.status === Result.OK)
    {
      onSuccess?.(this.getAbsolutePath(entryName), result);

      Toast.create("message.renaming_completed")
        .setI18N(application.i18n).show();

      this.selectedEntry = null;

      this.list();
    }
    else
    {
      this.handleError(result, true,
        () => this.rename(newEntryName, onSuccess));
    }
  }

  handleDownloadResult(entryName, result, onSuccess)
  {
    this.showButtonsPanel();

    if (result.status === Result.OK)
    {
      onSuccess?.(this.getAbsolutePath(entryName), result);

      const data = result.data;
      WebUtils.downloadFile(data, entryName);
    }
    else
    {
      this.handleError(result, false, () => this.download(onSuccess));
    }
  }

  handleUploadResult(file, result, onSuccess)
  {
    const application = this.application;
    this.showButtonsPanel();

    if (result.status === Result.OK)
    {
      onSuccess?.(this.getAbsolutePath(file.name), result);

      Toast.create("message.file_saved").setI18N(application.i18n).show();

      this.list();
    }
    else
    {
      this.handleError(result, true, () => this.upload(file, onSuccess));
    }
  }

  refreshServices()
  {
    const application = this.application;

    this.basePath = "/";
    const COLLECTION = Metadata.COLLECTION;

    this.directoryElem.textContent = "/";
    this.entriesElem.innerHTML = "";
    let firstLink = null;
    for (let serviceName in application.services[this.group])
    {
      let service = application.services[this.group][serviceName];
      let entryElem = document.createElement("li");
      entryElem.className = "entry service";
      entryElem.entry = new Metadata(service.name, service.description, 0);
      let linkElem = document.createElement("a");
      linkElem.href = "#";
      linkElem.textContent = service.description || service.name;
      entryElem.appendChild(linkElem);
      if (firstLink === null) firstLink = linkElem;
      this.entriesElem.appendChild(entryElem);
    }
    this.highlight();
    this.updateButtons();
    if (firstLink) firstLink.focus();
  }

  showDirectory(entryName, result)
  {
    const application = this.application;

    this.basePath = this.getFullPath(entryName);
    const FILE = Metadata.FILE;
    this.directoryElem.textContent = this.getBasePathName();
    let entries = result.entries;
    entries.sort(this.entryComparator);
    this.entriesElem.innerHTML = "";
    let firstLink = null;
    for (let entry of entries)
    {
      let entryElem = document.createElement("li");
      entryElem.classList.add("entry");
      if (entry.type === FILE)
      {
        entryElem.classList.add("file");
        let formatInfo = IOManager.getFormatInfo(entry.name);
        if (formatInfo?.icon)
        {
          entryElem.classList.add(formatInfo.icon);
        }
      }
      else
      {
        entryElem.classList.add("collection");
      }
      entryElem.entry = entry;
      let linkElem = document.createElement("a");
      linkElem.href= "#";
      let label = entry.description;
      let size = entry.size;
      if (entry.type === FILE && size > 0 && this.showFileSize)
      {
        label += " (";
        if (size > 1000000) label += (size / 1000000).toFixed(0) + "&nbsp;Mb";
        else if (size > 1000) label += (size / 1000).toFixed(0) + "&nbsp;Kb";
        else label += "1&nbsp;Kb";
        label += ")";
      }
      linkElem.innerHTML = label;
      entryElem.appendChild(linkElem);
      if (firstLink === null) firstLink = linkElem;
      this.entriesElem.appendChild(entryElem);
      if (entry.name === this._nextEntryName)
      {
        this.selectedEntry = entry;
      }
    }
    if (entryName)
    {
      if (firstLink) firstLink.focus();
    }
    else
    {
      this.highlight(true);
    }
    this.updateButtons();
    this._nextEntryName = null;
  }

  highlight(center = false)
  {
    const entriesElem = this.entriesElem;
    const entryName = this.selectedEntry?.name || "";
    for (let childNode of entriesElem.childNodes)
    {
      if (childNode.nodeName === "LI")
      {
        if (childNode.entry.name === entryName)
        {
          childNode.classList.add("selected");
          childNode.focus();
          if (center)
          {
            childNode.scrollIntoView(
            {
               behavior: "smooth",
               block: "center"
            });
          }
        }
        else
        {
          childNode.classList.remove("selected");
        }
      }
    }
  }

  onClick(event)
  {
    event.preventDefault();
    if (this.application.progressBar.visible) return;

    const entryElem = event.target.parentElement;
    this.selectedEntry = entryElem?.entry || null;
    this.highlight();
    this.updateButtons();
  }

  onDoubleClick(event)
  {
    event.preventDefault();
    if (this.application.progressBar.visible) return;

    const contextMenu = this.contextMenu;
    for (let menuItem of contextMenu.menuItems)
    {
      if (menuItem instanceof MenuItem)
      {
        let action = menuItem.action; // FileAction
        if (action.isEnabled() && action.isDefaultAction?.())
        {
          action.perform();
          break;
        }
      }
    }
  }

  onContextMenu(event)
  {
    event.preventDefault();

    if (this.application.progressBar.visible) return;

    const entryElem = event.target.parentElement;
    this.selectedEntry = entryElem?.entry || null;
    this.highlight();
    this.contextMenu.show(event);
  }

  updateButtons()
  {
  }

  showButtonsPanel()
  {
    this.buttonsPanelElem.style.display = "block";
    this.application.progressBar.visible = false;
  }

  showProgressBar(message = "")
  {
    this.buttonsPanelElem.style.display = "none";
    this.application.progressBar.message = message;
    this.application.progressBar.progress = undefined;
    this.application.progressBar.visible = true;
  }

  setProgress(progress, message)
  {
    this.application.progressBar.progress = progress;
    if (message)
    {
      this.application.progressBar.message = message;
    }
  }

  entryComparator(a, b)
  {
    const COLLECTION = Metadata.COLLECTION;
    const FILE = Metadata.FILE;

    if (a.type === COLLECTION && b.type === FILE) return -1;
    if (a.type === FILE && b.type === COLLECTION) return 1;
    if (a.name < b.name) return -1;
    if (a.name > b.name) return 1;
    return 0;
  }

  handleError(result, isWriteAction, onLogin, onFailed)
  {
    if (result.status === Result.INVALID_CREDENTIALS)
    {
      this.requestCredentials("message.invalid_credentials",
        onLogin, onFailed);
    }
    else if (result.status === Result.FORBIDDEN)
    {
      this.requestCredentials(isWriteAction ?
        "message.action_denied" : "message.access_denied",
        onLogin, onFailed);
    }
    else
    {
      if (onFailed) onFailed();

      MessageDialog.create("ERROR", result.message)
        .setClassName("error")
        .setI18N(this.application.i18n).show();
    }
  }

  requestCredentials(message, onLogin, onFailed)
  {
    const loginDialog = new LoginDialog(this.application, message);
    loginDialog.login = (username, password) =>
    {
      this.service.setCredentials(username, password);
      if (onLogin) onLogin();
    };
    loginDialog.onCancel = () =>
    {
      loginDialog.hide();
      if (onFailed) onFailed();
    };
    loginDialog.show();
  }

  addProxyFields(dialog, service)
  {
    dialog.useProxyElem = dialog.addCheckBoxField("useProxy",
      "label.use_proxy", service?.useProxy === true);
  }

  setServiceParameters(dialog, service, parameters)
  {
    parameters.useProxy = dialog.useProxyElem.checked;

    service.setParameters(parameters);
    this.application.addService(service, this.group);
    this.refreshServices();
  }

  getFullPath(name)
  {
    if (name)
    {
      if (this.basePath.endsWith("/")) return this.basePath + name;
      else return this.basePath + "/" + name;
    }
    return this.basePath;
  }

  getAbsolutePath(name)
  {
    return this.service.url + this.getFullPath(name);
  }
};

export { FileExplorer };
