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
package org.bimrocket.express.data;

import org.bimrocket.express.ExpressDefinedType;
import org.bimrocket.express.ExpressEntity;
import org.bimrocket.express.ExpressNamedType;
import org.bimrocket.express.ExpressSchema;
import org.bimrocket.express.ExpressType;
import org.bimrocket.express.data.CompactData.Element;

/**
 *
 * @author realor
 */
public class CompactData extends AbstractListData<Element>
{
  long lastId = 0;

  public CompactData(ExpressSchema schema)
  {
    super(schema);
  }

  public class Element
  {
    private final ExpressNamedType namedType;
    private final Object[] values;
    private final long id;

    public Element(String typeName)
    {
      namedType = schema.getNamedType(typeName);
      id = ++lastId;

      if (namedType instanceof ExpressEntity entity)
      {
        values = new Object[entity.getAllAttributes().size()];
      }
      else if (namedType instanceof ExpressDefinedType)
      {
        values = new Object[1];
      }
      else
      {
        throw new RuntimeException("Invalid type: " + typeName);
      }
    }
  }

  @Override
  protected Element getElement(Object value)
  {
    if (value instanceof Element element)
    {
      return element;
    }
    return null;
  }

  @Override
  protected Element createEntity(ExpressEntity entity)
  {
    return new Element(entity.getNormalizedTypeName());
  }

  @Override
  protected Element createDefinedType(ExpressDefinedType definedType)
  {
    return new Element(definedType.getNormalizedTypeName());
  }

  @Override
  protected String getElementTypeName(Element element)
  {
    return element.namedType.getTypeName();
  }

  @Override
  protected String getElementId(Element element)
  {
    return String.valueOf(element.id);
  }

  @Override
  protected Object getElementValue(Element element, String name, ExpressType type)
  {
    if (element.namedType instanceof ExpressEntity entity)
    {
      int index = entity.getAttributeIndex(name);
      return element.values[index];
    }
    else
    {
      return element.values[0];
    }
  }

  @Override
  protected void setElementValue(Element element, String name, Object value,
    ExpressType type)
  {
    if (element.namedType instanceof ExpressEntity entity)
    {
      int index = entity.getAttributeIndex(name);
      element.values[index] = value;
    }
    else
    {
      element.values[0] = value;
    }
  }
}
