package org.bimrocket.servlet.oauth2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.config.Config;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 *
 * @author jordi.hernandez@i2cat.net
 */
public class KeycloakAuthManager implements AuthenticationManager
{
  @Inject
  Config config;

  static final String BASE = "services.security.oauth2.";

  @Override
  public String getIssuer()
  {
    return "keycloak";
  }

  @Override
  public List<String> getDefaultRoles()
  {
    return List.of(Utils.PROJECTISTA);
  }

  @Override
  public String getAuthenticationToken(String code, String redirectUri) throws Exception
  {
    String urlAuthenticationToken = config.getValue(BASE + "keycloak.urlAuthenticationToken", String.class);
    String clientId = config.getValue(BASE + "keycloak.clientId", String.class);
    String secretId = config.getValue(BASE + "keycloak.secretId", String.class);

    String json = "";
    URL url = new URL(urlAuthenticationToken);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setDoOutput(true);
    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

    String params = "grant_type=authorization_code"
            + "&client_id=" + clientId
            + "&client_secret=" + secretId
            + "&redirect_uri=" + redirectUri
            + "&code=" + code;

    try (OutputStream os = conn.getOutputStream())
    {
      os.write(params.getBytes(StandardCharsets.UTF_8));
    }

    try (InputStream responseStream = conn.getInputStream())
    {
      json = IOUtils.toString(responseStream, StandardCharsets.UTF_8);
    }

    return json;
  }

  @Override
  public UserToken getUseridFromToken(String jsonToken) throws Exception
  {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(jsonToken);

    String accessToken = node.get("access_token").asText();
    String refreshToken = node.get("refresh_token").asText();
    String idtoken = node.get("id_token").asText();

    //Decode id token
    JsonNode jsonNode = Utils.decodeJWTToken(idtoken);
    String username = jsonNode.get("preferred_username").asText();

    return  new UserToken(username, accessToken, refreshToken);
  }

}
