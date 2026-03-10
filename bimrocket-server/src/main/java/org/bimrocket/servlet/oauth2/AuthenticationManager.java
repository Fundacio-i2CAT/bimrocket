package org.bimrocket.servlet.oauth2;

/**
 *
 * @author jordi.hernandez@i2cat.net
 */
public interface AuthenticationManager
{
  public String getAuthenticationToken(String code, String redirectUri) throws Exception;

  public UserToken getUseridFromToken(String jsonToken) throws Exception;
}
