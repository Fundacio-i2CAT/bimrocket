/*
 * BIMROCKET
 *
 * Copyright (C) 2021-2025, Ajuntament de Sant Feliu de Llobregat
 *
 * This program is licensed and may be used, modified and redistributed under
 * the terms of the European Public License (EUPL), either version 1.1 or (at
 * your option) any later version as soon as they are approved by the European
 * Commission.
 *
 * Alternatively, you may redistribute and/or modify this program under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either  version 3 of the License, or (at your option)
 * any later version.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the licenses for the specific language governing permissions, limitations
 * and more details.
 *
 * You should have received a copy of the EUPL1.1 and the LGPLv3 licenses along
 * with this program; if not, you may find them at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 * http://www.gnu.org/licenses/
 * and
 * https://www.gnu.org/licenses/lgpl.txt
 */
package org.bimrocket.api.security.oauth2.providers;

import jakarta.inject.Inject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import org.bimrocket.api.security.oauth2.OAuth2Provider;
import org.bimrocket.api.security.oauth2.TokenInfo;
import org.eclipse.microprofile.config.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author jordi.hernandez@i2cat.net
 * @author realor
 */
public abstract class AbstractOAuth2Provider implements OAuth2Provider
{
  static final String BASE = "services.security.oauth2.providers.";

  @Inject
  Config config;

  @Override
  public String getLogoUrl()
  {
    return getValue("logoUrl");
  }

  @Override
  public String getAuthorizationUrl(String redirectUri, String state)
    throws Exception
  {
    String baseUrl = getValue("baseUrl", "");
    String authUrl = getValue("authUrl");
    String clientId = getValue("clientId");
    String scope = getValue("scope", "authenticate");

    if (authUrl.startsWith("/")) authUrl = baseUrl + authUrl;

		return authUrl +
			"?scope=" + urlEncode(scope) +
			"&redirect_uri=" + urlEncode(redirectUri) +
			"&response_type=code" +
			"&client_id=" + urlEncode(clientId) +
			"&approval_prompt=auto" +
      "&state=" + urlEncode(state);
  }

  @Override
  public TokenInfo exchangeCode(String code, String redirectUri)
    throws Exception
  {
    // We are receiving the redirectUri with parameters, but at the moment to get the autorization
    // we declared redirectUri without partameters. For this we remove from the url all the parameters
    //begining with ?
    int idx = redirectUri.indexOf('?');
    redirectUri = idx >= 0 ? redirectUri.substring(0, idx) : redirectUri;

    String baseUrl = getValue("baseUrl", "");
    String tokenUrl = getValue("tokenUrl");
    String clientId = getValue("clientId");
    String secretId = getValue("secretId");

    if (tokenUrl.startsWith("/")) tokenUrl = baseUrl + tokenUrl;


    String params = "grant_type=authorization_code"
      + "&client_id=" + clientId
      + "&client_secret=" + secretId
      + "&redirect_uri=" + redirectUri
      + "&code=" + code;

    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(tokenUrl))
      .header("Content-Type", "application/x-www-form-urlencoded")
      .POST(HttpRequest.BodyPublishers.ofString(params))
      .build();

    HttpClient client = HttpClient.newHttpClient();
    HttpResponse<String> response =
      client.send(request, HttpResponse.BodyHandlers.ofString());

    return parseTokenInfo(response.body());
  }

  protected TokenInfo parseTokenInfo(String json) throws Exception
  {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode tokenNode = mapper.readTree(json);

    TokenInfo tokenInfo = new TokenInfo();

    if (tokenNode.has("access_token"))
    {
      tokenInfo.setAccessToken(tokenNode.get("access_token").asText());
    }
    if (tokenNode.has("refresh_token"))
    {
      tokenInfo.setRefreshToken(tokenNode.get("refresh_token").asText());
    }
    if (tokenNode.has("error"))
    {
      tokenInfo.setError(tokenNode.get("error").asText());
    }
    return tokenInfo;
  }

  protected String getValue(String path)
  {
    return config.getValue(BASE + getName() + "." + path, String.class);
  }

  protected String getValue(String path, String orElse)
  {
    return config.getOptionalValue(BASE + getName() + "." + path, String.class)
      .orElse(null);
  }

  protected List<String> getValues(String path)
  {
    return config.getOptionalValues(BASE + getName() + "." + path, String.class)
      .orElse(Collections.emptyList());
  }

	protected String urlEncode(final String value)
    throws UnsupportedEncodingException
  {
    if (value == null) return "";
		return URLEncoder.encode(value, "UTF-8");
	}
}
