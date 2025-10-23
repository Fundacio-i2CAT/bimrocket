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

public class ValidAuthManager implements AuthenticationManager
{
  @Inject
  Config config;

  static final String BASE = "services.security.oauth2.";

  @Override
  public String getAuthenticationToken(String code, String redirectUri) throws Exception
  {
    String urlAuthenticationToken = config.getValue(BASE + "valid.urlAuthenticationToken", String.class);
    String clientId = config.getValue(BASE + "valid.clientId", String.class);
    String secretId = config.getValue(BASE + "valid.secretId", String.class);

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

    String identifier = "";
    String accessToken = node.get("access_token").asText();
    String refreshToken = node.get("refresh_token").asText();
    String urlGetUserInfo = config.getValue(BASE + "valid.urlGetUserInfo"+ "?AccessToken=" + accessToken, String.class);

    // Call again to get identifier
    HttpURLConnection connection = null;
    URL url = new URL(urlGetUserInfo);
    connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    connection.setRequestProperty("Accept", "application/json");

    int responseCode = connection.getResponseCode();
    if (responseCode == HttpURLConnection.HTTP_OK)
    {
      try (InputStream inputStream = connection.getInputStream())
      {
        String jsonResponse = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        mapper = new ObjectMapper();
        JsonNode jsonUser = mapper.readTree(jsonResponse);
        if (jsonUser.has("identifier"))
        {
          identifier = jsonUser.get("identifier").asText();
        }
        else
        {
          throw new Exception("getUserInfo do not contains identifier");
        }
      }
    }
    else
    {
      throw new Exception("Unable to get User Info");
    }

    return  new UserToken(identifier, accessToken, refreshToken);
  }

}
