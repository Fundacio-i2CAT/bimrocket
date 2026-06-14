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
  static handleError(application, service, error,
    isWriteAction, onResolved, onFailed)
  {
    const retry = () =>
    {
      onResolved();
    };

    const authenticate = () =>
    {
      let message;

      if (error.code === 401 ||
        error.status === Result.INVALID_CREDENTIALS)
      {
        message = "message.invalid_credentials";
      }
      else if (error.code === 403 ||
               error.status === Result.FORBIDDEN)
      {
        message = isWriteAction ?
          "message.action_denied" : "message.accces_denied";
      }
      else if (error.message)
      {
        message = error.message;
      }
      else
      {
        message = String(error);
      }

      new ServiceLoginDialog(application)
        .setService(service)
        .setMessage(message)
        .setOnLogin(onResolved)
        .setOnFailed(onFailed)
        .show();
    };

    const showError = () =>
    {
      onFailed?.();
      const message = error.message || String(error);
      MessageDialog.create("ERROR", message)
        .setClassName("error")
        .setI18N(application.i18n).show();
    };

    ServerSession.getSession(service).handleError(error,
      retry, authenticate, showError);
  }
}

export { ErrorHandler };