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

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bimrocket.api.security.User;
import org.bimrocket.service.file.*;
import org.bimrocket.service.security.SecurityService;
import org.bimrocket.util.TextUtils;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.bimrocket.util.TextUtils.getISODate;

/**
 *
 * @author realor
 */
@WebServlet(name = "Oauth2", urlPatterns = "/api/oauth2/*")
public class TokenValidationServlet extends HttpServlet
{
  private static final long serialVersionUID = 1L;

  static final Logger LOGGER =
    Logger.getLogger(TokenValidationServlet.class.getName());

  @Inject
  transient SecurityService securityService;

  @Inject
  transient KeycloakAuthManager keycloakAuthManager;

  @Inject
  transient ValidAuthManager validAuthManager;

  @Inject
  transient GicarAuthManager gicarAuthManager;

  private static final String MISSING_CODE_PARAMETER = "Missing Code parameter";
  private static final String INVALID_PATH_REDIRECT_URL = "Invalid path for redirect url";
  private static final String ISSUER_KEYCLOAK = "keycloak";
  private static final String ISSUER_VALID = "valid";
  private static final String ISSUER_GICAR = "gicar";

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException
  {
    // PathInfo to select for keycloak, valid or gicar issuer
    String pathInfo = request.getPathInfo();

    // Get redirect uri specified in the request to be used after when call to get token from code
    String redirectUri = request.getRequestURL().toString();
    String json = "";
    UserToken ut = null;

    // Cocde received by keycloak in the redirect uri
    String code = request.getParameter("code");
    if (code == null || code.isBlank())
    {
      sendBadRequest(response, MISSING_CODE_PARAMETER);
      return;
    }

    logParameters(request, pathInfo);

    try
    {
      String issuer = null;

      if (pathInfo.contains(ISSUER_KEYCLOAK))
      {
        issuer = ISSUER_KEYCLOAK;
      } else if (pathInfo.contains(ISSUER_VALID))
      {
        issuer = ISSUER_VALID;
      } else if (pathInfo.contains(ISSUER_GICAR))
      {
        issuer = ISSUER_GICAR;
      }

      if (issuer == null)
      {
        sendBadRequest(response, INVALID_PATH_REDIRECT_URL);
        return;
      }

      switch (issuer)
      {
        case ISSUER_KEYCLOAK:
          json = keycloakAuthManager.getAuthenticationToken(code, redirectUri);
          ut = keycloakAuthManager.getUseridFromToken(json);
          checkUseridDB(ut, issuer);
          break;
        case ISSUER_VALID:
          json = validAuthManager.getAuthenticationToken(code, redirectUri);
          ut = validAuthManager.getUseridFromToken(json);
          checkUseridDB(ut, issuer);
          break;
        case ISSUER_GICAR:
          json = gicarAuthManager.getAuthenticationToken(code, redirectUri);
          ut = gicarAuthManager.getUseridFromToken(json);
          checkUseridDB(ut, issuer);
          break;
        default:
          sendBadRequest(response, INVALID_PATH_REDIRECT_URL);
          return;
      }

      // Generate response HTML with the access token
      generateHTMLResponse(ut, response);
    }
    catch(Exception e)
    {
      sendBadRequest(response, e.getMessage());
    }

  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException
  {
    response.setContentType("application/text");
    response.getWriter().write("POST method");
  }

  // internal methods

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

  public void checkUseridDB(UserToken userToken, String issuer) throws Exception
  {
    Set<String> rols = new HashSet<>();
    List<String> valorsKeycloak = Arrays.asList(Utils.PROJECTISTA);
    List<String> valorsValid = Arrays.asList(Utils.PROJECTISTA);
    List<String> valorsGicar = Arrays.asList(Utils.VECTOR_UT_OGE);

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
      switch (issuer)
      {
        case ISSUER_KEYCLOAK ->
        {
          rols.addAll(valorsKeycloak);
          newUser.setRoleIds(rols);
        }
        case ISSUER_VALID ->
        {
          rols.addAll(valorsValid);
          newUser.setRoleIds(rols);
        }
        case ISSUER_GICAR ->
        {
          rols.addAll(valorsGicar);
          newUser.setRoleIds(rols);
        }
      }
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
    out.println("    const accessToken = " + Utils.escapeJsString(userToken.getAccessToken()) + ";");
    out.println("    if (window.opener) {");
    out.println("      window.opener.postMessage({ accessToken: accessToken }, '" + targetOrigin + "');");
    out.println("      window.close();");
    out.println("    } else {");
    out.println("      document.body.textContent = 'Unable to return access token.';");
    out.println("    }");
    out.println("  })();");
    out.println("</script>");
    out.println("</body></html>");
  }

}
