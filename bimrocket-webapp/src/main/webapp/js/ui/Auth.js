/**
 * Auth.js
 *
 * @author alexis-i2cat
 */

export class Auth
{
  static init()
  {
    window.addEventListener("message", Auth.handleAuthToken);
  }

  static openAuthPopup(authUrl)
  {
    const popupWidth = 900, popupHeight = 700;
    const left = (window.screen.width / 2) - (popupWidth / 2);
    const top = (window.screen.height / 2) - (popupHeight / 2);
    const options = `width=${popupWidth},height=${popupHeight},top=${top},left=${left}`;

    window.open(authUrl, "authPopup", options);
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


