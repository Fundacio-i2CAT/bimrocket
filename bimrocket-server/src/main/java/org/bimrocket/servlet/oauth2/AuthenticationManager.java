package org.bimrocket.servlet.oauth2;

import org.bimrocket.service.security.SecurityService;

public interface AuthenticationManager {

    public String getAuthenticationToken(String code);

    public UserToken getUseridFromToken(UserToken userToken);

    public boolean checkUseridDB(UserToken userToken, SecurityService securityService);

    public void generateHTMLResponse(UserToken userToken);
}
