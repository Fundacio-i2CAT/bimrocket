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

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 *
 * @author realor
 */
public class TextUtils
{
  public static final String MINUTES = "minutes";
  public static final String HOURS = "hours";
  public static final String DAYS = "days";

  public static String getISODate()
  {
    return getISODate(new Date());
  }

  public static String getISODate(Date date)
  {
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    return df.format(date);
  }

  public static int compareDates(String dateInit, String dateEnd)
  {
    //Return
    // 0 = Equals
    // 1 = dateInit < dateEnd
    // 2 = dateInit > dateEnd
    DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    LocalDateTime date1 = LocalDateTime.parse(dateInit, df);
    LocalDateTime date2 = LocalDateTime.parse(dateEnd, df);

    if (date1.isBefore(date2))
    {
      return 1;
    }
    else if (date1.isAfter(date2))
    {
      return 2;
    }
    else
    {
      return 0;
    }
  }

  public static String addTime(String isoDate, int amount, String unit)
  {
    DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    LocalDateTime date = LocalDateTime.parse(isoDate, df);

    switch (unit.toLowerCase())
    {
      case "minutes":
        date = date.plusMinutes(amount);
        break;
      case "hours":
        date = date.plusHours(amount);
        break;
      case "days":
        date = date.plusDays(amount);
    }

    return df.format(date);
  }
}
