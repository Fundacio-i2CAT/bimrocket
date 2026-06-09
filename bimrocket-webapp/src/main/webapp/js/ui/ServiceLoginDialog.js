/*
 * ServiceLoginDialog.js
 *
 * @author alexis-i2cat
 * @author realor
 */

import { LoginDialog } from "./LoginDialog.js";
import { SecurityService } from "../io/SecurityService.js";
import { Controls } from "./Controls.js";
import { ServerSession } from "../io/ServerSession.js";
import { Auth } from "./Auth.js";

class ServiceLoginDialog extends LoginDialog
{
	constructor(application)
	{
		super(application);

		this.onLogin = null;
		this.onFailed = null;
    this.oauthContainer = null;

    this.authSuccessListener = (event) => this.handleOAuthSuccess(event);
  }

	setService(service)
	{
    this.serverSession = ServerSession.getSession(service);
    return this;
	}

	setOnLogin(onLogin)
	{
		this.onLogin = onLogin;
		return this;
	}

	setOnFailed(onFailed)
	{
		this.onFailed = onFailed;
		return this;
	}

  show()
  {
    super.show();
    window.addEventListener("message", this.authSuccessListener);

    this.loadOAuth2Providers();

    return this;
  }

  hide()
  {
    super.hide();
    window.removeEventListener("message", this.authSuccessListener);
    if (this.oauthWindow)
    {
      this.oauthWindow.close();
      this.oauthWindow = null;
    }
    return this;
  }

	onCancel()
	{
		super.onCancel();
		this.onFailed?.();
	}

  loadOAuth2Providers()
  {
    const serverSession = this.serverSession;
    if (!serverSession) return;

    const baseUrl = serverSession.baseUrl;
    if (baseUrl === "" || baseUrl.startsWith("/"))
    {
      // Only show OAuth2 providers for same domain services
      serverSession.getOAuth2Providers((providers) =>
      {
        if (providers.length > 0) this.addOAuth2Buttons(providers);
      });
    }
  }

  addOAuth2Buttons(providers)
  {
    this.setSize(280, 240); // increase Dialog height

    if (!this.oauthContainer)
    {
      this.oauthContainer = document.createElement("div");
      this.oauthContainer.className = "oauth_buttons";
      this.footerElem.append(this.oauthContainer);
    }
    else
    {
      this.oauthContainer.innerHTML = "";
    }

    providers.forEach((provider) =>
    {
      const buttonLabel = provider.name.toUpperCase();
      const authUrl = provider.authUrl;

      if (provider.logoUrl)
      {
        Controls.addButtonWithLogo(
          this.oauthContainer,
          `auth_${provider.name}`,
          buttonLabel,
          provider.logoUrl,
          () => this.oauthWindow = Auth.openAuthPopup(authUrl),
          "oauth_logo_btn"
        );
      }
      else
      {
        Controls.addButton(
          this.oauthContainer,
          `auth_${provider.name}`,
          buttonLabel,
          () => this.oauthWindow = Auth.openAuthPopup(authUrl)
        );
      }
    });
  }

  handleOAuthSuccess(event)
  {
    if (event.data.includes("success"))
    {
      this.hide();
      this.onLogin?.();
    }
  }

  login(username, password)
  {
    const progressBar = this.application.progressBar;
    const serverSession = this.serverSession;
    if (!serverSession) return;

    progressBar.process = "undefined";
    progressBar.visible = true;

    serverSession.login(username, password, () =>
    {
      progressBar.visible = false;
      this.hide();
      this.onLogin?.();
    },
    (error) =>
    {
      progressBar.visible = false;
      this.show();
			this.setMessage("message.login_failed");
    });
  }
}

export { ServiceLoginDialog }