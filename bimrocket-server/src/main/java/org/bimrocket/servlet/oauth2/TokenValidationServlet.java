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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bimrocket.service.file.*;
import org.bimrocket.service.security.SecurityService;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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

  private static final String MISSING_CODE_PARAMETER = "Missing Code parameter";

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {

    String redirectUri = request.getRequestURL().toString();
    String json = "";
    UserToken ut = null;

    String code = request.getParameter("code");
    if (code == null || code.isBlank())
    {
      sendBadRequest(response, MISSING_CODE_PARAMETER);
      return;
    }

    try {
      // Get authentication token from the code received
      json = keycloakAuthManager.getAuthenticationToken(code, redirectUri);

      // Get userid, access token and refresh token from the json token
      ut = keycloakAuthManager.getUseridFromToken(json);

      // Generate response HTML with the access token
      keycloakAuthManager.generateHTMLResponse(ut, response);
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

  void log(HttpServletRequest request, HttpServletResponse response)
  {
    String method = request.getMethod();
    String parameters = (String)request.getAttribute("log.parameters");
    int status = response.getStatus();

    if (parameters == null)
    {
      LOGGER.log(Level.INFO, "{0} -> {1}",
       new Object[] { method, status });
    }
    else
    {
      LOGGER.log(Level.INFO, "{0} {1} -> {2}",
       new Object[] { method, parameters, status });
    }
  }

  private void sendBadRequest(HttpServletResponse resp, String error) throws IOException
  {
    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    resp.setContentType("application/json");
    resp.getWriter().write("{\"error\":\"" + error + "\"}");
  }

  private void sendDecodeException(HttpServletResponse resp, String error) throws IOException
  {
    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    resp.setContentType("application/json");
    resp.getWriter().write("{\"error\":\"" + error + "\"}");
  }

  private JsonNode decodeJWTToken(String token) throws JsonProcessingException
  {
    String[] parts = token.split("\\.");

    String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

    ObjectMapper mapper = new ObjectMapper();
    return mapper.readTree(payloadJson);
  }

}
