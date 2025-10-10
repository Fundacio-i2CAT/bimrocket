package org.bimrocket.servlet.oauth2;

import org.bimrocket.service.security.SecurityService;

public interface AuthenticationManager {

    public void getAuthenticationToken(String code);

    public UserToken getUseridFromToken(String jsonToken);

    public boolean checkUseridDB(UserToken userToken, SecurityService securityService);

    public void generateHTMLResponse(UserToken userToken);
}
