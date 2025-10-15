package org.bimrocket.servlet.oauth2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.bimrocket.service.security.SecurityService;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class KeycloakAuthManager implements AuthenticationManager
{
  @Override
  public String getAuthenticationToken(String code) throws Exception
  {
      String json = "";
      URL url = new URL("http://localhost:8080/auth/realms/bim/protocol/openid-connect/token");
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setDoOutput(true);
      conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

      String params = "grant_type=authorization_code"
              + "&client_id=bim-test"
              + "&client_secret=QYtoLmYX25Q4yQzdz425yowdiC080AkX"
              + "&redirect_uri=http://127.0.0.1:9090/bimrocket-server/api/oauth2/authCode"
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
