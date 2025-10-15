package org.bimrocket.servlet.oauth2;

import org.bimrocket.service.security.SecurityService;

public interface AuthenticationManager
{
  public String getAuthenticationToken(String code, String redirectUri) throws Exception;

  public UserToken getUseridFromToken(String jsonToken) throws Exception;

  public boolean checkUseridDB(UserToken userToken, SecurityService securityService) throws Exception;

  public void generateHTMLResponse(UserToken userToken) throws Exception;
}
