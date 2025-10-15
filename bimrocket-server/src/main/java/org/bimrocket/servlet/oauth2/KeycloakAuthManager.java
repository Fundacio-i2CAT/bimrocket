package org.bimrocket.servlet.oauth2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.bimrocket.service.security.SecurityService;
import org.eclipse.microprofile.config.Config;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class KeycloakAuthManager implements AuthenticationManager
{
  @Inject
  Config config;

  static final String BASE = "services.security.oauth2.";

  @Override
  public String getAuthenticationToken(String code) throws Exception
  {
      config.getValue(BASE + "keycloak.urlAuthenticationToken", String.class);

      String json = "";
      URL url = new URL("https://iam.i2cat.net/auth/realms/SEG/protocol/openid-connect/token");
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setDoOutput(true);
      conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

      String params = "grant_type=authorization_code"
              + "&client_id=bim"
              + "&client_secret=SibGBtmT9yWfH0BwKcXx3n1sbU9lAOIL"
              + "&redirect_uri=http://localhost:9090/bimrocket-server/api/oauth2/authCode"
              + "&code=" + code;

      try (OutputStream os = conn.getOutputStream()) {
          os.write(params.getBytes(StandardCharsets.UTF_8));
      }

      try (InputStream responseStream = conn.getInputStream()) {
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

  @Override
  public boolean checkUseridDB(UserToken userToken, SecurityService securityService) throws Exception
  {
    return false;
  }

  @Override
  public void generateHTMLResponse(UserToken userToken) throws Exception
  {
  }
}
