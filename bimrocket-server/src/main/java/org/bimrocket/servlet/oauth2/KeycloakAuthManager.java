package org.bimrocket.servlet.oauth2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.bimrocket.service.security.SecurityService;
import org.bimrocket.util.TextUtils;
import org.eclipse.microprofile.config.Config;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bimrocket.api.security.User;
import static org.bimrocket.util.TextUtils.getISODate;

public class KeycloakAuthManager implements AuthenticationManager
{
  @Inject
  Config config;

  @Inject
  SecurityService securityService;

  static final String BASE = "services.security.oauth2.";

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
  public void checkUseridDB(UserToken userToken) throws Exception
  {
    Set<String> rols = new HashSet<>();
    List<String> valors = Arrays.asList(Utils.PROJECTISTA, Utils.VECTOR_UT_OGE);
    rols.addAll(valors);

    User user = securityService.getUser(userToken.getUserId());
    if (user == null)
    {
      User newUser = new User();
      newUser.setAccessToken(userToken.getAccessToken());
      newUser.setAccessTokenExpiresAt(TextUtils.addTime(getISODate(), 5, TextUtils.MINUTES));
      newUser.setId(userToken.getUserId());
      newUser.setName(userToken.getUserId());
      newUser.setPassword(Utils.generatePassword());
      newUser.setRefreshToken(userToken.getRefreshToken());
      newUser.setRefreshTokenExpiresAt(TextUtils.addTime(getISODate(), 2, TextUtils.HOURS));
      newUser.setRoleIds(rols);
      securityService.createUser(newUser);
    }
    else
    {
      user.setAccessToken(userToken.getAccessToken());
      user.setAccessTokenExpiresAt(TextUtils.addTime(getISODate(), 5, TextUtils.MINUTES));
      user.setRefreshToken(userToken.getRefreshToken());
      user.setRefreshTokenExpiresAt(TextUtils.addTime(getISODate(), 2, TextUtils.HOURS));
      securityService.updateUser(user);
    }
  }

  @Override
  public void generateHTMLResponse(UserToken userToken, HttpServletResponse response) throws Exception
  {
    String targetOrigin = "*";

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();

    out.println("<!DOCTYPE html>");
    out.println("<html lang='ca'>");
    out.println("<head><meta charset='UTF-8'><title>Authentication completed</title></head>");
    out.println("<body>");
    out.println("<script>");
    out.println("  (function() {");
    out.println("    const token = " + Utils.escapeJsString(userToken.getAccessToken()) + ";");
    out.println("    if (window.opener) {");
    out.println("      window.opener.postMessage({ token: token }, '" + targetOrigin + "');");
    out.println("      window.close();");
    out.println("    } else {");
    out.println("      document.body.textContent = 'Unable to return access token.';");
    out.println("    }");
    out.println("  })();");
    out.println("</script>");
    out.println("</body></html>");
  }
}
