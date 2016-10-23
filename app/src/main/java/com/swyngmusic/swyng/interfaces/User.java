package com.swyngmusic.swyng.interfaces;

/**
 * Created by ewolfe on 10/22/2016.
 */

public interface User {
    boolean hasPlaylist(String name);
    String createPlaylist(String name);
    void addTrack(String userId, String trackId);
    //String getPlaylistId(String name);
}
