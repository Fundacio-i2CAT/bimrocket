/**
 * ErrorHandler.js
 *
 * @author realor
 */

import { ServerSession } from "../io/ServerSession.js";
import { Result } from "../io/FileService.js";
import { ServiceLoginDialog } from "./ServiceLoginDialog.js";
import { MessageDialog } from "./MessageDialog.js";

class ErrorHandler
{
  /**
   * Handles a ServiceError.
   *
   * @param {Application} application - the current application
   * @param {Service} service - the service that generated the error
   * @param {ServiceError} error - the error that has ocurred
   * @param {boolean} isWriteAction - true if the action that generated
   *   the error is a write action, false otherwise
   * @param {function} onResolved - function to call after error is resolved
   * @param {function} onFailed - function to call if error can not be resolved
   */
  static handleError(application, service, error,
    isWriteAction, onResolved, onFailed)
  {
    const retry = () =>
    {
      onResolved();
    };

    const authenticate = () =>
    {
      new ServiceLoginDialog(application)
        .setService(service)
        .setError(error)
        .setWriteAction(isWriteAction)
        .setOnLogin(onResolved)
        .setOnFailed(onFailed)
        .show();
    };

    const showError = () =>
    {
      onFailed?.();
      const message = error.message;
      MessageDialog.create("ERROR", message)
        .setClassName("error")
        .setI18N(application.i18n).show();
    };

    ServerSession.getSession(service).handleError(error,
      retry, authenticate, showError);
  }
}

export { ErrorHandler };