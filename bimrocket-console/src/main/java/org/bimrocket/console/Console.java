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
package org.bimrocket.console;

import org.bimrocket.console.annotation.Command;
import org.bimrocket.console.lib.IfcLibrary;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bimrocket.console.lib.PsetLibrary;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 *
 * @author realor
 */
public class Console
{
  static final String VERSION = "BIMROCKET Console 1.0";
  static final String PROMPT_SINGLE = "> ";
  static final String PROMPT_MULTIPLE = ">>> ";
  static final String MULTILINE = "...";
  static final String QUIT = ":quit";
  static final String HELP = ":help";

  private static final List<Class<? extends Library>> libraryClasses = new ArrayList<>();

  public static void main(String[] args)
  {
    System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
    libraryClasses.add(IfcLibrary.class);
    libraryClasses.add(PsetLibrary.class);

    Console console = new Console();
    if (args.length == 0)
    {
      console.run();
    }
    else
    {
      console.runFile(args[0]);
    }
  }

  public void runFile(String filename)
  {
    try
    {
      String code = Files.readString(Path.of(filename));
      try (Context context = createContext())
      {
        context.eval("js", code);
      }
    }
    catch (Exception ex)
    {
      System.err.println("Error: " + ex.getMessage());
    }
  }

  public void run()
  {
    try (Context context = createContext())
    {
      boolean multiLine = false;

      Set<String> commands = getCommands();

      Terminal terminal = TerminalBuilder.builder()
        .system(true)
        .build();

      LineReader reader = LineReaderBuilder.builder()
        .terminal(terminal)
        .parser(new DefaultParser())
        .completer(new StringsCompleter(getCommands()))
        .build();

      PrintStream out = System.out;

      out.println(VERSION);
      out.println("Type " + HELP + " to show help.");
      out.println("Type " + MULTILINE + " to enter multiline mode.");
      out.println("Type " + QUIT + " to quit program.");

      StringBuilder buffer = new StringBuilder();
      while (true)
      {
        String prompt = multiLine ? PROMPT_MULTIPLE : PROMPT_SINGLE;
        String line = reader.readLine(prompt);

        line = line.trim();

        if (line.equals(QUIT))
        {
          break;
        }

        if (line.startsWith(HELP))
        {
          String cmd = line.substring(HELP.length()).trim();
          showHelp(context, cmd);
          continue;
        }

        if (line.equals(MULTILINE))
        {
          multiLine = true;
          continue;
        }

        String command = null;

        if (multiLine)
        {
          if (line.length() == 0) // execute multiLine
          {
            command = buffer.toString();
            buffer.setLength(0);
            multiLine = false;
          }
          else
          {
            buffer.append(line).append("\n");
          }
        }
        else
        {
          command = completeCommand(line, commands);
        }

        try
        {
          if (command != null && command.length() > 0)
          {
            Value result = context.eval("js", command);
            if (result.isHostObject())
            {
              Object object = result.asHostObject();
              if (object instanceof Status status)
              {
                if (status.getCode() > 0)
                {
                  out.println(status.getMessage());
                }
                continue;
              }
            }
            out.println(result);
          }
        }
        catch (PolyglotException ex)
        {
          out.println("Error: " + ex.getMessage());
        }
      }
    }
    catch (Exception ex)
    {
      System.out.println(ex);
    }
  }

  Context createContext() throws Exception
  {
    Context context = Context.newBuilder("js")
      .allowAllAccess(true)
      .allowHostAccess(HostAccess.ALL)
      .allowHostClassLookup(s -> true)
      .build();

    for (Class<? extends Library> cls : libraryClasses)
    {
      Constructor<? extends Library> constructor =
        cls.getConstructor(Context.class, PrintStream.class);
      Library baseLibrary = constructor.newInstance(context, System.out);
      baseLibrary.init();
    }
    return context;
  }

  String completeCommand(String line, Set<String> commands)
  {
    // transform commands like this: <command> [number|string]*
    // to JS syntax

    int index = line.indexOf(" ");
    String command = index == -1 ? line : line.substring(0, index);
    if (!commands.contains(command)) return line;

    StringBuilder buffer = new StringBuilder();
    buffer.append(command);
    buffer.append("(");
    boolean inArg = false;
    int quote = -1;
    int argCount = 0;

    for (int i = command.length(); i <= line.length(); i++)
    {
      char ch = i == line.length() ? 0 : line.charAt(i);

      if (inArg)
      {
        if (quote != -1)
        {
          if (ch == 0) // unterminated string
          {
            ch = (char)quote;
          }
          buffer.append(ch);
          if (ch == (char)quote)
          {
            inArg = false;
            quote = -1;
          }
        }
        else
        {
          if (ch == 0)
          {
            // skip
          }
          else if (Character.isDigit(ch))
          {
            buffer.append(ch);
          }
          else if (ch == ' ')
          {
            inArg = false;
          }
          else return line;
        }
      }
      else // not inArg
      {
        if (ch == 0 || ch == ' ')
        {
          // skip
        }
        else if (Character.isDigit(ch) || ch == '"' || ch == '\'')
        {
          if (argCount > 0)
          {
            buffer.append(", ");
          }
          buffer.append(ch);
          inArg = true;
          argCount++;

          if (ch == '"' || ch == '\'')
          {
            quote = ch;
          }
        }
        else return line;
      }
    }
    buffer.append(")");
    line = buffer.toString();
    System.out.println(line);
    return line;
  }

  Set<String> getCommands()
  {
    Set<String> commands = new HashSet<>();

    for (Class<? extends Library> cls : libraryClasses)
    {
      Method[] methods = cls.getMethods();
      for (Method method : methods)
      {
        if (method.isAnnotationPresent(Command.class))
        {
          Command annotation = method.getAnnotation(Command.class);
          String name = annotation.name();
          if ("".equals(name)) name = method.getName();
          commands.add(name);
        }
      }
    }
    return commands;
  }

  void showHelp(Context context, String fn)
  {
    for (Class<? extends Library> cls : libraryClasses)
    {
      Method[] methods = cls.getMethods();
      Arrays.sort(methods,
        (Method o1, Method o2) -> o1.getName().compareTo(o2.getName()));

      String libName = Library.getName(cls);
      System.out.println("\nLibrary " + libName + ":");

      for (Method method : methods)
      {
        if (method.isAnnotationPresent(Command.class))
        {
          Command annotation = method.getAnnotation(Command.class);
          String name = annotation.name();
          if (name.length() == 0) name = method.getName();

          System.out.println("- " + name +
            "(" + annotation.parameters() + "): " + annotation.description());
        }
      }
    }
  }
}
