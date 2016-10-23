package com.swyngmusic.swyng.interfaces;

import java.util.List;

/**
 * Created by ewolfe on 10/22/2016.
 */

public interface Track {
    String getName();
    String getUri();
    String getId();
    List<Artist> getArtists();
}
