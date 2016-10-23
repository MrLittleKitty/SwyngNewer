package com.swyngmusic.swyng.implementations;

/**
 * Created by ewolfe on 10/22/2016.
 */

public class Artist  implements com.swyngmusic.swyng.interfaces.Artist{

    public Artist(String name, String id, String uri)
    {
        this.name = name;
        this.uri = uri;
        this.id = id;
    }

    private String name;
    private String uri;
    private String id;

    public String getName() {
        return name;
    }

    public String getUri()
    {
        return uri;
    }

    public String getId()
    {
        return id;
    }
}
