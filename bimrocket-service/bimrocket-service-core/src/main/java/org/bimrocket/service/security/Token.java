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
package org.bimrocket.service.security;

import java.util.Date;

import org.bimrocket.util.TextUtils;
import org.bson.codecs.pojo.annotations.BsonId;

import jakarta.persistence.Id;

/**
 *
 * @author realor
 */
public class Token
{
  @Id
  @BsonId
  private String id;
  private String userId;
  private String creationDate;
  private String expirationDate;

  static Token create(String id, String userId, int tokenTimeout)
  {
    Token token = new Token();
    token.id = id;
    token.userId = userId;
    token.creationDate = TextUtils.getISODate();
    token.updateExpirationDate(tokenTimeout);
    return token;
  }

  public String getId()
  {
    return id;
  }

  public void setId(String id)
  {
    this.id = id;
  }

  public String getUserId()
  {
    return userId;
  }

  public void setUserId(String userId)
  {
    this.userId = userId;
  }

  public String getCreationDate()
  {
    return creationDate;
  }

  public void setCreationDate(String creationDate)
  {
    this.creationDate = creationDate;
  }

  public String getExpirationDate()
  {
    return expirationDate;
  }

  public void setExpirationDate(String expirationDate)
  {
    this.expirationDate = expirationDate;
  }

  public void updateExpirationDate(int timeout)
  {
    long expirationTime = System.currentTimeMillis() + 1000L * timeout;
    this.expirationDate = TextUtils.getISODate(new Date(expirationTime));
  }
}
