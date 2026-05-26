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
package org.bimrocket.util;

import java.io.UnsupportedEncodingException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class URIEncoderTest
{

  @Test
  public void testEncodeWithDefaultCharset()
  {
    // Input and output expected
    String input = "https://example.com/test?name=John Doe&age=25";
    String expectedOutput = "https://example.com/test?name=John%20Doe&age=25";

    // Execute encode method
    String result = URIEncoder.encode(input);

    // Verify that is the expected output
    assertEquals(expectedOutput, result);
  }

  @Test
  public void testEncodeWithCustomCharset()
  {
    // Entrada y salida esperada
    String input = "https://example.com/test?name=John Doe&age=25";
    String expectedOutput = "https://example.com/test?name=John%20Doe&age=25";

    // Execute encode method with a diferent charset
    String result = URIEncoder.encode(input, "UTF-8");

    // Verify that is the expected output
    assertEquals(expectedOutput, result);
  }

  @Test
  public void testEncodeWithSpecialCharacters()
  {
    // Input with special characters
    String input = "https://example.com/test?param=hello world!&code=123/456";
    String expectedOutput = "https://example.com/test?param=hello%20world!&code=123/456";

    // Execute encode method
    String result = URIEncoder.encode(input);

    // Verify that is the expected output
    assertEquals(expectedOutput, result);
  }

  @Test
  public void testEncodeWithUnsupportedCharset()
  {
    // Input with a supported charset
    String input = "https://example.com";

    // Verify we are throwing RuntimeException and the reason is UnsupportedEncodingException
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      URIEncoder.encode(input, "INVALID_CHARSET");
    });

    // Verify the reason is UnsupportedEncodingException
    assertTrue(exception.getCause() instanceof UnsupportedEncodingException);
  }
}
