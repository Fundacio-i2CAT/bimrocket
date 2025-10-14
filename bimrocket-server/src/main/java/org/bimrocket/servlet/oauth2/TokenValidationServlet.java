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
import com.fasterxml.jackson.databind.ObjectWriter;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bimrocket.service.file.*;
import org.bimrocket.service.security.SecurityService;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
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

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
    String scheme = request.getScheme();               // Ex: http o https
    String contextPath = request.getContextPath();     // Ex: /bimrocket-server
    String hostHeader = request.getHeader("Host");  // Ex Host: localhost:9090
    String pathInfo = request.getPathInfo();

    String code = request.getParameter("code");
    if (code == null || code.isBlank())
    {
      sendBadRequest(response, "Missing code parameter");
      return;
    }

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

    InputStream responseStream = conn.getInputStream();
    String json = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);

    ObjectMapper mapper = new ObjectMapper();
    Object jsonObject = mapper.readValue(json, Object.class);
    ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
    String prettyJson = writer.writeValueAsString(jsonObject);

    response.setStatus(200);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    try (PrintWriter out = response.getWriter())
    {
      out.print(prettyJson);
    }


    /*String token = request.getParameter("token");
    if (token == null || token.isBlank())
    {
      sendBadRequest(response, "Missing token parameter");
      return;
    }

    logParameters(request, token);

    try {
      //Decode token
      JsonNode jsonNode = decodeJWTToken(token);
      String username = jsonNode.get("preferred_username").asText();
      String origin = jsonNode.get("iss").asText();
    }
    catch(Exception jp)
    {
      sendDecodeException(response, "Invalid Token");
      return;
    }*/

  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException
  {
    /*String scheme = request.getScheme();               // Ex: http o https
    String contextPath = request.getContextPath();     // Ex: /bimrocket-server
    String hostHeader = request.getHeader("Host");  // Ex Host: localhost:9090
    String pathInfo = request.getPathInfo();

    // Get code from the server authorization and call to obtain token
    if ("/authCode".equals(pathInfo))
    {
      System.out.println("authCode");
    }
    // Recevives token from the server authorization
    else if ("/tokenGen".equals(pathInfo))
    {
      System.out.println("tokenGen");
    }
    else
    {
      response.sendError(HttpServletResponse.SC_NOT_FOUND, "Path not found");
    }*/
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
