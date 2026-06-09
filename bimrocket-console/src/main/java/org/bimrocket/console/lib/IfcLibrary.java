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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.function.Function;
import org.bimrocket.console.annotation.Command;
import org.bimrocket.console.annotation.CommandLibrary;
import org.bimrocket.console.Library;
import org.bimrocket.console.Status;
import org.bimrocket.express.ExpressCollection;
import org.bimrocket.express.ExpressEntity;
import org.bimrocket.express.ExpressType;
import org.bimrocket.express.data.ExpressCursor;
import static org.bimrocket.express.data.ExpressCursor.CONTAINER;
import org.bimrocket.express.data.ExpressData;
import org.bimrocket.express.data.ExpressDataFinder;
import org.bimrocket.step.io.StepExporter;
import org.bimrocket.step.io.StepLoader;
import org.bimrocket.util.Chronometer;
import org.graalvm.polyglot.Context;

/**
 *
 * @author realor
 */
@CommandLibrary(
  name = "ifc",
  description = "IFC")
public class IfcLibrary extends Library
{
  ExpressDataFinder finder = null;

  public IfcLibrary(Context context, PrintStream out)
  {
    super(context, out);
  }

  @Override
  public void init()
  {
    super.init();

    context.eval("js",
      "const C = (value) => new org.bimrocket.express.ExpressConstant(value)");
  }

  @Command(
    name = "loadIFC",
    description = "Load an IFC file.",
    parameters = "ifc_file:string")
  public Status load(String filename) throws IOException
  {
    File file = new File(filename);
    if (!file.exists() || !file.isFile())
      return new Status(1, "Can't open file");
    double size = ((double)file.length()) / (1024L * 1024L);

    println("Loading from %s (%.2f MB)...".formatted(filename, size));
    Chronometer chrono = new Chronometer();
    StepLoader loader = new StepLoader();
    loader.load(filename);
    data(loader.getData());
    cursor(data().getRoot());
    double seconds = chrono.seconds();
    println("Load completed in " + seconds + " seconds.");

    return Status.OK;
  }

  @Command(
    name = "exportIFC" ,
    description = "Export an IFC file.",
    parameters = "ifc_file:string")
  public Status export(String filename) throws IOException
  {
    ExpressCursor cursor = cursor();

    if (cursor == null)
    {
      return new Status(1, "No file loaded.");
    }
    println("Exporting to " + filename + "...");
    Chronometer chrono = new Chronometer();
    StepExporter exporter = new StepExporter(data());
    exporter.export(filename, selection().values());
    double seconds = chrono.seconds();
    println("Export completed in " + seconds + " seconds.");
    return Status.OK;
  }

  @Command(
    description = "Generate an IFC class histogram.")
  public Status histogram()
  {
    ExpressCursor cursor = cursor();
    ExpressData data = data();

    if (cursor == null)
    {
      return new Status(1, "No file loaded.");
    }
    HashMap<String, Integer> map = new HashMap<>();
    ExpressCursor cur = data.getRoot();
    int size = cur.size();
    for (int i = 0; i < size; i++)
    {
      cur.enter(i);
      String typeName = cur.getType().getTypeName();
      Integer count = map.get(typeName);
      if (count == null) count = 0;
      count++;
      map.put(typeName, count);
      cur.exit();
    }
    ArrayList<String> keys = new ArrayList<>(map.keySet());
    Collections.sort(keys);
    for (String key : keys)
    {
      println(key + ": " + map.get(key));
    }
    return Status.OK;
  }

  public Status list()
  {
    return list(0);
  }

  public Status list(int start)
  {
    return list(start, 20);
  }

  @Command(
    description = "List the items of the current object.",
    parameters = "[start=0], [count=20]")
  public Status list(int start, int count)
  {
    if (cursor() == null)
    {
      return new Status(1, "No file loaded.");
    }
    ExpressCursor cursor = cursor();

    ExpressType type = cursor.getType();
    String id = cursor.getId();
    if (id != null)
    {
      print("#" + cursor.getId() + " ");
    }
    print(type.getTypeName());
    if (type instanceof ExpressCollection)
    {
      print("[" + cursor.size() + "]");
    }
    println(":");
    int end = Math.min(start + count, cursor.size());
    for (int i = start; i < end; i++)
    {
      if (type instanceof ExpressEntity entity)
      {
        print(i + " ");
        print(entity.getAllAttributes().get(i).getName());
      }
      else
      {
        print(i);
      }
      print(": ");
      Object value = cursor.get(i);
      if (CONTAINER.equals(value))
      {
        cursor.enter(i);
        print(cursor().getType().getTypeName());
        println("[...]");
        cursor.exit();
      }
      else
      {
        println(value);
      }
    }
    return Status.OK;
  }

  @Command(
    description = "Move the cursor to the object at the specified index or name.",
    parameters = "index:number | name:string")
  public Status enter(int index)
  {
    ExpressCursor cursor = cursor();
    if (cursor == null)
    {
      return new Status(1, "No file loaded.");
    }
    cursor.enter(index);
    return Status.OK;
  }

  public Status enter(String name)
  {
    ExpressCursor cursor = cursor();
    if (cursor == null)
    {
      return new Status(1, "No file loaded.");
    }
    cursor.enter(name);
    return Status.OK;
  }

  @Command(
    description = "Move the cursor to the parent object.")
  public Status exit()
  {
    ExpressCursor cursor = cursor();
    if (cursor == null)
    {
      return new Status(1, "No file loaded.");
    }
    if (cursor.getDepth() == 0)
    {
      return new Status(1, "Can't go parent.");
    }
    cursor.exit();
    return Status.OK;
  }

  @Command(
    description = "Move the cursor to the root object.")
  public Status root()
  {
    ExpressCursor cursor = cursor();
    if (cursor == null)
    {
      return new Status(1, "No file loaded.");
    }
    cursor(data().getRoot());
    return Status.OK;
  }

  @Command(
    description = "Select the object referenced by the cursor.")
  public Status select()
  {
    ExpressCursor cursor = cursor();
    if (cursor == null)
    {
      return new Status(1, "No file loaded.");
    }

    if (!(cursor.getType() instanceof ExpressEntity))
    {
      return new Status(1, "Not an entity.");
    }

    String id = cursor.getId();
    selection().put(id, cursor);
    println("Entity #" + id + " selected.");
    return Status.OK;
  }

  public Status unselect()
  {
    return unselect(false);
  }

  @Command(
    description = "Unselect the current object or all selected objects.",
    parameters = "allObjects:boolean")
  public Status unselect(boolean all)
  {
    if (all)
    {
      selection().clear();
      println(("Selection cleared."));
      return Status.OK;
    }

    ExpressCursor cursor = cursor();
    if (cursor == null)
    {
      return new Status(1, "No file loaded.");
    }

    String id = cursor.getId();
    selection().remove(id);
    return Status.OK;
  }

  @Command(
    description = "Find objects that meet the specified criteria.",
    parameters = "criteria:function")
  public Status find(Function<ExpressCursor, Boolean> filter)
  {
    finder = new ExpressDataFinder(filter);
    if (finder.find(data().getRoot()))
    {
      cursor(finder.cursor());
      println("Match found: #" + cursor().getId());
    }
    else
    {
      println("Not found.");
      finder = null;
    }
    return Status.OK;
  }

  public Status next()
  {
    return next(false);
  }

  @Command(
    description = "Find the next object that meets the specified criteria.",
    parameters = "[skipChildren:false]")
  public Status next(boolean skipChildren)
  {
    if (finder == null)
    {
      return new Status(1, "Call find first.");
    }

    if (finder.next(skipChildren))
    {
      cursor(finder.cursor());
      println("Match found: #" + cursor().getId());
    }
    else
    {
      println("Not found.");
      finder = null;
    }
    return Status.OK;
  }
}
