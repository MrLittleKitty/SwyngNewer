package com.swyngmusic.swyng.implementations;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ewolfe on 10/22/2016.
 */

public class Track implements com.swyngmusic.swyng.interfaces.Track{

    public Track(String name, String uri, String id, List<com.swyngmusic.swyng.interfaces.Artist> artists)
    {
        this.name = name;
        this.uri = uri;
        this.id = id;
        this.artists = artists;
    }

    private String name;
    private String uri;
    private String id;
    private List<com.swyngmusic.swyng.interfaces.Artist> artists;

    public List<com.swyngmusic.swyng.interfaces.Artist> getArtists()
    {
        return artists;
    }

    public String getName()
    {
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
