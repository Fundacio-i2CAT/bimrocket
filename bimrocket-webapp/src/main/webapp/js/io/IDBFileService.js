/**
 * IDBFileService.js
 *
 * @author realor
 */

import { FileService, Metadata, Result } from "./FileService.js";
import { ServiceManager } from "./ServiceManager.js";

const OK = Result.OK;
const ERROR = Result.ERROR;
const INVALID_CREDENTIALS = Result.INVALID_CREDENTIALS;
const FORBIDDEN = Result.INVALID_CREDENTIALS;
const BAD_REQUEST = Result.BAD_REQUEST;
const NOT_FOUND = Result.NOT_FOUND;

const COLLECTION = Metadata.COLLECTION;
const FILE = Metadata.FILE;

class IDBFileService extends FileService
{
  constructor(parameters)
  {
    super(parameters);
  }

  setParameters(parameters)
  {
    super.setParameters(parameters);
    if (!this.url) this.url = "default";
  }

  find(path, options, onCompleted)
  {
    const readOperation = new ReadOperation(false);
    readOperation.execute(this.url, path, onCompleted);
  }

  read(path, onCompleted, onProgress)
  {
    const readOperation = new ReadOperation(true);
    readOperation.execute(this.url, path, onCompleted);
  }

  write(path, data, onCompleted, onProgress)
  {
    const writeOperation = new WriteOperation(data);
    writeOperation.execute(this.url, path, onCompleted);
  }

  remove(path, onCompleted, onProgress)
  {
    const removeOperation = new RemoveOperation();
    removeOperation.execute(this.url, path, onCompleted);
  }

  makeCollection(path, onCompleted, onProgress)
  {
    const mkColOperation = new MakeCollectionOperation();
    mkColOperation.execute(this.url, path, onCompleted);
  }

  move(sourcePath, destinationPath, onCompleted)
  {
    const srcIndex = sourcePath.lastIndexOf("/");
    let srcBase, srcName;
    if (srcIndex === -1)
    {
      srcBase = "";
      srcName = sourcePath;
    }
    else
    {
      srcBase = sourcePath.substring(0, srcIndex);
      srcName = sourcePath.substring(srcIndex + 1);
    }

    const destIndex = destinationPath.lastIndexOf("/");
    let destBase, destName;
    if (destIndex === -1)
    {
      destBase = "";
      destName = destinationPath;
    }
    else
    {
      destBase = destinationPath.substring(0, destIndex);
      destName = destinationPath.substring(destIndex + 1);
    }

    if (srcBase === destBase)
    {
      if (srcName !== destName)
      {
        const renameOperation = new RenameOperation(destName);
        renameOperation.execute(this.url, sourcePath, onCompleted);
      }
      else onCompleted(new Result(OK));
    }
    else
    {
      onCompleted(new Result(ERROR, "Unsupported operation."));
    }
  }

  copy(sourcePath, destinationPath, onCompleted, onProgress)
  {
    onCompleted(new Result(ERROR, "Not implemented."));
  }
}

class Operation
{
  constructor()
  {
    this.index = 0,
    this.nodeId = 0;
    this.nodes = [];
    this.pathArray = null;
    this.onCompleted = null;
  }

  execute(dbName, path, onCompleted)
  {
    if (!dbName) dbName = "default";

    if (path.startsWith("//")) path = path.substring(1);
    const pathArray = path === "/" ? [""] : path.split("/");

    this.pathArray = pathArray;
    this.onCompleted = onCompleted;

    const openReq = indexedDB.open(dbName, 1);

    // Create the schema
    openReq.onupgradeneeded = () =>
    {
      const db = openReq.result;
      const store = db.createObjectStore("nodes", { keyPath: "id" });

      // create root node
      const rootNode = {
        id : 0,
        entries : {},
        lastId : 0
      };
      store.put(rootNode);
    };

    openReq.onsuccess = () =>
    {
      const db = openReq.result;
      const tx = db.transaction("nodes", "readwrite");
      const store = tx.objectStore("nodes");
      this.store = store;

      tx.oncomplete = () =>
      {
        db.close();
      };

      tx.onerror = (event) =>
      {
        this.error(ERROR, event.error);
      };

      this.loadNextNode();
    };

    openReq.onerror = (event) =>
    {
      this.error(ERROR, event.error);
    };
  }

  loadNextNode()
  {
    const getReq = this.store.get(this.nodeId);

    getReq.onsuccess = () =>
    {
      const node = getReq.result;
      const pathArray = this.pathArray;
      const store = this.store;

      if (!node)
      {
        this.error(NOT_FOUND, "Invalid path.");
        return;
      }

      this.nodes.push(node);

      if (this.index === pathArray.length - 1)
      {
        this.onLastNode();
      }
      else if (node.entries)
      {
        this.index++;
        let name = pathArray[this.index];

        let entry = node.entries[name];
        if (entry)
        {
          this.nodeId = entry.id;
          this.loadNextNode();
        }
        else if (this.index === pathArray.length - 1)
        {
          this.onNewNode();
        }
        else
        {
          this.error(NOT_FOUND, "Not found.");
        }
      }
      else
      {
        this.error(NOT_FOUND, "Not found.");
      }
    };

    getReq.onerror = (event) =>
    {
      this.error(ERROR, event.error);
    };
  }

  createNodeId()
  {
    const store = this.store;
    const rootNode = this.nodes[0];
    rootNode.lastId++;
    store.put(rootNode);
    return rootNode.lastId;
  }

  onLastNode()
  {
    this.error(ERROR, "Invalid operation.");
  }

  onNewNode()
  {
    this.error(ERROR, "Invalid operation.");
  }

  error(status, msg)
  {
    this.onCompleted(new Result(status, msg));
  }

  getSize(data)
  {
    if (!data) return 0;
    return typeof data === "string" ? data.length : data.size;
  }
}

class ReadOperation extends Operation
{
  constructor(loadFileData = false)
  {
    super();
    this.loadFileData = loadFileData;
  }

  onNewNode()
  {
    this.error(NOT_FOUND, "Not found.");
  }

  async onLastNode()
  {
    let name = this.pathArray[this.index];
    let path = this.pathArray.join("/");
    if (path === "") path = "/";

    const node = this.nodes[this.index];
    const metadata = new Metadata();
    metadata.name = name;
    metadata.description = name;

    let entries = null;
    let data = null;

    if (node.entries) // dir
    {
      metadata.type = COLLECTION;
      metadata.size = 0;
      metadata.lastModified = 0;
      entries = [];
      for (let fileName in node.entries)
      {
        const entry = node.entries[fileName];
        const fileMetadata = new Metadata();
        fileMetadata.name = fileName;
        fileMetadata.description = fileName;
        fileMetadata.type = entry.type;
        fileMetadata.size = entry.size;
        fileMetadata.lastModified = entry.modified;
        entries.push(fileMetadata);
      }
    }
    else // file
    {
      const parentNode = this.nodes[this.index - 1];
      const entry = parentNode.entries[name];
      metadata.type = FILE;
      metadata.size = entry.size;
      metadata.lastModified = entry.modified;
      if (this.loadFileData)
      {
        const mimeType = node.data.type || "text/plain";
        if (mimeType === "text/plain")
        {
          data = await node.data.text();
        }
        else
        {
          data = await node.data.arrayBuffer();
        }
      }
    }
    this.onCompleted(new Result(OK, "", path, metadata, entries, data));
  }
}

class WriteOperation extends Operation
{
  constructor(data)
  {
    super();
    if (typeof data === "string")
    {
      data = new Blob([data], { type : "text/plain" });
    }
    else if (data instanceof ArrayBuffer)
    {
      data = new Blob([data], { type : "application/octet-stream" });
    }
    this.data = data; // always save data as blob
  }

  onLastNode()
  {
    // update file
    const node = this.nodes[this.index];
    if (node.data)
    {
      const parentNode = this.nodes[this.index - 1];
      const name = this.pathArray[this.index];
      const entry = parentNode.entries[name];
      entry.size = this.getSize(this.data);
      entry.modified = Date.now();
      this.store.put(parentNode);

      node.data = this.data;
      this.store.put(node);

      this.onCompleted(new Result(OK));
    }
    else // attempt to replace collection by file
    {
      this.error(BAD_REQUEST, "A directory with the same name already exists.");
    }
  }

  onNewNode()
  {
    // create new file
    const store = this.store;
    const parentNode = this.nodes[this.index - 1];
    const pathArray = this.pathArray;
    const name = pathArray[this.index];

    let nodeId = this.createNodeId();

    // register file in node entries
    parentNode.entries[name] = {
      id : nodeId,
      type : FILE,
      size : this.getSize(this.data),
      modified : Date.now()
    };
    store.put(parentNode);

    // create new file node
    const fileNode = {
      id: nodeId,
      data : this.data
    };
    this.store.put(fileNode);

    this.onCompleted(new Result(OK));
  }
}

class RemoveOperation extends Operation
{
  constructor()
  {
    super();
  }

  onLastNode()
  {
    const store = this.store;
    const nodes = this.nodes;
    const node = nodes[this.index];
    const parentNode = nodes[this.index - 1];

    const name = this.pathArray[this.index];
    const entry = parentNode.entries[name];
    if (entry.type === COLLECTION && Object.keys(node.entries).length > 0)
    {
      this.error(BAD_REQUEST, "Directory is not empty.");
    }
    else
    {
      delete parentNode.entries[name];

      store.put(parentNode);
      store.delete(node.id);

      this.onCompleted(new Result(OK));
    }
  }
}

class MakeCollectionOperation extends Operation
{
  constructor()
  {
    super();
  }

  onLastNode()
  {
    this.error(BAD_REQUEST, "A directory with the same name already exists.");
  }

  onNewNode()
  {
    const store = this.store;
    const parentNode = this.nodes[this.index - 1];
    const pathArray = this.pathArray;
    const name = pathArray[this.index];

    let nodeId = this.createNodeId();

    // register collection name in node entries
    parentNode.entries[name] = {
      id : nodeId,
      type : COLLECTION,
      size : 0,
      modified: Date.now()
    };
    store.put(parentNode);

    // create new collection node
    const colNode = {
      id: nodeId,
      entries: {}
    };
    this.store.put(colNode);
    this.onCompleted(new Result(OK));
  }
}

class RenameOperation extends Operation
{
  constructor(newName)
  {
    super();
    this.newName = newName;
  }

  onLastNode()
  {
    const newName = this.newName;
    let name = this.pathArray[this.index];
    if (name !== newName)
    {
      const parentNode = this.nodes[this.index - 1];
      if (parentNode.entries[newName])
      {
        // a resource already exists with newName
        this.onCompleted(new Result(ERROR, "Unsupported operation."));
        return;
      }
      let entry = parentNode.entries[name];
      delete parentNode.entries[name];
      parentNode.entries[newName] = entry;

      this.store.put(parentNode);
    }
    this.onCompleted(new Result(OK));
  }
}

ServiceManager.addClass(IDBFileService);

export { IDBFileService };