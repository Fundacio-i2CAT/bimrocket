package org.bimrocket.servlet.oauth2;

public class UserToken {
    private String userId;
    private String accessToken;
    private String refreshToken;

    public UserToken(String userId, String accessToken, String refreshToken)
    {
        this.userId = userId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }
    public String getUserId()
    {
        return this.userId;
    }

    public void setAccessToken(String accessToken)
    {
        this.accessToken = accessToken;
    }
    public String getAccessToken()
    {
        return this.accessToken;
    }

    public void setRefreshToken(String refreshToken)
    {
        this.refreshToken = refreshToken;
    }
    public String getRefreshToken()
    {
        return this.refreshToken;
    }
}
