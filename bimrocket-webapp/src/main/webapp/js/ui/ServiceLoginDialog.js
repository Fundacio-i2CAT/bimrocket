/*
 * ServiceLoginDialog.js
 *
 * @author alexis-i2cat
 */

import { LoginDialog } from "./LoginDialog.js";
import { SecurityService } from "../io/SecurityService.js";
import { Controls } from "./Controls.js";
import { Auth } from "./Auth.js";

class ServiceLoginDialog extends LoginDialog
{
	constructor(application)
	{
		super(application);

		this.service = null;
		this.onLogin = null;
		this.onFailed = null;
    this.oauthContainer = null;

    this.authSuccessListener = () => this.handleOAuthSuccess();
	}

	setService(service)
	{
		this.service = service;
		return this;
	}

	setMessage(message)
	{
		const translatedMessage = this.application.i18n.get(message);

		if (!this.errorElem)
		{
			this.errorElem = this.addText(translatedMessage, "error block");
			this.bodyElem.insertBefore(this.errorElem, this.bodyElem.firstChild);
		}
		else
		{
			this.errorElem.textContent = translatedMessage;
		}

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
    window.addEventListener("auth-success", this.authSuccessListener);

    if (this.service) this.loadOAuthProviders();

    return this;
  }

  hide()
  {
    super.hide();
    window.removeEventListener("auth-success", this.authSuccessListener);
  }

	onCancel()
	{
		super.onCancel();
		if (this.onFailed) this.onFailed();
	}

  getBaseUrl()
  {
    const serviceUrl = this.service.url;
		let index = serviceUrl.indexOf("/api");
		let baseUrl = index !== -1 ? serviceUrl.substring(0, index) : serviceUrl;
		if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length - 1);

    return baseUrl;
  }

  getSecurityService()
  {
    return new SecurityService({
      url: this.getBaseUrl() + "/api/security"
    });
  }

  loadOAuthProviders()
  {
    const securityService = this.getSecurityService();

    securityService.getOAuth2Providers(
      (providers) =>
      {
        if (providers.length > 0) this.addOAuthButtons(providers, securityService.url);
      }
    );
  }

  addOAuthButtons(providers, securityBaseUrl)
  {
    if (!this.oauthContainer)
    {
      this.oauthContainer = document.createElement("div");
      this.oauthContainer.className =  "oauth_buttons";
      this.bodyElem.append(this.oauthContainer);
    }
    else
    {
      this.oauthContainer.innerHTML = "";
    }

    providers.forEach((provider) => {
      const buttonLabel = provider.name.toUpperCase();
      const authUrl = `${securityBaseUrl}/oauth2/login/${provider.name}`;

      Controls.addButton(
        this.oauthContainer,
        `auth_${provider.name}`,
        buttonLabel,
        () => Auth.openAuthPopup(authUrl)
      );
    });
  }

  handleOAuthSuccess()
  {
    this.hide();
    if (this.onLogin) this.onLogin();
  }

	login(username, password) 
	{
		if (!this.service) throw new Error("Service is required");

    const temporarySecurityService = this.getSecurityService();

    temporarySecurityService.login(username, password, () => {
      this.hide();
      if (this.onLogin) this.onLogin();
		}, 
		(error) => 
		{
			this.show();
			this.setMessage("message.login_failed");
		});
	}
}

export { ServiceLoginDialog }