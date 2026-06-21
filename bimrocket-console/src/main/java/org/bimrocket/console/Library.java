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

import java.io.File;
import org.bimrocket.console.annotation.Command;
import java.io.PrintStream;
import java.lang.reflect.Method;
import org.bimrocket.console.annotation.CommandLibrary;
import org.bimrocket.express.data.ExpressCursor;
import org.bimrocket.express.data.ExpressData;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

/**
 *
 * @author realor
 */
public abstract class Library
{
  public static final String LIBS_VAR = "libs";
  public static final String DATA_VAR = "data";
  public static final String CURSOR_VAR = "cursor";
  public static final String CWD_VAR = "cwd";

  final protected Context context;
  final protected Value bindings;
  final protected PrintStream out;

  public Library(Context context, PrintStream out)
  {
    this.context = context;
    bindings = context.getBindings("js");
    this.out = out;
  }

  public static String getName(Class<? extends Library> cls)
  {
    return cls.isAnnotationPresent(CommandLibrary.class) ?
      cls.getAnnotation(CommandLibrary.class).name() : cls.getName();
  }

  public String name()
  {
    return getName(getClass());
  }

  protected Context context()
  {
    return context;
  }

  protected String argString(String command, String name, String defaultValue)
  {
    Value value = context.eval("js", "globalThis.args?.%s?.%s || null"
      .formatted(command, name));
    if (value.isNull())
    {
      value = context.eval("js",
        "((globalThis.args ||= {}).%s ||= {}).%s = \"%s\""
        .formatted(command, name, defaultValue.replace("\\", "\\\\")));
    }
    return value.as(String.class);
  }

  protected int argInteger(String command, String name, int defaultValue)
  {
    Value value = context.eval("js", "globalThis.args?.%s?.%s || null"
      .formatted(command, name));
    if (value.isNull())
    {
      value = context.eval("js",
        "((globalThis.args ||= {}).%s ||= {}).%s = %d"
        .formatted(command, name, defaultValue));
    }
    return value.as(Integer.class);
  }

  protected ExpressCursor cursor()
  {
    Value member = bindings.getMember(CURSOR_VAR);
    return member == null ? null : member.as(ExpressCursor.class);
  }

  protected void cursor(ExpressCursor cursor)
  {
    bindings.putMember(CURSOR_VAR, cursor);
  }

  protected ExpressData data()
  {
    Value member = bindings.getMember(DATA_VAR);
    return member == null ? null : member.as(ExpressData.class);
  }

  protected void data(ExpressData data)
  {
    bindings.putMember(DATA_VAR, data);
  }

  protected File cwd()
  {
    Value member = bindings.getMember(CWD_VAR);
    if (member == null)
    {
      return new File(System.getProperty("user.dir"));
    }
    else
    {
      return member.as(File.class);
    }
  }

  protected void cwd(File cwd)
  {
    bindings.putMember(CWD_VAR, cwd);
  }

  protected File getFile(String path)
  {
    try
    {
      File file;
      if (path.startsWith("/") || path.contains(":"))
      {
        file = new File(path);
      }
      else
      {
        file = new File(cwd(), path);
      }
      return file.getCanonicalFile();
    }
    catch (Exception ex)
    {
      return null;
    }
  }

  protected void print(Object object)
  {
    out.print(object);
  }

  protected void println(Object object)
  {
    out.println(object);
  }

  protected void println()
  {
    out.println();
  }

  @SuppressWarnings("unchecked")
  public void init()
  {
    String libName = name();
    Value libs = bindings.getMember(LIBS_VAR);
    if (libs == null)
    {
      context.eval("js", "const %s = {}".formatted(LIBS_VAR));
      libs = bindings.getMember(LIBS_VAR);
    }

    libs.putMember(libName, this);

    for (Method method : getClass().getMethods())
    {
      if (method.isAnnotationPresent(Command.class))
      {
        String methodName = method.getName();
        Command annotation = method.getAnnotation(Command.class);
        String cmdName = annotation.name();
        if (cmdName.length() == 0) cmdName = methodName;
        registerCommand(context, cmdName, libName, methodName);
      }
    }
  }

  private void registerCommand(Context context,
    String cmdName, String libName, String fnName)
  {
    context.eval("js", "const %s = (...args) => libs.%s.%s(...args);"
      .formatted(cmdName, libName, fnName));
  }
}
