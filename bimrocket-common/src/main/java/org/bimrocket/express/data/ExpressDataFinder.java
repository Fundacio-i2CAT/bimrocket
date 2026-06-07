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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import static org.bimrocket.express.data.ExpressCursor.CONTAINER;

/**
 *
 * @author realor
 */
public class ExpressDataFinder
{
  private final Function<ExpressCursor, Boolean> filter;
  private ExpressCursor cursor;
  private final Deque<Integer> stack = new ArrayDeque<>();
  private final Set<String> visited = new HashSet<>();

  public ExpressDataFinder()
  {
    this.filter = cur -> true;
  }

  public ExpressDataFinder(Function<ExpressCursor, Boolean> filter)
  {
    this.filter = filter;
  }

  public boolean find(ExpressCursor cursor)
  {
    this.cursor = cursor.copy();
    stack.clear();
    visited.clear();
    return next(false);
  }

  public boolean next(boolean skipChildren)
  {
    while (moveNext(skipChildren))
    {
      skipChildren = false;
      if (filter.apply(cursor)) return true;
    }
    return false;
  }

  public ExpressCursor cursor()
  {
    return cursor.copy(true);
  }

  boolean moveNext(boolean skipChildren)
  {
    int index;

    if (skipChildren)
    {
      if (cursor.getDepth() == 0) return false;

      cursor.exit();
      index = stack.pop();
      index++;
    }
    else
    {
      index = 0;
    }

    while (true)
    {
      while (index < cursor.size())
      {
        if (CONTAINER.equals(cursor.get(index)))
        {
          stack.push(index);
          cursor.enter(index);
          String id = cursor.getId();
          if (!visited.contains(id))
          {
            visited.add(id);
            return true;
          }
        }
        index++;
      }

      if (cursor.getDepth() == 0) return false;

      cursor.exit();
      index = stack.pop();
      index++;
    }
  }
}
