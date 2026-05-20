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
package org.bimrocket.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bimrocket.express.ExpressAttribute;
import org.bimrocket.express.ExpressCollection;
import org.bimrocket.express.ExpressEntity;
import org.bimrocket.express.ExpressType;
import org.bimrocket.express.data.ExpressCursor;
import static org.bimrocket.express.data.ExpressCursor.CONTAINER;
import org.bimrocket.express.data.ExpressData;
import org.bimrocket.step.io.StepExporter;
import org.bimrocket.step.io.StepLoader;

/**
 *
 * @author realor
 */
public class IfcProcessor
{
  static final String VERSION = "1.0";
  static final String PROMPT = "> ";
  static final String QUIT = "quit";
  static final String HELP = "help";

  ExpressData data;
  ExpressCursor cursor;
  Chronometer chrono = new Chronometer();
  HashMap<String, Command> commands = new HashMap<>();
  PrintStream out;
  Map<String, ExpressCursor> selection = new HashMap<>();

  public IfcProcessor()
  {
  }

  void registerCommand(Command command)
  {
    commands.put(command.getName(), command);
  }

  public void run()
  {
    out = System.out;

    registerCommand(new LoadCommand());
    registerCommand(new ExportCommand());
    registerCommand(new ListCommand());
    registerCommand(new EnterCommand());
    registerCommand(new ExitCommand());
    registerCommand(new RootCommand());
    registerCommand(new FindCommand());
    registerCommand(new SelectCommand());
    registerCommand(new UnselectCommand());
    registerCommand(new HistogramCommand());
    registerCommand(new HelpCommand());

    out.println("BIMROCKET IFC processor " + VERSION);

    List<String> jvmArgs =
      ManagementFactory.getRuntimeMXBean().getInputArguments();

    out.println("JVM flags:");
    jvmArgs.forEach(System.out::println);

    out.println("Type " + QUIT + " to exit or " +
      HELP + " to list the available commands.");

    BufferedReader reader =
      new BufferedReader(new InputStreamReader(System.in));

    while (true)
    {
      try
      {
        out.print(PROMPT);
        String line = reader.readLine();
        if (QUIT.equals(line)) break;

        List<String> tokens = Arrays.asList(line.split("\\s+"));
        if (tokens.isEmpty()) continue;

        String commandName = tokens.get(0).trim();
        if (commandName.length() == 0) continue;

        Command command = commands.get(commandName);
        if (command == null)
        {
          out.println("Invalid command.");
          continue;
        }

        List<String> commandArgs = tokens.subList(1, tokens.size());
        command.execute(commandArgs);
      }
      catch (Exception ex)
      {
        out.println(ex.toString());
      }
    }

  }

  public static void main(String[] args) throws Exception
  {
    IfcProcessor processor = new IfcProcessor();
    processor.run();
  }

  abstract class Command
  {
    abstract String getName();

    String getHelp()
    {
      return getName();
    }

    abstract void execute(List<String> args) throws Exception;
  }

  class LoadCommand extends Command
  {
    @Override
    String getName()
    {
      return "load";
    }

    @Override
    String getHelp()
    {
      return "load <filename>";
    }

    @Override
    void execute(List<String> args)  throws Exception
    {
      if (args.isEmpty())
      {
        out.println("Missing file argument.");
        return;
      }

      String filename = args.get(0);
      out.println("Loading from " + filename + "...");
      chrono.reset();
      StepLoader loader = new StepLoader();
      loader.load(filename);
      data = loader.getData();
      cursor = data.getRoot();
      double seconds = chrono.seconds();
      out.println("Load completed in " + seconds + " seconds.");
    }
  }

  class ExportCommand extends Command
  {
    @Override
    String getName()
    {
      return "export";
    }

    @Override
    String getHelp()
    {
      return "export <filename>";
    }

    @Override
    public void execute(List<String> args) throws Exception
    {
      if (data == null)
      {
        out.println("No file loaded.");
        return;
      }

      if (args.isEmpty())
      {
        out.println("Missing file argument.");
        return;
      }

      String filename = args.get(0);
      out.println("Exporting to " + filename + "...");
      chrono.reset();
      StepExporter exporter = new StepExporter(data);
      exporter.export(filename, selection.values());
      double seconds = chrono.seconds();
      out.println("Export completed in " + seconds + " seconds.");
    }
  }

  class HistogramCommand extends Command
  {
    @Override
    public String getName()
    {
      return "histogram";
    }

    @Override
    public void execute(List<String> args)
    {
      if (data == null)
      {
        out.println("No file loaded.");
        return;
      }

      HashMap<String, Integer> map = new HashMap<>();
      ExpressCursor cursor = data.getRoot();
      int size = cursor.size();
      for (int i = 0; i < size; i++)
      {
        cursor.enter(i);
        String typeName = cursor.getType().getTypeName();
        Integer count = map.get(typeName);
        if (count == null) count = 0;
        count++;
        map.put(typeName, count);
        cursor.exit();
      }
      ArrayList<String> keys = new ArrayList<>(map.keySet());
      Collections.sort(keys);
      for (String key : keys)
      {
        out.println(key + ": " + map.get(key));
      }
    }
  }

  class FindCommand extends Command
  {
    Set<String> visited = new HashSet<>();
    String attributeName = "";
    String pattern = "";
    ExpressCursor lastCursor;
    int lastIndex;

    @Override
    public String getName()
    {
      return "find";
    }

    @Override
    public String getHelp()
    {
      return "find [class|<attribute>] [<value>]";
    }

    @Override
    public void execute(List<String> args)
    {
      visited.clear();

      boolean found;
      if (args.isEmpty() && lastCursor != null)
      {
        cursor = lastCursor;
        found = findChildren(lastIndex + 1);
      }
      else if (!args.isEmpty())
      {
        attributeName = args.get(0);
        pattern = args.size() >= 2 ? args.get(1) : "";
        lastCursor = null;
        lastIndex = 0;
        found = find();
      }
      else
      {
        out.println("No search criteria.");
        return;
      }

      if (found)
      {
        out.println("Match found: #" + cursor.getId());
      }
      else
      {
        out.println("Not found.");
      }
    }

    boolean find()
    {
      String id = cursor.getId();
      if (visited.contains(id)) return false;

      ExpressType type = cursor.getType();
      if ("class".equals(attributeName))
      {
        if (type.getTypeName().equalsIgnoreCase(pattern)) return true;
      }
      else if (type instanceof ExpressEntity entity)
      {
        ExpressAttribute attribute = entity.getAttribute(attributeName);
        if (attribute != null)
        {
          Object value = cursor.get(attributeName);
          String valueString = value == null ? "" : value.toString();
          if (valueString.contains(pattern)) return true;
        }
      }

      visited.add(id);

      return findChildren(0);
    }

    boolean findChildren(int index)
    {
      int size = cursor.size();
      for (int i = index; i < size; i++)
      {
        Object value = cursor.get(i);
        if (CONTAINER.equals(value))
        {
          cursor.enter(i);
          if (find())
          {
            cursor.exit();
            lastCursor = cursor.copy();
            lastIndex = i;
            cursor.enter(i);
            return true;
          }
          cursor.exit();
        }
      }
      return false;
    }
  }

  class ListCommand extends Command
  {
    @Override
    String getName()
    {
      return "list";
    }

    @Override
    String getHelp()
    {
      return "list [<start=0>] [<count=20>]";
    }

    @Override
    void execute(List<String> args) throws Exception
    {
      if (cursor == null)
      {
        out.println("No file loaded.");
        return;
      }
      int start = 0;
      int count = 20;

      if (args.size() >= 1) start = Integer.parseInt(args.get(0));
      if (args.size() >= 2) count = Integer.parseInt(args.get(1));

      ExpressType type = cursor.getType();
      String id = cursor.getId();
      if (id != null)
      {
        out.print("#" + cursor.getId() + " ");
      }
      out.print(type.getTypeName());
      if (type instanceof ExpressCollection)
      {
        out.print("[" + cursor.size() + "]");
      }
      out.println(":");
      int end = Math.min(start + count, cursor.size());
      for (int i = start; i < end; i++)
      {
        if (type instanceof ExpressEntity entity)
        {
          out.print(i + " ");
          out.print(entity.getAllAttributes().get(i).getName());
        }
        else
        {
          out.print(i);
        }
        out.print(": ");
        Object value = cursor.get(i);
        if (CONTAINER.equals(value))
        {
          cursor.enter(i);
          out.print(cursor.getType().getTypeName());
          out.println("[...]");
          cursor.exit();
        }
        else
        {
          out.println(value);
        }
      }
    }
  }

  class EnterCommand extends Command
  {
    @Override
    String getName()
    {
      return "enter";
    }

    @Override
    String getHelp()
    {
      return "enter <name|index>";
    }

    @Override
    public void execute(List<String> args) throws Exception
    {
      if (cursor == null)
      {
        out.println("No file loaded.");
        return;
      }

      String selector = args.get(0);
      if (Character.isDigit(selector.charAt(0)))
      {
        Integer index = Integer.valueOf(selector);
        cursor.enter(index);
      }
      else
      {
        cursor.enter(selector);
      }
    }
  }

  class ExitCommand extends Command
  {
    @Override
    String getName()
    {
      return "exit";
    }

    @Override
    public void execute(List<String> args) throws Exception
    {
      if (cursor == null)
      {
        out.println("No file loaded.");
        return;
      }

      cursor.exit();
    }
  }

  class RootCommand extends Command
  {
    @Override
    String getName()
    {
      return "root";
    }

    @Override
    public void execute(List<String> args) throws Exception
    {
      if (data == null)
      {
        out.println("No file loaded.");
        return;
      }

      cursor = data.getRoot();
    }
  }

  class SelectCommand extends Command
  {
    @Override
    String getName()
    {
      return "select";
    }

    @Override
    public void execute(List<String> args) throws Exception
    {
      if (cursor == null)
      {
        out.println("No file loaded.");
        return;
      }

      if (!(cursor.getType() instanceof ExpressEntity))
      {
        out.println("Not an entity.");
        return;
      }

      String id = cursor.getId();
      selection.put(id, cursor);
      out.println("Entity #" + id + " selected.");
    }
  }

  class UnselectCommand extends Command
  {
    @Override
    String getName()
    {
      return "unselect";
    }

    @Override
    String getHelp()
    {
      return "unselect [all]";
    }

    @Override
    public void execute(List<String> args) throws Exception
    {
      if (cursor == null)
      {
        out.println("No file loaded.");
        return;
      }

      if (args.size() == 1 && args.get(0).equals("all"))
      {
        selection.clear();
        out.println("Selection cleared.");
      }
      else
      {
        String id = cursor.getId();
        if (selection.containsKey(id))
        {
          selection.remove(id);
          out.println("Entity #" + id + " unselected.");
        }
      }
    }
  }

  class HelpCommand extends Command
  {
    @Override
    String getName()
    {
      return HELP;
    }

    @Override
    void execute(List<String> args) throws Exception
    {
      ArrayList<String> commandNames = new ArrayList<>(commands.keySet());
      Collections.sort(commandNames);

      for (String commandName : commandNames)
      {
        Command command = commands.get(commandName);
        out.println("-" + command.getHelp());
      }
    }
  }
}
