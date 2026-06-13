/**
 * ServerSession.js
 *
 * @author realor
 */

import { SecurityService } from "./SecurityService.js";
import { Result } from "./FileService.js";
import { WebUtils } from "../utils/WebUtils.js";

/**
 * Represents a session of a specific type of server.
 * It also holds a registry of active server sessions.
 *
 * @class
 */
class ServerSession
{
  static sessions = new Map();
  static classes = {};

  constructor(baseUrl)
  {
    this.baseUrl = baseUrl;
  }

  /**
   * Log in to the server with the given username and password.
   *
   * @param {string} username - the username to log in with
   * @param {string} password - the password to log in with
   * @param {function} onCompleted - called when login completes
   * @param {function} onError - called when login fails
   */
  login(username, password, onCompleted, onError)
  {
    onCompleted?.();
  }

  /**
   * Closes the server session.
   *
   * @param {function} onCompleted - called when logout completes
   * @param {function} onError - called when logout fails
   */
  logout(onCompleted, onError)
  {
    onCompleted?.();
  }

  /**
   * Retrieves the OAuth2 providers supported by this server.
   *
   * @param {function} onCompleted - called when retrieval completes
   * @param {function} onError - called when retrieval fails
   */
  getOAuth2Providers(onCompleted, onError)
  {
    onCompleted?.([]);
  }

  /**
   * Gets the user who is currently logged into the server.
   *
   * @param {function} onCompleted - called when operation completes
   * @param {function} onError - called when operation completes
   */
  getUsername(onCompleted, onError)
  {
    onCompleted?.("anonymous");
  }

  /**
   * Sets the fetch options to make a request to this server.
   * This methods must inject the required fetch headers.
   *
   * @param {Object} fetchOptions - the options to use in the fetch operation
   */
  setFetchOptions(fetchOptions)
  {
  }

  /**
   * Creates a normalized error object from the http status and the 
   * server response.
   * 
   * @param {number} status - the http status
   * @param {Object} detail - an object that represents the body of the 
   *  server's response
   *
   * @returns {Object} the normalized error { code: number, message: string }
   */
  getError(status, detail)
  {
    const error = 
    {
      code : status,
      message : WebUtils.getHttpStatusMessage(status)
    };
    
    if (detail) error.detail = detail;
    
    return error;
  }

  /**
   * Determine the action to be taken based on the error that has occurred.
   *
   * @param {Object|Result} error - the error that has occurred
   * @param {function} retry - called when the error was fixed (token renewal)
   * @param {function} authenticate - called when user authentication is required
   * @param {function} showError - called to show this error to the user
   */
  handleError(error, retry, authenticate, showError)
  {
    showError?.();
  }

  /**
   * Gets the server base url from the given service url.
   *
   * @param {string} url - the service url
   * @returns {string} - the server base url.
   */
  static getBaseUrl(url)
  {
    return url;
  }

  /**
   * Gets the the session associated with the specified service.
   *
   * @param {Service} service
   * @param {boolean} create - when true, creates the session if it does not exists
   * @returns {ServerSession} - the session associated with the given service
   */
  static getSession(service, create = true)
  {
    const sessionClass = this.classes[service.serverType] ||
          BimrocketServerSession;
    const baseUrl = sessionClass.getBaseUrl(service.url);
    const key = sessionClass.serverType + ":" + baseUrl;

    let session = this.sessions.get(key);
    if (!session && create)
    {
      session = new sessionClass(baseUrl);
      this.sessions.set(key, session);
    }
    return session;
  }

  /**
   * Get all active server sessions.
   *
   * @returns {ServerSession[]} array of ServerSessions
   */
  static getSessions()
  {
    return [...this.sessions.values()];
  }

  /**
   * Adds a new ServerSession class.
   *
   * @param {class} cls - the ServerSession class
   */
  static addClass(cls)
  {
    this.classes[cls.serverType] = cls;
  }

  /**
   * Returns the name of all available ServerSession classes.
   *
   * @returns {string[]} array of ServerSession class names
   */
  static getClassNames()
  {
    return Object.keys(this.classes);
  }
}

/**
 * ServerSession for Bimrocket servers.
 * It authenticates the user through the SecurityService.
 *
 * @class
 */
class BimrocketServerSession extends ServerSession
{
  static serverType = "bimrocket";

  constructor(baseUrl)
  {
    super(baseUrl);
    this.username = null;
    this.authorization = null;
  }

  login(username, password, onCompleted, onError)
  {
    const processResult = (result) =>
    {
      this.username = username;
      this.authorization = result.token ? "Bearer " + result.token : null;
      onCompleted?.();
    };

    this.getSecurityService().login(username, password, processResult, onError);
  }

  logout(onCompleted, onError)
  {
    const processResult = () =>
    {
      this.username = null;
      this.authorization = null;
      onCompleted?.();
    };

    this.getSecurityService().logout(processResult, onError);
  }

  getOAuth2Providers(onCompleted, onError)
  {
    const baseUrl = this.baseUrl;
    const processProviders = (providers) =>
    {
      providers.forEach(provider =>
        provider.authUrl = `${baseUrl}/api/security/oauth2/login/${provider.name}`);
      onCompleted?.(providers);
    };

    this.getSecurityService().getOAuth2Providers(processProviders, onError);
  }

  getUsername(onCompleted, onError)
  {
    onCompleted?.(this.username || "anonymous");
  }

  setFetchOptions(fetchOptions)
  {
    const headers = fetchOptions.headers;

    headers["X-Requested-With"] = "Fetch"; // always send Fetch.

    if (this.authorization)
    {
      // send Bearer authorization if present
      fetchOptions.credentials = "omit";
      headers["Authorization"] = this.authorization;
    }
    else
    {
      if (this.username)
      {
        // send session cookie
        fetchOptions.credentials = "include";
      }
    }
  }
  
  getError(status, detail)
  {
    // bimrocket already returns error detail in normalized form
    if (typeof detail.code === "number") return detail;
    
    return super.getError(status, detail);
  }

  handleError(error, retry, authenticate, showError)
  {
    if (error.code === 401 ||
        error.code === 403 ||
        error.status === Result.INVALID_CREDENTIALS ||
        error.status === Result.FORBIDDEN)
    {
      authenticate?.();
    }
    else
    {
      showError?.();
    }
  }

  getSecurityService()
  {
    return new SecurityService({ url: this.baseUrl + "/api/security" });
  }

  static getBaseUrl(url)
  {
		let index = url.indexOf("/api");
		let baseUrl = index !== -1 ? url.substring(0, index) : url;
    return baseUrl;
  }
}

/**
 *  ServerSession for generic servers
 *  It delegates authentication to the browser.
 *
 * @class
 */
class GenericServerSession extends ServerSession
{
  static serverType = "generic";

  constructor(baseUrl)
  {
    super(baseUrl);
  }
}

ServerSession.addClass(BimrocketServerSession);
ServerSession.addClass(GenericServerSession);

export { ServerSession };