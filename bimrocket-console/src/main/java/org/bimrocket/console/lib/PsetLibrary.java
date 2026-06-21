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
import java.util.function.Consumer;
import org.bimrocket.console.annotation.Command;
import org.bimrocket.console.annotation.CommandLibrary;
import org.bimrocket.console.Library;
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

  public void addPsetRule(
    String pset,
    String property,
    String newName)
  {
    addPsetRule(pset, property, newName, null);
  }

  @Command(
    description = "Add a pset transform rule.",
    arguments = "pset:String, property:String, newName:String, updater:function")
  public void addPsetRule(
    String pset,
    String property,
    String newName,
    Consumer<ExpressCursor> updater)
  {
    pset = pset == null ? "" : pset.trim();
    property = property == null ? "" : property.trim();
    newName = newName == null ? "" : newName.trim();

    if (pset.length() == 0 && property.length() == 0)
    {
      throw new RuntimeException("pset or property are required.");
    }

    if (newName.length() == 0 && updater == null)
    {
      throw new RuntimeException("newName or updater are required.");
    }

    String path = pset + "/" + property;
    rules.put(path, new Rule(pset, property, newName, updater));
  }

  @Command(
    description = "Clear all pset transform rules.")
  public void clearPsetRules()
  {
    rules.clear();
  }

  @Command(
    description = "Transform all psets with the current rules."
  )
  public void transformPsets()
  {
    ExpressData data = data();
    if (data == null)
    {
      throw new RuntimeException("No file loaded.");
    }

    ExpressDataFinder.create()
      .filter(cur -> cur.is("IfcPropertySet"))
      .action(cur ->
      {
        String globalId = cur.get("GlobalId");
        String psetName = cur.get("Name");

        // rename Pset
        Rule psetRule = rules.get(psetName + "/");
        if (psetRule != null && psetRule.newName != null)
        {
          cur.set("Name", psetRule.newName);
          println("%s: rename %s to %s"
            .formatted(cur.getId(), psetName, psetRule.newName));
        }

        // rename Properties
        cur.enter("HasProperties");
        for (int i = 0; i < cur.size(); i++)
        {
          cur.enter(i);

          if (cur.is("IfcPropertySingleValue"))
          {
            String propName = cur.get("Name");

            Rule propRule = rules.get(psetName + "/" + propName);
            if (propRule == null) propRule = rules.get("/" + propName);

            if (propRule != null)
            {
              if (propRule.newName != null)
              {
                cur.set("Name", propRule.newName);
                println("%s: rename %s.%s to %s"
                  .formatted(globalId, psetName, propName, propRule.newName));
              }

              if (propRule.updater != null)
              {
                propRule.updater.accept(cur);
                println("%s: update %s.%s"
                  .formatted(globalId, psetName, propName));
              }
            }
          }
          cur.exit();
        }
        cur.exit();

        return true;
      })
      .minDepth(0)
      .maxDepth(0)
      .find(cursor());
  }

  class Rule
  {
    final String pset;
    final String property;
    final String newName;
    final Consumer<ExpressCursor> updater;

    Rule(String pset, String property,
      String newName, Consumer<ExpressCursor> updater)
    {
      this.pset = pset;
      this.property = property;
      this.newName = newName;
      this.updater = updater;
    }
  }
}
