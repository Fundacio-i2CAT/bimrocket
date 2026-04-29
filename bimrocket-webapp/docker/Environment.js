/* Environment.js */

export const Environment =
{
  SERVER_URL : "http://localhost:9090/bimrocket-server",
  SERVER_ALIAS : "bimrocket",
  MODULES : ["base", "bim", "gis"]
};

/**
 * Optional: 'AUTH_ENVIRONMENT' can be added to configure multiple authentication providers.
 * * Each key in this object represents the system's host (e.g., "http://bim.santfeliu.cat").
 * It is crucial that this key matches the actual environment host; otherwise, the 
 * authorization buttons for these services will not be displayed.
 * * For each host, you can define different authorization methods (such as VALID, 
 * GICAR, or Keycloak) by specifying their OAuth2 parameters: authorization URL, 
 * client ID, scopes, and the redirect URI for the authentication code.
 */