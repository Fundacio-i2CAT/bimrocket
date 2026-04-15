/*
 * LoginDialog.js
 *
 * @author realor
 */

import { Auth } from "./Auth.js";
import { Dialog } from "./Dialog.js";

class LoginDialog extends Dialog
{
  constructor(application, message)
  {
    super("title.login");
    this.application = application;
    this.setI18N(this.application.i18n);

    this.setSize(260, 220);

    if (message)
    {
      this.errorElem = this.addText(message, "error block");
    }

    this.usernameElem = this.addTextField("loginUser",
      "label.username", "");
    this.usernameElem.setAttribute("spellcheck", "false");

    this.passwordElem = this.addPasswordField("loginPassword",
      "label.password", "");

    this.passwordElem.addEventListener("keypress", event =>
    {
      if (event.keyCode === 13)
      {
        this.onAccept();
      }
    });

    this.acceptButton = this.addButton("login_accept", "button.accept",
      () => this.onAccept());

    this.cancelButton = this.addButton("login_cancel", "button.cancel",
      () => this.onCancel());

    Auth.addAuthButtons(this.footerElem)

    this.authSuccessHandler = (event) =>
      {
        this.hide();
        this.application.setup.sessionActive = true;
        this.application.checkCurrentSession();
        window.removeEventListener("auth-success", this.authSuccessHandler);
      };
  
    window.addEventListener("auth-success", this.authSuccessHandler);
  }

  onShow()
  {
    this.usernameElem.focus();
  }

  onAccept()
  {
    this.hide();
    this.login(this.usernameElem.value, this.passwordElem.value);
  }

  onCancel()
  {
    this.hide();
  }

  login(username, password)
  {
  }
}

export { LoginDialog };
