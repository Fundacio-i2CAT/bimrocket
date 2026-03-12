package org.bimrocket.servlet.oauth2;

import java.util.List;

/**
 *
 * @author jordi.hernandez@i2cat.net
 */
public interface AuthenticationManager
{
  String getIssuer();

  public String getAuthenticationToken(String code, String redirectUri) throws Exception;

  public UserToken getUseridFromToken(String jsonToken) throws Exception;

  List<String> getDefaultRoles();
}
