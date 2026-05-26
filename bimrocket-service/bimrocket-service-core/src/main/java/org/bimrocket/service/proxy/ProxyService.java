/*
 * BIMROCKET
 *
 * Copyright (C) 2021-2026, Ajuntament de Sant Feliu de Llobregat
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
package org.bimrocket.service.proxy;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bimrocket.api.security.User;
import org.bimrocket.exception.AccessDeniedException;
import org.bimrocket.exception.InvalidRequestException;
import static org.bimrocket.service.security.SecurityConstants.AUTHENTICATED_ROLE;
import org.bimrocket.service.security.SecurityService;
import org.bimrocket.util.URIEncoder;
import org.eclipse.microprofile.config.Config;

/**
 *
 * @author realor
 */
@ApplicationScoped
public class ProxyService
{
  static final Logger LOGGER = Logger.getLogger(ProxyService.class.getName());

  static final String BASE = "services.proxy.";

  static final String URL_PARAMETER = "url";

  static final HashSet<String> ignoredHeaders = new HashSet<>();

  static
  {
    ignoredHeaders.add("host");
    ignoredHeaders.add("connection");
    ignoredHeaders.add("content-length");
    ignoredHeaders.add("user-agent");
    ignoredHeaders.add("accept-encoding");
  }

  @Inject
  SecurityService securityService;

  @Inject
  Config config;

  HttpClient httpClient;

  @PostConstruct
  public void init()
  {
    LOGGER.log(Level.INFO, "Init ProxyService");

    httpClient = HttpClient.newBuilder()
      .version(HttpClient.Version.HTTP_1_1)
      .build();
  }

  @PreDestroy
  public void destroy()
  {
    LOGGER.log(Level.INFO, "Destroying ProxyService");

    // httpClient shutdowns its internal thread when it is no longer referenced
    // A GC is performed to release the httpClient.
    // In JDK21 this could be replaced by a call to the httpClient.close()
    // method which was introduced to stop this thread.

    httpClient = null;
    System.gc();

    try
    {
      Thread.sleep(5000);
    }
    catch (InterruptedException ex)
    {
    }
  }

  public HttpResponse<InputStream> forwardRequest(
    String method,
    Map<String, List<String>> parameterMap,
    Map<String, List<String>> headerMap,
    String remoteAddress,
    InputStream body)
  {
    try
    {
      HttpRequest request =
        createRequest(method, parameterMap, headerMap, remoteAddress, body);

      return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    }
    catch (RuntimeException ex)
    {
      throw ex;
    }
    catch (Exception ex)
    {
      throw new RuntimeException(ex);
    }
  }

  private HttpRequest createRequest(
    String method,
    Map<String, List<String>> parameterMap,
    Map<String, List<String>> headerMap,
    String remoteAddress,
    InputStream body)
    throws Exception
  {
    List<String> urlList = parameterMap.get(URL_PARAMETER);
    if (urlList == null || urlList.isEmpty())
        throw new InvalidRequestException(URL_PARAMETER + " parameter required");

    String url = urlList.get(0);

    String alias;

    // get and validate url
    if (url.startsWith("@"))
    {
      alias = url.substring(1);

      String aliasBase = BASE + "aliases." + alias + ".";

      url = config.getOptionalValue(aliasBase + "url", String.class).orElse(null);
      if (url == null)
      {
        throw new IOException("Invalid alias url: " + alias);
      }

      if (remoteAddress != null)
      {
        String ipFilter =
          config.getOptionalValue(aliasBase + ".ipfilter", String.class).orElse(null);
        if (ipFilter != null && !remoteAddress.startsWith(ipFilter))
        {
          throw new AccessDeniedException("Not authorized remote ip: " + remoteAddress);
        }
      }
    }
    else
    {
      alias = null;

      if (!isValidUrl(url))
      {
        throw new AccessDeniedException("Access forbidden to " + url);
      }
    }

    String encodedUrl = URIEncoder.encode(url);
    StringBuilder uriBuffer = new StringBuilder(encodedUrl);
    boolean firstParam = true;

    // add parameters to url
    for (String name : parameterMap.keySet())
    {
      if (!name.equals("url"))
      {
        List<String> values = parameterMap.get(name);
        for (String value : values)
        {
          if (firstParam)
          {
            uriBuffer.append("?");
            firstParam = false;
          }
          else
          {
            uriBuffer.append("&");
          }
          String encodedName = URLEncoder.encode(name, "UTF-8");
          String encodedValue = URLEncoder.encode(value, "UTF-8");
          uriBuffer.append(encodedName).append("=").append(encodedValue);
        }
      }
    }

    // set body
    HttpRequest.BodyPublisher bodyPub = body == null ?
      HttpRequest.BodyPublishers.noBody() :
      HttpRequest.BodyPublishers.ofInputStream(() -> body);

    HttpRequest.Builder builder = HttpRequest.newBuilder()
      .uri(new URI(uriBuffer.toString()))
      .method(method, bodyPub);

    // set headers
    setHttpHeaders(builder, headerMap, alias);
    builder.header("X-Forwarded-For", remoteAddress);
    return builder.build();
  }

  private boolean isValidUrl(String url)
  {
    List<String> validUrls =
      config.getValues(BASE + "validUrls", String.class);

    for (String validUrl : validUrls)
    {
      if (url.startsWith(validUrl))
      {
        return true;
      }
    }

    User user = securityService.getCurrentUser();
    return user.getRoleIds().contains(AUTHENTICATED_ROLE);
  }

  private void setHttpHeaders(HttpRequest.Builder builder,
    Map<String, List<String>> headerMap, String alias)
  {
    for (String name : headerMap.keySet())
    {
      String value = headerMap.get(name).get(0);

      if (alias != null
        && name.equalsIgnoreCase("Authorization")
        && "Bearer implicit".equals(value))
      {
        String authoKey = BASE + "aliases." + alias + ".authorization";
        String autho = config.getOptionalValue(authoKey, String.class).orElse(null);
        if (autho != null)
        {
          builder.header("Authorization", autho);
        }
      }
      else
      {
        if (name.equalsIgnoreCase("Forwarded-Authorization"))
        {
          builder.header("Authorization", value);
        }
        else if (!ignoredHeaders.contains(name.toLowerCase()))
        {
          builder.header(name, value);
        }
      }
    }
  }
}