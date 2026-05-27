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
package org.bimrocket.api.security;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.NewCookie;
import org.eclipse.microprofile.config.Config;

/**
 *
 * @author realor
 */
@ApplicationScoped
public class SessionCookieManager
{
  public static final String SESSION_COOKIE = "SESSION_TOKEN";

  @Inject
  Config config;

  boolean secure;
  boolean httpOnly;
  String sameSite;

  @PostConstruct
  public void init()
  {
    String path = "services.security.cookie.";
    this.secure = config.getValue(path + "secure", Boolean.class);
    this.httpOnly = config.getValue(path + "httpOnly", Boolean.class);
    this.sameSite = config.getValue(path + "sameSite", String.class);
  }

  public NewCookie createCookie(String token)
  {
    NewCookie cookie = new NewCookie.Builder(SESSION_COOKIE)
      .value(token)
      .path("/")
      .secure(secure)
      .httpOnly(httpOnly)
      .sameSite(NewCookie.SameSite.valueOf(sameSite))
      .build();
    return cookie;
  }

  public String getDestroyCookieString()
  {
    return "%s=; Max-Age=0; Path=/; %sSameSite=%s"
      .formatted(SESSION_COOKIE, httpOnly ? "HttpOnly; " : "", sameSite);
  }
}
