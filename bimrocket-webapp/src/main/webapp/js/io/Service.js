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
   * name, description, url
   */
  getParameters()
  {
    return {
      name : this.name,
      description : this.description,
      url : this.url,
    };
  }

  /**
   * Sets the persistent service parameters.
   *
   * @param {object} parameters - the service paramaters to set that contains
   * these properties: name, description and url
   */
  setParameters(parameters)
  {
    this.name = parameters.name;
    this.description = parameters.description;
    this.url = parameters.url;
  }
}

export { Service };