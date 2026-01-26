package org.bimrocket.servlet.webdav;

import org.bimrocket.service.file.ACL;
import org.bimrocket.service.file.Privilege;
import org.bimrocket.service.file.util.MutableACL;
import org.bimrocket.util.TextUtils;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ACLXMLSerializerTest
{
  @Test
  public void testSerialize_privilegis_admin()
  {
    // ADMIN ha de tenir privilegis de READ_ACL i WRITE_ACL

    MutableACL acl = new MutableACL();
    acl.grant("ADMIN", Privilege.READ_ACL);
    acl.grant("ADMIN", Privilege.WRITE_ACL);

    acl.grant("PROJECTISTA", Privilege.READ);
    String result = ACLXMLSerializer.serialize(acl);

    assertTrue((result.contains("read-acl") && result.contains("ADMIN")) ||
                     (result.contains("write-acl") && result.contains("ADMIN"))
    );
  }

  @Test
  public void testSerialize_error_privilegis_admin()
  {
    // ADMIN ha de tenir privilegis de READ_ACL i WRITE_ACL pero no WRITE

    MutableACL acl = new MutableACL();
    acl.grant("ADMIN", Privilege.READ_ACL);
    acl.grant("ADMIN", Privilege.WRITE_ACL);

    acl.grant("PROJECTISTA", Privilege.READ);
    String result = ACLXMLSerializer.serialize(acl);

    assertTrue(result.contains("read-acl") && result.contains("ADMIN"));
    assertTrue(result.contains("write-acl") && result.contains("ADMIN"));
    assertFalse(result.matches("(?s).*\\bwrite\\b.*ADMIN.*"));
  }
}
