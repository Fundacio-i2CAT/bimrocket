/*
 * WebdavService.js
 *
 * @author realor
 */

import { IOManager } from "./IOManager.js";
import { FileService, Metadata, Result, ACL } from "./FileService.js";
import { ServiceManager } from "./ServiceManager.js";
import { WebUtils } from "../utils/WebUtils.js";

const OK = Result.OK;
const ERROR = Result.ERROR;
const INVALID_CREDENTIALS = Result.INVALID_CREDENTIALS;
const FORBIDDEN = Result.INVALID_CREDENTIALS;
const BAD_REQUEST = Result.BAD_REQUEST;
const NOT_FOUND = Result.NOT_FOUND;

const COLLECTION = Metadata.COLLECTION;
const FILE = Metadata.FILE;

class WebdavService extends FileService
{
  static PROXY_URI = "/bimrocket-server/api/proxy?url=";

  static roleToPrincipal =
  {
    "EVERYONE": { type: "all" },
    "AUTHENTICATED": { type: "authenticated" }
  };

  static xmlTagToPrivilege =
  {
    "read": "READ",
    "write": "WRITE",
    "read-acl": "READ_ACL",
    "write-acl": "WRITE_ACL"
  };

  static privilegeToXmlTag = {};

  static
  {
    for (let tag in this.xmlTagToPrivilege)
    {
      let privilege = this.xmlTagToPrivilege[tag];
      this.privilegeToXmlTag[privilege] = tag;
    }
  }

  constructor(parameters)
  {
    super(parameters);
  }

  getParameters()
  {
    const parameters = super.getParameters();
    parameters.useProxy = this.useProxy;
    return parameters;
  }

  setParameters(parameters)
  {
    super.setParameters(parameters);
    this.useProxy = parameters.useProxy || false;
  }

  find(path, options, onCompleted)
  {
    let url = this.getUrl(path);

    let baseUri = url;
    let index = baseUri.indexOf("://");
    if (index !== -1)
    {
      baseUri = baseUri.substring(index + 3);
      index = baseUri.indexOf("/");
      if (index !== -1)
      {
        baseUri = baseUri.substring(index);
        if (baseUri.lastIndexOf("/") !== baseUri.length - 1)
        {
          baseUri += "/";
        }
      }
    }

    // **** HTTP PROPFIND REQUEST ****

    let request = new XMLHttpRequest();
    request.onerror = () =>
    {
      // ERROR
      onCompleted(new Result(ERROR, "Connection error"));
    };
    request.onload = () =>
    {
      if (request.status === 200 || request.status === 207)
      {
        try
        {
          // OK
          let xml = request.responseXML;
          let multiNode = xml.childNodes[0];
          let responseNodes = multiNode.childNodes;
          let metadata = new Metadata();
          let entries = null;
          for (let responseNode of responseNodes)
          {
            if (responseNode.localName === "response")
            {
              let hrefNode = responseNode.querySelector("href");
              let hrefValue = hrefNode.textContent;

              if (hrefValue.startsWith("http:") ||
                  hrefValue.startsWith("https:"))
              {
                let resUrl = new URL(hrefValue);
                hrefValue = resUrl.pathname;
              }

              if (hrefValue.endsWith("/"))
                hrefValue = hrefValue.substring(0, hrefValue.length - 1);

              hrefValue = decodeURI(hrefValue);

              let fileName = hrefValue.substring(baseUri.length);
              let isCollectionNode = responseNode.querySelector(
                "propstat prop resourcetype collection") !== null;
              let contentLengthNode = responseNode.querySelector(
                "propstat prop getcontentlength");
              let lastModifiedNode = responseNode.querySelector(
                "propstat prop getlastmodified");

              let fileSize = contentLengthNode ?
                parseInt(contentLengthNode.textContent) : 0;
              let lastModified = lastModifiedNode ? // HTTP-Date
                Date.parse(lastModifiedNode.textContent) : 0;

              if (fileName.indexOf("/") === 0) fileName = fileName.substring(1);

              if (fileName.length === 0) // filename is the requested resource
              {
                let index = hrefValue.lastIndexOf("/");
                metadata.name = hrefValue.substring(index + 1);
                metadata.description = metadata.name;
                metadata.type = isCollectionNode ? COLLECTION : FILE;
                metadata.size = fileSize;
                metadata.lastModified = lastModified;
              }
              else // filename is a child resource
              {
                if (!entries) entries = [];
                let entry = new Metadata(fileName, fileName,
                  isCollectionNode ? COLLECTION : FILE, fileSize, lastModified);
                entries.push(entry);
              }
            }
          }

          onCompleted(new Result(OK, "", path, metadata, entries, null));
        }
        catch (ex)
        {
          onCompleted(new Result(ERROR, ex));
        }
      }
      else
      {
        onCompleted(this.createError("Can't open", request.status));
      }
    };
    this.openRequest("PROPFIND", url, request);
    request.setRequestHeader("depth", options?.depth || "1");
    request.send();
  }

  read(path, onCompleted, onProgress)
  {
    let url = this.getUrl(path);
    let metadata = new Metadata();
    let index = path.lastIndexOf("/");
    metadata.name = index === -1 ? path : path.substring(index + 1);
    metadata.type = FILE;

    let request = new XMLHttpRequest();
    let formatInfo = IOManager.getFormatInfo(url);
    let dataType = formatInfo?.dataType || "arraybuffer";
    request.responseType = dataType;

    request.onload = () =>
    {
      if (request.status === 200)
      {
        if (onProgress)
        {
          onProgress({ progress : 100, message : "Download completed." });
        }
        metadata.size = parseInt(request.getResponseHeader("Content-Length"));

        onCompleted(new Result(OK, "", path, metadata, null, request.response));
      }
      else
      {
        onCompleted(this.createError("Read failed", request.status));
      }
    };
    request.onerror = error =>
    {
      onCompleted(new Result(ERROR, error));
    };
    if (onProgress)
    {
      request.onprogress = event =>
      {
        let progress = Math.round(
          100 * event.loaded / event.total);
        let message = "Downloading file...";
        onProgress({ progress : progress, message : message });
      };
    }
    this.openRequest("GET", url, request);
    request.send();
  }

  write(path, data, onCompleted, onProgress)
  {
    const url = this.getUrl(path);
    const request = new XMLHttpRequest();
    request.onerror = error =>
    {
      // ERROR
      onCompleted(new Result(ERROR, "Connection error"));
    };
    request.onload = () =>
    {
      if (request.status === 200 || request.status === 201)
      {
        onCompleted(new Result(OK));
      }
      else
      {
        onCompleted(this.createError("Write failed", request.status));
      }
    };
    if (onProgress)
    {
      request.onprogress = event =>
      {
        let progress = Math.round(
          100 * event.loaded / event.total);
        let message = "Uploading file...";
        onProgress({ progress : progress, message : message });
      };
    }
    this.openRequest("PUT", url, request);
    request.send(data);
  }

  remove(path, onCompleted, onProgress)
  {
    const url = this.getUrl(path);
    const request = new XMLHttpRequest();
    request.onerror = error =>
    {
      // ERROR
      onCompleted(new Result(ERROR, "Connection error"));
    };
    request.onload = () =>
    {
      if (request.status === 200 || request.status === 204)
      {
        onCompleted(new Result(OK));
      }
      else
      {
        onCompleted(this.createError("Remove failed", request.status));
      }
    };
    this.openRequest("DELETE", url, request);
    request.send();
  }

  makeCollection(path, onCompleted, onProgress)
  {
    const url = this.getUrl(path);
    const request = new XMLHttpRequest();
    request.onerror = error =>
    {
      onCompleted(new Result(ERROR, "Connection error"));
    };
    request.onload = () =>
    {
      if (request.status === 200 || request.status === 201)
      {
        onCompleted(new Result(OK));
      }
      else
      {
        onCompleted(this.createError("Folder creation failed", request.status));
      }
    };
    this.openRequest("MKCOL", url, request);
    request.send();
  }

  move(sourcePath, destinationPath, onCompleted)
  {
    const url = this.getUrl(sourcePath);
    const request = new XMLHttpRequest();
    request.onerror = error =>
    {
      onCompleted(new Result(ERROR, "Connection error"));
    };
    request.onload = () =>
    {
      if (request.status === 200 || request.status === 201)
      {
        onCompleted(new Result(OK));
      }
      else
      {
        onCompleted(this.createError("Move operation failed", request.status));
      }
    };
    this.openRequest("MOVE", url, request);
    let destination = this.url + encodeURIComponent(destinationPath);
    request.setRequestHeader("Destination", destination);
    request.send();
  }

  copy(sourcePath, destinationPath, onCompleted, onProgress)
  {
    const url = this.getUrl(sourcePath);
    const request = new XMLHttpRequest();
    request.onerror = error =>
    {
      onCompleted(new Result(ERROR, "Connection error"));
    };
    request.onload = () =>
    {
      if (request.status === 200 || request.status === 201)
      {
        onCompleted(new Result(OK));
      }
      else
      {
        onCompleted(this.createError("Copy operation failed", request.status));
      }
    };
    this.openRequest("COPY", url, request);
    request.setRequestHeader("Destination", this.url + destinationPath);
    request.send();
  }

  getACL(path, onCompleted)
  {
    try
    {
      const url = this.getUrl(path);
      const request = new XMLHttpRequest();

      request.onerror = () => onCompleted(new Result(ERROR, "Connection error"));
      request.onload = () =>
      {
        if (request.status === 200 || request.status === 207)
        {
          try
          {
            const acl = this.convertXMLToACL(request.response);
            onCompleted(new Result(OK, "ACL read", path, null, null, acl));
          }
          catch (error)
          {
            onCompleted(new Result(ERROR, `Failed to parse ACL: ${error}`));
          }
        }
        else
        {
          onCompleted(this.createError("ACL retrieval failed", request.status));
        }
      };

      this.openRequest("PROPFIND", url, request);
      request.setRequestHeader("Depth", "0");
      request.setRequestHeader("Content-Type", "application/xml; charset=utf-8");
      request.send(
        '<?xml version="1.0" encoding="utf-8" ?>' +
        '<d:propfind xmlns:d="DAV:">' +
        '  <d:prop>' +
        '    <d:acl/>' +
        '    <d:owner/>' +
        '  </d:prop>' +
        '</d:propfind>'
      );
    }
    catch (error)
    {
      onCompleted(new Result(ERROR, `ACL request failed: ${error}`, path));
    }
  }

  setACL(path, acl, onCompleted)
  {
    try
    {
      const aclXML = this.convertACLToXML(acl);
      const url = this.getUrl(path);
      const request = new XMLHttpRequest();

      request.onerror = () => onCompleted(new Result(ERROR, "Connection error"));
      request.onload = () =>
      {
        if (request.status === 200 || request.status === 201)
        {
          onCompleted(new Result(OK));
        }
        else
        {
          onCompleted(this.createError("ACL change failed", request.status));
        }
      };

      this.openRequest("ACL", url, request);
      request.setRequestHeader("Content-Type", "application/xml; charset=utf-8");
      request.send(aclXML);
    }
    catch (error)
    {
      onCompleted(new Result(ERROR, `ACL change failed: ${error}`, path));
    }
  }

  /* internal methods */

  openRequest(method, url, request)
  {
    if (this.useProxy)
    {
      url = WebdavService.PROXY_URI + url;
    }
    request.open(method, encodeURI(url), true);
    request.setRequestHeader("X-Requested-With", "XMLHttpRequest");

    const credentials = this.getCredentials();

    if (this.useProxy)
    {
      if (credentials.username && credentials.password)
      {
        WebUtils.setBasicAuthorization(request,
          credentials.username, credentials.password, "Forwarded-Authorization");
      }
    }
    else
    {
      WebUtils.setBasicAuthorization(request,
        credentials.username, credentials.password);
    }
  }

  createError(message, status)
  {
    let statusMessage = WebUtils.getHttpStatusMessage(status);
    if (statusMessage.length > 0)
    {
      message += ": " + statusMessage;
    }
    message += " (HTTP " + status + ").";

    let resultStatus;
    switch (status)
    {
      case 400: resultStatus = BAD_REQUEST; break;
      case 401: resultStatus = INVALID_CREDENTIALS; break;
      case 403: resultStatus = FORBIDDEN; break;
      case 404: resultStatus = NOT_FOUND; break;
      default: resultStatus = ERROR;
    }
    return new Result(resultStatus, message);
  }

  getUrl(path)
  {
    let url = this.url || "";

    if (url && url.endsWith("/"))
    {
      url = url.substring(0, url.length - 1);
    }

    if (path && !path.startsWith("/"))
    {
      path = "/" + path;
    }
    return url + path;
  }

  convertXMLToACL(xmlString)
  {
    const DAV_NS = "DAV:";
    const parser = new DOMParser();
    const xmlDoc = parser.parseFromString(xmlString, "application/xml");

    const acl = new ACL();

    const aceElements = xmlDoc.getElementsByTagNameNS(DAV_NS, "ace");

    for (const aceElement of aceElements)
    {
      let role = null;

      const hrefElement = aceElement.getElementsByTagNameNS(DAV_NS, "href")[0];
      if (hrefElement)
      {
        role = hrefElement.textContent.trim();
      }
      else if (aceElement.getElementsByTagNameNS(DAV_NS, "all")[0])
      {
        role = "EVERYONE";
      }
      else if (aceElement.getElementsByTagNameNS(DAV_NS, "authenticated")[0])
      {
        role = "AUTHENTICATED";
      }
      if (!role) continue;

      const privilegeElements = aceElement.getElementsByTagNameNS(DAV_NS, "privilege");

      for (const privilegeElement of privilegeElements)
      {
        const privilegeNode = privilegeElement.firstElementChild;
        if (privilegeNode)
        {
          let privilegeTag = privilegeNode.localName;
          let privilege = this.mapXmlTagToPrivilege(privilegeTag);
          acl.grant(role, privilege);
        }
      }
    }
    return acl;
  }

  convertACLToXML(acl)
  {
    const roleToPrincipal = this.constructor.roleToPrincipal;

    let xml = `<?xml version="1.0" encoding="utf-8" ?>\n<D:acl xmlns:D="DAV:">\n`;

    for (const role in acl.roles)
    {
      const privileges = acl.getPrivileges(role);
      if (privileges.length === 0) continue;

      const principal = roleToPrincipal[role] || { type: "href", value: role };

      xml += `  <D:ace>\n`;
      if (principal.type === "href")
      {
        xml += `    <D:principal><D:href>${principal.value}</D:href></D:principal>\n`;
      }
      else
      {
        xml += `    <D:principal><D:${principal.type}/></D:principal>\n`;
      }
      xml += `    <D:grant>\n`;

      for (let privilege of privileges)
      {
        let privilegeTag = this.mapPrivilegeToXmlTag(privilege);
        xml += `      <D:privilege><D:${privilegeTag}/></D:privilege>\n`;
      }
      xml += `    </D:grant>\n`;
      xml += `  </D:ace>\n`;
    }

    xml += `</D:acl>`;

    return xml;
  }

  mapXmlTagToPrivilege(privilegeTag)
  {
    const privilege = this.constructor.xmlTagToPrivilege[privilegeTag];
    if (!privilege) throw "Unsupported privilege " + privilegeTag;
    return privilege;
  }

  mapPrivilegeToXmlTag(privilege)
  {
    const privilegeTag = this.constructor.privilegeToXmlTag[privilege];
    if (!privilegeTag) throw "Unsupported privilege " + privilege;
    return privilegeTag;
  }
}

ServiceManager.addClass(WebdavService);

export { WebdavService };