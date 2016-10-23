package com.swyngmusic.swyng;

/**
 * Created by ewolfe on 10/22/2016.
 */

public class AuthToken {
    private String preparedAuthToken;

    public AuthToken(String accessToken)
    {
        this.preparedAuthToken = "Bearer "+accessToken;
    }

    public String getAuthToken()
    {
        return toString();
    }

    @Override
    public String toString()
    {
        return preparedAuthToken;
    }
}
