/**
 * Service.js
 *
 * @author realor
 */

class Service
{
  constructor(parameters)
  {
    if (typeof parameters === "object")
    {
      this.setParameters(parameters);
    }
  }

  /**
   * Gets the persistent service parameters.
   *
   * @returns {object} the service paramaters that contains these properties:
   * name, description, url, useBasicAuth
   */
  getParameters()
  {
    return {
      name : this.name,
      description : this.description,
      url : this.url,
      useBasicAuth : this.useBasicAuth,
    };
  }

  /**
   * Sets the persistent service parameters.
   *
   * @param {object} parameters - the service paramaters to set that contains
   * these properties: name, description, url and useBasicAuth
   */
  setParameters(parameters)
  {
    this.name = parameters.name;
    this.description = parameters.description;
    this.url = parameters.url;
    this.useBasicAuth = parameters.useBasicAuth || false;
  }
}

export { Service };