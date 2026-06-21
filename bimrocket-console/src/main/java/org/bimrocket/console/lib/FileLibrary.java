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
import java.io.FileFilter;
import java.io.PrintStream;
import java.util.regex.Pattern;
import org.bimrocket.console.annotation.Command;
import org.bimrocket.console.annotation.CommandLibrary;
import org.bimrocket.console.Library;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

/**
 *
 * @author realor
 */
@CommandLibrary(
  name = "file",
  description = "File management")
public class FileLibrary extends Library
{
  public FileLibrary(Context context, PrintStream out)
  {
    super(context, out);
  }

  @Override
  public void init()
  {
    super.init();

    argString("dir", "wildcard", "*");
  }

  public void dir()
  {
    dir(argString("dir", "wildcard", "*"));
  }

  @Command(
    name = "dir",
    description = "Print the names of the files in the current working directory.",
    arguments = "wildcard:String")
  public void dir(String wildcard)
  {
    Pattern pattern = toRegex(wildcard);

    File dir = getFile(".");
    File[] files = dir.listFiles(f -> pattern.matcher(f.getName()).matches());
    if (files != null)
    {
      for (File file : files)
      {
        if (file.isDirectory())
        {
          println("%s <DIR>".formatted(file.getName()));
        }
        else
        {
          long length = file.length();
          println("%s (%,d)".formatted(file.getName(), length));
        }
      }
    }
  }

  @Command(
    name = "pwd",
    description = "Print the current working directory.")
  public void pwd()
  {
    println(cwd());
  }

  @Command(
    name = "cd",
    description = "Change the current working directory.",
    arguments = "dirName:String")
  public void cd(String dirName)
  {
    File dir = getFile(dirName);
    if (dir.exists() && dir.isDirectory())
    {
      cwd(dir);
      return;
    }
    throw new RuntimeException("Invalid directory.");
  }

  @Command(
    name = "paths",
    description = "Get an array of all files under a directory.",
    arguments = "dirName:String, wildcard:String")
  public Value paths(String dirName, String wildcard)
  {
    Pattern pattern = toRegex(wildcard.toLowerCase());

    Value jsArrayClass = context.eval("js", "Array");

    Value jsArray = jsArrayClass.newInstance();

    File baseDir = getFile(dirName);
    FileFilter filter = f -> f.isDirectory() ||
      pattern.matcher(f.getName().toLowerCase()).matches();

    if (baseDir != null && baseDir.isDirectory())
    {
      String basePath = baseDir.getAbsolutePath();

      collectFiles(baseDir, basePath, filter, jsArray);
    }
    return jsArray;
  }

  private void collectFiles(
    File dir,
    String basePath,
    FileFilter filter,
    Value jsArray)
  {
    File[] files = dir.listFiles(filter);
    if (files == null)
    {
      return;
    }

    for (File file : files)
    {
      if (file.isDirectory())
      {
        collectFiles(file, basePath, filter, jsArray);
      }
      else
      {
        String absolute = file.getAbsolutePath();
        String relative = absolute.substring(basePath.length());
        relative = relative.replace(File.separatorChar, '/');

        if (relative.startsWith("/"))
        {
          relative = relative.substring(1);
        }
        jsArray.invokeMember("push", relative);
      }
    }
  }

  private Pattern toRegex(String wildcard)
  {
    String regex = wildcard
      .replace("\\", "\\\\")
      .replace(".", "\\.")
      .replace("*", ".*")
      .replace("?", ".");

    return Pattern.compile("^" + regex + "$");
  }
}
