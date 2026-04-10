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
package org.bimrocket.servlet.oauth2;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.NewCookie;
import org.bimrocket.api.security.User;
import org.bimrocket.service.security.SecurityService;
import org.bimrocket.util.TextUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

import static org.bimrocket.util.TextUtils.getISODate;

/**
 *
 * @author jordi.hernandez@i2cat.net
 */
@WebServlet(name = "Oauth2", urlPatterns = "/api/oauth2/*")
public class TokenValidationServlet extends HttpServlet
{
  private static final long serialVersionUID = 1L;

  static final Logger LOGGER =
    Logger.getLogger(TokenValidationServlet.class.getName());

  private static final String MISSING_CODE_PARAMETER = "Missing Code parameter";
  private static final String INVALID_PATH_REDIRECT_URL = "Invalid path for redirect url";

  @Inject
  transient SecurityService securityService;

  @Inject
  Instance<AuthenticationManager> authManagers;

  private Map<String, AuthenticationManager> issuerManagers;

  @Override
  public void init()
  {
    issuerManagers = new HashMap<>();
    for (AuthenticationManager manager : authManagers)
    {
      issuerManagers.put(manager.getIssuer(), manager);
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException
  {
    String pathInfo = request.getPathInfo();

    // Get redirect uri specified in the request to be used after when call to get token from code
    String redirectUri = request.getRequestURL().toString();

    String code = request.getParameter("code");
    if (code == null || code.isBlank())
    {
      sendBadRequest(response, MISSING_CODE_PARAMETER);
      return;
    }

    logParameters(request, pathInfo);

    try
    {
      String issuer = extractIssuer(pathInfo);

      AuthenticationManager manager = issuerManagers.get(issuer);

      if (manager == null)
      {
        sendBadRequest(response, INVALID_PATH_REDIRECT_URL);
        return;
      }

      String json = manager.getAuthenticationToken(code, redirectUri);
      UserToken ut = manager.getUseridFromToken(json);
      checkUseridDB(ut, manager);

      // Create HttpOnly cookie
      NewCookie cookie = securityService.createHttpOnlyCookie(request, ut.getUserId());
      response.addHeader("Set-Cookie", cookie.toString());

      // Generate response HTML with the access token
      generateHTMLResponse(response);
    }
    catch(Exception e)
    {
      sendBadRequest(response, e.getMessage());
    }

  }

  // internal methods

  private String extractIssuer(String pathInfo)
  {
    if (pathInfo == null || pathInfo.isBlank())
    {
      return null;
    }

    String[] parts = pathInfo.split("/");

    return parts[parts.length - 1].toLowerCase();
  }

  void logParameters(HttpServletRequest request, Object parameters)
  {
    request.setAttribute("log.parameters", parameters.toString());
  }

  private void sendBadRequest(HttpServletResponse resp, String error) throws IOException
  {
    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    resp.setContentType("application/json");
    resp.getWriter().write("{\"error\":\"" + error + "\"}");
  }

  private void checkUseridDB(UserToken userToken, AuthenticationManager manager) throws Exception
  {
    User user = securityService.getUser(userToken.getUserId());
    if (user == null)
    {
      User newUser = new User();
      newUser.setId(userToken.getUserId());
      newUser.setName(userToken.getUserId());
      newUser.setPassword(Utils.generatePassword());
      newUser.setAccessToken(userToken.getAccessToken());
      newUser.setAccessTokenExpiresAt(TextUtils.addTime(getISODate(), 5, TextUtils.MINUTES));
      newUser.setRefreshToken(userToken.getRefreshToken());
      newUser.setRefreshTokenExpiresAt(TextUtils.addTime(getISODate(), 2, TextUtils.HOURS));
      newUser.setRoleIds(new HashSet<>(manager.getDefaultRoles()));
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

  private void generateHTMLResponse(HttpServletResponse response) throws Exception
  {
    String targetOrigin = "*";

    String message = "Cookie created successfully";

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();

    out.println("<!DOCTYPE html>");
    out.println("<html lang='ca'>");
    out.println("<head><meta charset='UTF-8'><title>Authentication completed</title></head>");
    out.println("<body>");
    out.println("<script>");
    out.println("  (function() {");
    out.println("    const message = '" + message + "';");
    out.println("    if (window.opener) {");
    out.println("      window.opener.postMessage(message, '" + targetOrigin + "');");
    out.println("      window.close();");
    out.println("    } else {");
    out.println("      document.body.textContent = message;");
    out.println("    }");
    out.println("  })();");
    out.println("</script>");
    out.println("</body></html>");
  }

}
