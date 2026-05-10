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
package org.bimrocket.api.security.oauth2;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.Config;


/**
 *
 * @author realor
 */
public class OAuth2ProviderRegistry
{
  private Map<String, OAuth2Provider> providerMap;
  private  List<OAuth2Provider> providerList;

  @Inject
  Instance<OAuth2Provider> providers;

  @Inject
  Config config;

  @PostConstruct
  void init()
  {
    List<String> active = config.getOptionalValues(
      "services.security.oauth2.active", String.class)
      .orElse(Collections.emptyList());

    providerMap = providers.stream()
      .filter(provider -> active.contains(provider.getName()))
      .collect(Collectors.toMap(
        OAuth2Provider::getName,
        Function.identity()
      ));

    List<OAuth2Provider> list = new ArrayList<>();
    for (String providerName : active)
    {
      list.add(providerMap.get(providerName));
    }

    providerList = Collections.unmodifiableList(list);
  }

  public List<OAuth2Provider> getAll()
  {
    return providerList;
  }

  public OAuth2Provider get(String name)
  {
    OAuth2Provider provider = providerMap.get(name);
    if (provider == null)
    {
      throw new IllegalArgumentException("Provider not enabled: " + name);
    }
    return provider;
  }
}
