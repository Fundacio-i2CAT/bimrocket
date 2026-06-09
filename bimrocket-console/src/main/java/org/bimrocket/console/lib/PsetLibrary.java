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
package org.bimrocket.console.lib;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.bimrocket.console.annotation.Command;
import org.bimrocket.console.annotation.CommandLibrary;
import org.bimrocket.console.Library;
import org.bimrocket.console.Status;
import org.bimrocket.express.data.ExpressCursor;
import org.bimrocket.express.data.ExpressData;
import org.bimrocket.express.data.ExpressDataFinder;
import org.graalvm.polyglot.Context;

/**
 *
 * @author realor
 */
@CommandLibrary(
  name = "pset",
  description = "IFC Pset transform")
public class PsetLibrary extends Library
{
  Map<String, Rule> rules = new HashMap<>();

  public PsetLibrary(Context context, PrintStream out)
  {
    super(context, out);
  }

  public Status addPsetRule(
    String pset,
    String property,
    String newName)
  {
    return addPsetRule(pset, property, newName, null);
  }

  @Command(
    description = "Add a pset transform rule.",
    parameters = "pset:string, property:string, newName:string, newValue:function")
  public Status addPsetRule(
    String pset,
    String property,
    String newName,
    Function<Object, Object> newValue)
  {
    pset = pset == null ? "" : pset.trim();
    property = property == null ? "" : property.trim();

    if (pset.length() == 0 && property.length() == 0)
    {
      return new Status(1, "pset or property can not be blank.");
    }

    if (newName == null || newName.trim().length() == 0)
    {
      return new Status(1, "newName can not be blank.");
    }

    String path = pset + "/" + property;
    rules.put(path, new Rule(pset, property, newName, newValue));

    return Status.OK;
  }

  @Command(
    description = "Clear all pset transform rules.")
  public Status clearPsetRules()
  {
    rules.clear();
    return Status.OK;
  }

  @Command(
    description = "Transform all psets with the current rules."
  )
  public Status transformPsets()
  {
    ExpressData data = data();
    if (data == null)
    {
      return new Status(1, "No file loaded.");
    }

    Function<ExpressCursor, Boolean> filter = cursor ->
    {
      return cursor.isTypeName("IfcPropertySet");
    };

    ExpressDataFinder finder = new ExpressDataFinder(filter);
    boolean found = finder.find(data().getRoot());
    while (found)
    {
      ExpressCursor cursor = finder.cursor();
      String psetName = cursor.get("Name");

      // rename Pset
      Rule psetRule = rules.get(psetName + "/");
      if (psetRule != null)
      {
        cursor.set("Name", psetRule.newName);
        println("%s: rename %s to %s"
          .formatted(cursor.getId(), psetName, psetRule.newName));
      }

      // rename Properties
      cursor.enter("HasProperties");
      for (int i = 0; i < cursor.size(); i++)
      {
        cursor.enter(i);
        String propName = cursor.get("Name");

        Rule propRule = rules.get(psetName + "/" + propName);
        if (propRule == null) propRule = rules.get("/" + propName);

        if (propRule != null)
        {
          cursor.set("Name", propRule.newName);
          println("%s: rename %s.%s to %s"
            .formatted(cursor.getId(), psetName, propName, propRule.newName));
        }
        cursor.exit();
      }
      cursor.exit();

      found = finder.next(false);
    }
    return Status.OK;
  }

  class Rule
  {
    final String pset;
    final String property;
    final String newName;
    final Function<Object, Object> newValue;

    Rule(String pset, String property,
      String newName, Function<Object, Object> newValue)
    {
      this.pset = pset;
      this.property = property;
      this.newName = newName;
      this.newValue = newValue;
    }
  }
}
