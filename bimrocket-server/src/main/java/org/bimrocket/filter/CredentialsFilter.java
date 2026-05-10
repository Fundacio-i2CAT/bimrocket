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
package org.bimrocket.filter;

import jakarta.inject.Inject;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.bimrocket.service.security.Credentials;
import static org.bimrocket.service.security.Credentials.COOKIE;
import static org.bimrocket.api.security.SessionCookieManager.SESSION_COOKIE;
import org.bimrocket.service.security.SecurityService;

/**
 *
 * @author realor
 */
@WebFilter("/api/*")
public class CredentialsFilter extends HttpFilter
{
  private static final long serialVersionUID = 1L;

  @Inject
  transient SecurityService securityService;

  @Override
  public void doFilter(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain chain)
    throws IOException, ServletException
  {
    Credentials credentials = null;

    String authorization = request.getHeader("Authorization");
    if (authorization != null)
    {
      credentials = fromAuthorization(authorization);
    }
    else
    {
      Cookie[] cookies = request.getCookies();
      if (cookies != null)
      {
        for (Cookie cookie : cookies)
        {
          if (SESSION_COOKIE.equals(cookie.getName()))
          {
            credentials = fromCookie(cookie);
          }
        }
      }
    }
    securityService.setCredentials(credentials);

    chain.doFilter(request, response);
  }

  private Credentials fromAuthorization(String authorization)
  {
    String type;
    String value;

    authorization = authorization.trim();
    int index = authorization.indexOf(' ');
    if (index != -1)
    {
      type = authorization.substring(0, index).toLowerCase();
      value = authorization.substring(index + 1).trim();
    }
    else
    {
      type = "";
      value = authorization;
    }
    return new Credentials(type, value);
  }

  private Credentials fromCookie(Cookie cookie)
  {
    return new Credentials(COOKIE, cookie.getValue());
  }
}
