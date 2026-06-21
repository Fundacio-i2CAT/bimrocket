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
package org.bimrocket.express.data;


import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import org.bimrocket.express.ExpressEntity;
import static org.bimrocket.express.data.ExpressCursor.CONTAINER;

/**
 *
 * @author realor
 */
public class ExpressDataFinder
{
  private Predicate<ExpressCursor> filter;
  private Predicate<ExpressCursor> action;
  private int maxResults = 0;
  private int minDepth = 0;
  private int maxDepth = 0;

  private ExpressCursor cursor;
  private final Set<String> visited = new HashSet<>();
  private int count;
  private boolean finding;

  ExpressDataFinder()
  {
    this.filter = cur -> true;
    this.action = cur -> true;
  }

  public static ExpressDataFinder create()
  {
    return new ExpressDataFinder();
  }

  public ExpressDataFinder filter(Predicate<ExpressCursor> filter)
  {
    this.filter = filter;
    return this;
  }

  public ExpressDataFinder action(Predicate<ExpressCursor> action)
  {
    this.action = action;
    return this;
  }

  public ExpressDataFinder maxResults(int maxResults)
  {
    this.maxResults = maxResults;
    return this;
  }

  public ExpressDataFinder minDepth(int minDepth)
  {
    this.minDepth = minDepth;
    return this;
  }

  public ExpressDataFinder maxDepth(int maxDepth)
  {
    this.maxDepth = maxDepth;
    return this;
  }

  public int find(ExpressCursor cursor)
  {
    visited.clear();

    this.cursor = cursor.copy();

    finding = true;

    this.find();

    return count;
  }

  private void find()
  {
    if (maxResults > 0 && count >= maxResults)
    {
      finding = false;
      return;
    }

    int depth = cursor.getDepth();

    if (maxDepth > 0 && depth > maxDepth) return;

    if (cursor.getType() instanceof ExpressEntity && depth >= minDepth)
    {
      String id = cursor.getId();
      if (visited.contains(id)) return;

      visited.add(id);

      if (filter.test(cursor))
      {
        count++;

        finding = action.test(cursor);
      }
    }

    if (!finding) return;

    // explore children
    for (int i = 0; i < cursor.size(); i++)
    {
      if (CONTAINER.equals(cursor.get(i)))
      {
        cursor.enter(i);
        find();
        cursor.exit();
      }
    }
  }
}
