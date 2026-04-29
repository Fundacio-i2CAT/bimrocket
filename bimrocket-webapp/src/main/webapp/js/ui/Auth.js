/**
 * Auth.js
 *
 * @author alexis-i2cat
 */

import { Environment } from "../Environment.js";
import { Controls } from "./Controls.js";

export class Auth
{
  /**
 * Responsible for OAuth authentication management.
 * It relies on the configuration defined in Environment.js (AUTH_ENVIRONMENT).
 * If the current origin (window.location.origin) does not match any key
 * in Environment.js, OAuth authentication is disabled and only basic login is shown.
 */

  static currentConfig = null;

  static init()
  {
    const origin = window.location.origin;
    Auth.currentConfig = Environment.AUTH_ENVIRONMENT?.[origin];

    if (!Auth.currentConfig) return;

    window.addEventListener("message", Auth.handleAuthToken);
  }

  static addAuthButtons(parentElement)
  {
    if (!Auth.currentConfig) return;
  
    const container = document.createElement("div");
    parentElement.appendChild(container);

    const providers = Object.keys(Auth.currentConfig);

    providers.forEach((provider) =>
    {
      const buttonLabel = provider.toUpperCase();

      Controls.addButton(
        container,
        `auth_${provider}`,
        buttonLabel,
        () => Auth.login(provider),
      );
    });
  }

  static login(providerName)
  {
    const config = Auth.currentConfig?.[providerName];

    if (!config) throw new Error ("Invalid configuration or unsupported environment");

    Auth.openAuthPopup(config);
  }

  static openAuthPopup(config)
  {
    const params = new URLSearchParams();
    params.append("response_type", "code");
    params.append("client_id", config.clientId);
    params.append("scope", config.scope);
    params.append("redirect_uri", config.redirectUri);

    const url = `${config.authUrl}?${params.toString()}`;

    const popupWidth = 900, popupHeight = 700;
    const left = (window.screen.width / 2) - (popupWidth / 2);
    const top = (window.screen.height / 2) - (popupHeight / 2);
    const options = `width=${popupWidth},height=${popupHeight},top=${top},left=${left}`;

    window.open(url, "authPopup", options);
  }

  static handleAuthToken(event)
  { 
    if (!event.data) return;

    if (event.data.includes("success"))
    {
        const authEvent = new CustomEvent("auth-success");
        window.dispatchEvent(authEvent);
    }
  }
}


