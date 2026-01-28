package org.bimrocket.servlet.webdav;

import org.bimrocket.service.file.ACL;
import org.bimrocket.service.file.Privilege;
import org.bimrocket.service.file.util.MutableACL;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class ACLXMLDeserializerTest
{
  private static String xmlSource = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n" +
          "<D:acl xmlns:D=\"DAV:\">\n" +
          "      \n" +
          "    <!-- Access for ADMIN in .acl -->\n" +
          "      \n" +
          "    <D:ace>\n" +
          "            \n" +
          "        <D:principal>\n" +
          "                  \n" +
          "            <D:href>ADMIN</D:href>\n" +
          "                \n" +
          "        </D:principal>\n" +
          "            \n" +
          "        <D:grant>\n" +
          "                  \n" +
          "            <D:privilege>\n" +
          "                <D:read-acl/>\n" +
          "            </D:privilege>\n" +
          "                  \n" +
          "            <D:privilege>\n" +
          "                <D:write-acl/>\n" +
          "            </D:privilege>\n" +
          "                \n" +
          "        </D:grant>\n" +
          "          \n" +
          "    </D:ace>\n" +
          "      \n" +
          "  \n" +
          "    <!-- Access for PROJECTISTA in .acl -->\n" +
          "      \n" +
          "    <D:ace>\n" +
          "            \n" +
          "        <D:principal>\n" +
          "                  \n" +
          "            <D:href>PROJECTISTA</D:href>\n" +
          "                \n" +
          "        </D:principal>\n" +
          "            \n" +
          "        <D:grant>\n" +
          "                  \n" +
          "            <D:privilege>\n" +
          "                <D:read/>\n" +
          "            </D:privilege>\n" +
          "                  \n" +
          "            <D:privilege>\n" +
          "                <D:write/>\n" +
          "            </D:privilege>\n" +
          "                \n" +
          "        </D:grant>\n" +
          "          \n" +
          "    </D:ace>\n" +
          "      \n" +
          "  \n" +
          "    <!-- Access for VECTOR-UT-OGE in .acl -->\n" +
          "      \n" +
          "    <D:ace>\n" +
          "            \n" +
          "        <D:principal>\n" +
          "                  \n" +
          "            <D:href>VECTOR-UT-OGE</D:href>\n" +
          "                \n" +
          "        </D:principal>\n" +
          "            \n" +
          "        <D:grant>\n" +
          "                  \n" +
          "            <D:privilege>\n" +
          "                <D:read/>\n" +
          "            </D:privilege>\n" +
          "                \n" +
          "        </D:grant>\n" +
          "          \n" +
          "    </D:ace>\n" +
          "    \n" +
          "</D:acl>";

  @Test
  public void testDeserializePrivilegesAdmin()
  {
    Object[] arrayResult = null;

    // Privilegis per rol ADMIN han de ser READ-ACL i WRITE-ACL
    try
    {
      ACL acl = ACLXMLDeserializer.deserialize(xmlSource, "user");
      arrayResult = acl.getPrivilegesForRoleId("ADMIN").toArray();
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }

    assertTrue(arrayResult.length == 2 &&
            findStringInArray(arrayResult, Privilege.READ_ACL.toString()) &&
            findStringInArray(arrayResult, Privilege.WRITE_ACL.toString())
            );
  }

  @Test
  public void testDeserializePrivilegesProjectista()
  {
    Object[] listRolesIdPrivileges = null;

    // PROJECTISTA es l'unic amb privilegis de WRITE
    try {
      ACL acl = ACLXMLDeserializer.deserialize(xmlSource, "user");
      listRolesIdPrivileges = acl.getRoleIdsForPrivilege(Privilege.WRITE).toArray();
    } catch (IOException e)
    {
      throw new RuntimeException(e);
    }

    assertTrue(findStringInArray(listRolesIdPrivileges, "PROJECTISTA"));
  }

  @Test
  public void testDeserializePrivilegesVector()
  {
    Object[] listRolesIdPrivileges = null;

    // VECTOR-UT-OGE no te privilegis de WRITE
    try {
      ACL acl = ACLXMLDeserializer.deserialize(xmlSource, "user");
      listRolesIdPrivileges = acl.getRoleIdsForPrivilege(Privilege.WRITE).toArray();
    } catch (IOException e)
    {
      throw new RuntimeException(e);
    }

    assertFalse(findStringInArray(listRolesIdPrivileges, "VECTOR-UT-OGE"));
  }

  @Test
  public void testDeserializeUnknownRole()
  {
    Object[] listRolesId = null;

    // UNKNOWN Role no existeix
    try {
      ACL acl = ACLXMLDeserializer.deserialize(xmlSource, "user");
      listRolesId = acl.getRoleIds().toArray();
    } catch (IOException e)
    {
      throw new RuntimeException(e);
    }

    assertFalse(findStringInArray(listRolesId, "UNKNOWN"));
  }

  boolean findStringInArray(Object[] listValues, String value)
  {
    boolean foundValue = false;
    for (Object o : listValues)
    {
      if (value.equals(o.toString())) {
        foundValue = true;
        break;
      }
    }
    return foundValue;
  }

}
