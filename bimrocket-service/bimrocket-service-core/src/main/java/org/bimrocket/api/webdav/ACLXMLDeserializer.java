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
package org.bimrocket.api.webdav;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.bimrocket.service.file.ACL;
import org.bimrocket.service.file.util.MutableACL;
import static org.bimrocket.api.webdav.WebdavUtils.DAV_NS;
import static org.bimrocket.api.webdav.WebdavUtils.mapXmlTagToPrivilege;
import static org.bimrocket.service.security.SecurityConstants.AUTHENTICATED_ROLE;
import static org.bimrocket.service.security.SecurityConstants.EVERYONE_ROLE;

/**
 *
 * @author jordi.hernandez@i2cat.net
 */
public class ACLXMLDeserializer
{
  // Receives current user to replace when tag is D:owner
  public static ACL deserialize(String xml, String userId) throws IOException
  {
    try
    {
      MutableACL acl = new MutableACL();

      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      // Assume xml is UTF-8 encoded!
      ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
      Document doc = builder.parse(is);

      NodeList aceList = doc.getElementsByTagNameNS(DAV_NS, "ace");

      for (int i = 0; i < aceList.getLength(); i++)
      {
        Element ace = (Element) aceList.item(i);
        String principal = getPrincipal(ace, userId);
        if (principal == null) continue;

        NodeList privilegeList = ace.getElementsByTagNameNS(DAV_NS, "privilege");

        for (int j = 0; j < privilegeList.getLength(); j++)
        {
          Element privilegeElement = getFirstElementChild(privilegeList.item(j));
          if (privilegeElement == null) continue;

          String privilegeTag = privilegeElement.getLocalName();
          acl.grant(principal, mapXmlTagToPrivilege(privilegeTag));
        }
      }
      return acl;
    }
    catch (ParserConfigurationException | SAXException ex)
    {
      throw new RuntimeException(ex);
    }
    catch (IllegalArgumentException ex)
    {
      throw ex;
    }
  }

  //Get First Element for Privileges
  private static Element getFirstElementChild(Node node)
  {
    NodeList children = node.getChildNodes();
    for (int i = 0; i < children.getLength(); i++)
    {
      Node child = children.item(i);
      if (child.getNodeType() == Node.ELEMENT_NODE)
      {
        return (Element) child;
      }
    }
    return null;
  }

  // Check role type
  private static String getPrincipal(Element ace, String userId)
  {
    NodeList principalNodes = ace.getElementsByTagNameNS(DAV_NS, "principal");
    if (principalNodes.getLength() > 0)
    {
      Element principalElement = (Element) principalNodes.item(0);

      if (principalElement.getElementsByTagNameNS(DAV_NS, "all").getLength() > 0)
      {
        return EVERYONE_ROLE;
      }
      else if (principalElement.getElementsByTagNameNS(DAV_NS, "authenticated").getLength() > 0)
      {
        return AUTHENTICATED_ROLE;
      }
      else if (principalElement.getElementsByTagNameNS(DAV_NS, "href").getLength() > 0)
      {
        return principalElement.getElementsByTagNameNS(DAV_NS, "href").item(0).getTextContent();
      }
    }
    return null;
  }
}