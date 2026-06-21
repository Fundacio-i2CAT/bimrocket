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
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import org.bimrocket.console.annotation.Command;
import org.bimrocket.console.annotation.CommandLibrary;
import org.bimrocket.console.Library;
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
  private String branch;

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

    argInteger("list", "start", 0);
    argInteger("list", "count", 20);

    argInteger("find", "maxResults", 0);
    argInteger("find", "minDepth", 0);
    argInteger("find", "maxDepth", 1);

    argInteger("tree", "maxDepth", 1);
    argInteger("tree", "count", 10);

    argInteger("histogram", "maxDepth", 1);

    argString("find", "resultBranch", "output");
    argString("findRoots", "resultBranch", "roots");
  }

  @Command(
    name = "loadIFC",
    description = "Load an IFC file.",
    arguments = "filename:String")
  public void load(String filename) throws IOException
  {
    File file = getFile(filename);
    if (!file.exists() || !file.isFile())
      throw new IOException("Can't open file %s".formatted(file.getAbsolutePath()));
    double size = ((double)file.length()) / (1024L * 1024L);

    println("Loading from %s (%.2f MB)...".formatted(file.getAbsolutePath(), size));
    Chronometer chrono = new Chronometer();
    StepLoader loader = new StepLoader();
    loader.load(file);
    data(loader.getData());
    cursor(data().getCursor());
    double seconds = chrono.seconds();
    println("Load completed in " + seconds + " seconds.");

    this.branch = ExpressData.MAIN_BRANCH;
  }

  @Command(
    name = "exportIFC" ,
    description = "Export an IFC file.",
    arguments = "filename:String")
  public void export(String filename) throws IOException
  {
    if (cursor() == null)
    {
      throw new IOException("No file loaded.");
    }
    File file = getFile(filename);
    if (!file.getParentFile().exists())
    {
      if (!file.getParentFile().mkdirs())
        throw new IOException("Can't create folders.");
    }
    println("Exporting to " + file.getAbsolutePath() + "...");
    Chronometer chrono = new Chronometer();
    StepExporter exporter = new StepExporter(data());

    exporter.export(file, cursor());

    double seconds = chrono.seconds();
    println("Export completed in " + seconds + " seconds.");
  }

  public void branch()
  {
    branch(null);
  }

  @Command(
    description = "Move the cursor to the given branch.",
    arguments = "[branch:String]")
  public void branch(String branch)
  {
    ExpressData data = data();
    if (data ==  null)
    {
      throw new RuntimeException("No file loaded.");
    }

    if (branch != null)
    {
      this.branch = branch;
      cursor(data.getCursor(branch));
    }

    println("Current branch is " + this.branch);
  }

  @Command(
    description = "Print the data branch names.")
  public void branches()
  {
    if (data() == null)
    {
      throw new RuntimeException("No file loaded.");
    }

    Set<String> branchNames = data().getBranchNames();
    for (String branchName : branchNames)
    {
      String current = branchName.equals(branch) ? " (current)" : "";
      println("- %s%s".formatted(branchName, current));
    }
  }

  @Command(
    description = "Remove the specified branch.",
    arguments = "branch:String")
  public void removeBranch(String branch)
  {
    if (data() == null)
    {
      throw new RuntimeException("No file loaded.");
    }

    if (branch == null)
    {
      throw new RuntimeException("Invalid branch.");
    }

    if (branch.equals(this.branch))
    {
      throw new RuntimeException("Can't remove the current branch.");
    }

    data().removeBranch(branch);
  }

  public void histogram()
  {
    histogram(argInteger("histogram", "maxDepth", 1));
  }

  @Command(
    description = "Generate an IFC entity class histogram.",
    arguments = "[maxDepth:int]")
  public void histogram(int maxDepth)
  {
    if (data() == null)
    {
      throw new RuntimeException("No file loaded.");
    }

    HashMap<String, Integer> map = new HashMap<>();

    int total = ExpressDataFinder.create()
      .action(cur ->
      {
        ExpressType type = cur.getType();
        String typeName = type.getTypeName();
        Integer count = map.get(typeName);
        if (count == null) count = 0;
        count++;
        map.put(typeName, count);
        return true;
      })
      .minDepth(0)
      .maxDepth(maxDepth)
      .find(cursor());

    println("Entities found: " + total);

    ArrayList<String> keys = new ArrayList<>(map.keySet());
    Collections.sort(keys);
    for (String key : keys)
    {
      println(key + ": " + map.get(key));
    }
  }

  public void list()
  {
    list(argInteger("list", "start", 0));
  }

  public void list(int start)
  {
    list(start, argInteger("list", "count", 20));
  }

  @Command(
    description = "Print the objects referenced by the current cursor as a list.",
    arguments = "[start:int], [count:int]")
  public void list(int start, int count)
  {
    ExpressCursor cursor = cursor();
    if (cursor == null)
    {
      throw new RuntimeException("No file loaded.");
    }
    printCursor(cursor, start, start + count, 0);
  }

  public void tree()
  {
    tree(argInteger("tree", "maxDepth", 1));
  }

  public void tree(int maxDepth)
  {
    tree(maxDepth, argInteger("tree", "count", 10));
  }

  @Command(
    description = "Print the objects referenced by the current cursor as a tree.",
    arguments = "[maxDepth:int], [count:int]")
  public void tree(int maxDepth, int count)
  {
    ExpressCursor cursor = cursor();
    if (cursor == null)
    {
      throw new RuntimeException("No file loaded.");
    }
    cursor = cursor.copy();
    printCursor(cursor, 0, count, maxDepth);
  }

  @Command(
    description = "Move the cursor to the object at the specified index or name.",
    arguments = "index:int | name:String")
  public void enter(int index)
  {
    ExpressCursor cursor = cursor();
    if (cursor == null)
    {
      throw new RuntimeException("No file loaded.");
    }
    cursor.enter(index);
  }

  public void enter(String name)
  {
    ExpressCursor cursor = cursor();
    if (cursor == null)
    {
      throw new RuntimeException("No file loaded.");
    }
    cursor.enter(name);
  }

  @Command(
    description = "Move the cursor to the parent object.")
  public void exit()
  {
    ExpressCursor cursor = cursor();
    if (cursor == null)
    {
      throw new RuntimeException("No file loaded.");
    }
    if (cursor.getDepth() == 0)
    {
      throw new RuntimeException("Can't go parent.");
    }
    cursor.exit();
  }

  public void find(Predicate<ExpressCursor> filter)
  {
    find(filter, argString("find", "resultBranch", "output"));
  }

  @Command(
    description = "Find objects that meet the specified filter conditions.",
    arguments = "filter:function, [resultBranch:String]")
  public void find(Predicate<ExpressCursor> filter, String resultBranch)
  {
    if (data() == null)
    {
      throw new RuntimeException("No file loaded.");
    }

    data().removeBranch(resultBranch);

    ExpressCursor resultCursor = data().getCursor(resultBranch);

    int count = ExpressDataFinder.create()
      .filter(filter)
      .action(cur -> { resultCursor.add(cur); return true; })
      .minDepth(argInteger("find", "minDepth", 0))
      .maxDepth(argInteger("find", "maxDepth", 1))
      .maxResults(argInteger("find", "maxResults", 0))
      .find(cursor());

    println("Matches found: " + count);

    this.branch = resultBranch;
    cursor(data().getCursor(this.branch));
    println("Current branch is " + this.branch);
  }

  public void findRoots()
  {
    findRoots(argString("findRoots", "resultBranch", "roots"));
  }

  @Command(
    description = "Find root objects for the current branch.",
    arguments = "[resultBranch:String]")
  public void findRoots(String resultBranch)
  {
    if (data() == null)
    {
      throw new RuntimeException("No file loaded.");
    }

    data().removeBranch(resultBranch);

    Set<String> noRoots = new HashSet<>();

    ExpressDataFinder.create()
    .action(c ->
    {
      noRoots.add(c.getId());
      return true;
    })
    .minDepth(2)
    .maxDepth(0)
    .find(data().getCursor(branch));

    ExpressCursor resultCursor = data().getCursor(resultBranch);

    // extract roots
    ExpressCursor cur = data().getCursor(branch);
    for (int i = 0; i < cur.size(); i++)
    {
      cur.enter(i);
      if (cur.getType().isEntity())
      {
        if (!noRoots.contains(cur.getId())) resultCursor.add(cur);
      }
      cur.exit();
    }

    this.branch = resultBranch;
    cursor(data().getCursor(this.branch));
    println("Current branch is " + this.branch);
  }

  private void printCursor(ExpressCursor cursor,
    int start, int end, int maxDepth)
  {
    ExpressType type = cursor.getType();
    int depth = cursor.getDepth();
    String indent = "  ";
    int size = cursor.size();
    int thisStart = Math.min(start, size - 1);
    int thisEnd = Math.min(end, size - 1);

    for (int j = 0; j < depth; j++) print(indent);

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

    for (int i = thisStart; i <= thisEnd; i++)
    {
      for (int j = 0; j < depth; j++) print(indent);
      print("[" + i + "]");
      if (type instanceof ExpressEntity entity)
      {
        print(" ");
        print(entity.getAllAttributes().get(i).getName());
      }
      print(": ");
      Object value = cursor.get(i);
      if (CONTAINER.equals(value))
      {
        cursor.enter(i);
        if (depth < maxDepth)
        {
          println();
          printCursor(cursor, start, end, maxDepth);
        }
        else
        {
          print(cursor.getType().getTypeName());
          println("[...]");
        }
        cursor.exit();
      }
      else
      {
        println(value);
      }
    }
  }
}

