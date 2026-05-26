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
package org.bimrocket.quarkus;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bimrocket.util.BimRocketInfo;
import org.eclipse.microprofile.config.Config;

@ApplicationScoped
public class ServerManager
{
  private static final Logger LOGGER =
    Logger.getLogger(ServerManager.class.getName());

  private static final int MONITOR_INTERVAL = 2000;

  private static final String RUNNING_FILE_PATH = "running";

  private volatile boolean shuttingDown = false;

  private volatile Path flagPath;

  private Thread fileMonitorThread;

  @Inject
  Config config;

  void onStart(@Observes StartupEvent ev)
  {
    shuttingDown = false;

    flagPath = Paths.get(RUNNING_FILE_PATH).toAbsolutePath();

    String port =
      config.getValue("quarkus.http.port", String.class);

    try
    {
      Files.writeString(flagPath,
        """

        BIMROCKET version: %s
        -----------------------------------------
        Listening on: http://localhost:%s
        -----------------------------------------
        """.formatted(BimRocketInfo.getVersionLabel(), port));

      fileMonitorThread = new FileMonitorThread();
      fileMonitorThread.setDaemon(true);
      fileMonitorThread.start();
    }
    catch (IOException ex)
    {
      LOGGER.log(Level.WARNING,
        "Can't write to {0}: {1}",
        new Object[]{ flagPath, ex });
    }
  }

  void onStop(@Observes ShutdownEvent ev)
  {
    shuttingDown = true;

    if (fileMonitorThread != null)
    {
      fileMonitorThread.interrupt();
    }

    if (flagPath != null)
    {
      try
      {
        Files.deleteIfExists(flagPath);
      }
      catch (IOException ex)
      {
        LOGGER.log(Level.WARNING,
          "Can't delete {0}: {1}",
          new Object[]{ flagPath, ex });
      }
    }
  }

  class FileMonitorThread extends Thread
  {
    @Override
    public void run()
    {
      try
      {
        while (!isInterrupted() &&
               flagPath != null &&
               Files.exists(flagPath))
        {
          Thread.sleep(MONITOR_INTERVAL);
        }

        if (!isInterrupted() && !shuttingDown)
        {
          LOGGER.info("Running file deleted. Shutting down.");
          Quarkus.asyncExit();
        }
      }
      catch (InterruptedException ex)
      {
        interrupt();
      }
    }
  }
}