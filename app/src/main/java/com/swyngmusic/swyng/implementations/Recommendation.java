package com.swyngmusic.swyng.implementations;

import com.swyngmusic.swyng.interfaces.*;
import com.swyngmusic.swyng.interfaces.Track;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ewolfe on 10/22/2016.
 */

public class Recommendation implements com.swyngmusic.swyng.interfaces.Recommendation{

    private ArrayList<Track> tracks;
    public Recommendation(Iterable<Track> tracksToAdd)
    {
        tracks = new ArrayList<>();
        for(Track t : tracksToAdd)
            tracks.add(t);
    }

    @Override
    public List<Track> getTracks() {
        return tracks;
    }
}
