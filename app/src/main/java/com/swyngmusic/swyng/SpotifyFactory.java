package com.swyngmusic.swyng;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.swyngmusic.swyng.interfaces.Artist;
import com.swyngmusic.swyng.interfaces.Recommendation;
import com.swyngmusic.swyng.interfaces.Track;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ewolfe on 10/22/2016.
 */

public class SpotifyFactory
{
    public static final String SPOTIFY_URL_BASE = "https://api.spotify.com";

    public static String getUserID(AuthToken token)
    {
        JSONObject userProfile = getUserProfile(token);
        if(userProfile == null)
            return null;

        try {
            return userProfile.getString("id");
        } catch (JSONException e) {
            return null;
        }
    }

    public static boolean addTrackToPlaylist(AuthToken token, String userId, String playlistId, String trackUri)
    {
        try {
            HttpResponse<JsonNode> playlistsRequest = Unirest.post(SPOTIFY_URL_BASE+"/v1/users/"+userId+"/playlists/"+playlistId+"/tracks")
                    .header("Authorization", token.getAuthToken())
                    .header("Content-Type","json")
                    .queryString("uris",trackUri)
                    .asJson();
            if(playlistsRequest.getStatus() != 201)
                return false;

            return true;
        } catch (UnirestException e) {
            return false;
        }
    }

    public static JSONObject getUserProfile(AuthToken token)
    {
        HttpResponse<JsonNode> meResponse = null;
        try {
            meResponse = Unirest.get(SPOTIFY_URL_BASE+"/v1/me")
                    .header("Authorization", token.getAuthToken())
                    .asJson();
        } catch (UnirestException e) {
            return null;
        }

        if(meResponse.getStatus() != 200)
            return null;

        return meResponse.getBody().getObject();
    }

    public static String createPlaylist(AuthToken token, String userId, String playlistName)
    {
        try {
            HttpRequestWithBody playlistsRequest = Unirest.post(SPOTIFY_URL_BASE+"/v1/users/"+userId+"/playlists")
                    .header("Authorization", token.getAuthToken())
                    .header("Content-Type","json");

            JSONStringer stringer = new JSONStringer();
            stringer.object()
                    .key("name")
                    .value(playlistName)
                    .key("public")
                    .value(true)
                    .endObject();

            playlistsRequest.body(stringer.toString());

            HttpResponse<JsonNode> response =  playlistsRequest.asJson();

            if(response.getStatus() != 201)
                return null;

            JSONObject playlistObj = response.getBody().getObject();

            return playlistObj.getString("uri");
        } catch (UnirestException e) {
            return null;
        } catch (JSONException e) {
            return null;
        }
    }

    public static Track getTrack(AuthToken token, String userId, String playlistId)
    {
        try {
            HttpResponse<JsonNode> playlistsRequest = Unirest.get(SPOTIFY_URL_BASE+"/v1/users/"+userId+"/playlists/"+playlistId+"/tracks")
                    .header("Authorization", token.getAuthToken())
                    .queryString("limit",1)
                    .asJson();

            if(playlistsRequest.getStatus() != 200)
                return null;

            JSONObject pagingObject = playlistsRequest.getBody().getObject();
            JSONArray tracksObjects = pagingObject.getJSONArray("items");
            if(tracksObjects.length() <= 0)
                return null;

            JSONObject trackObj = tracksObjects.getJSONObject(0).getJSONObject("track");
            return parseTrack(trackObj);

        } catch (UnirestException e) {
            return null;
        } catch (JSONException e) {
            return null;
        }
    }

    private static Track parseTrack(JSONObject trackObj)
    {
        if(trackObj != null) {
            List<Artist> artistsList = new ArrayList<>();
            JSONArray artists = null;
            try {
                artists = trackObj.getJSONArray("artists");
                for (int a = 0; a < artists.length(); a++) {
                    JSONObject artistObj = artists.getJSONObject(a);
                    Artist artist = new com.swyngmusic.swyng.implementations.Artist(artistObj.getString("name"), artistObj.getString("id"), artistObj.getString("uri"));
                    artistsList.add(artist);
                }
                String name = trackObj.getString("name");
                String uri = trackObj.getString("uri");
                String id = trackObj.getString("id");
                return new com.swyngmusic.swyng.implementations.Track(name, uri, id, artistsList);
            } catch (JSONException e) {
                return null;
            }
        }
        return null;
    }


    public static JSONArray getPlaylists(AuthToken token, String userID)
    {
        try {
            HttpResponse<JsonNode> playlistsRequest = Unirest.get(SPOTIFY_URL_BASE+"/v1/users/"+userID+"/playlists")
                    .header("Authorization", token.getAuthToken())
                    .asJson();

            if(playlistsRequest.getStatus() != 200)
                return null;

            return playlistsRequest.getBody().getObject().getJSONArray("items");
        } catch (UnirestException e) {
            return null;
        } catch (JSONException e) {
            return null;
        }
    }

//    public List<String> getPlayListNames(AuthToken token, String userId)
//    {
//        JSONArray playlists = getPlaylists(token,userId);
//        if(playlists == null)
//            return null;
//
//        ArrayList<String> names = new ArrayList<>();
//        for(int i = 0; i < playlists.length(); i++)
//        {
//            try {
//                names.add(playlists.getJSONObject(i).getString("name"));
//            } catch (JSONException e) {
//                return null;
//            }
//        }
//        return names;
//    }

    public static Recommendation getRecommendation(AuthToken token, String seedArtists, String seedGenres, String seedTracks)
    {
        try {
            HttpResponse<JsonNode> response = Unirest.get(SPOTIFY_URL_BASE+"/v1/recommendations")
                    .header("Authorization", token.getAuthToken())
                    .queryString("seed_artists", seedArtists)
                    .queryString("seed_genres", seedGenres)
                    .queryString("seed_tracks", seedTracks)
                    .asJson();

            if(response.getStatus() != 200)
                return null;

            JSONArray tracks = response.getBody().getObject().getJSONArray("tracks");
            if(tracks == null)
                return null;

            ArrayList<Track> trackList = new ArrayList<>();
            for(int i = 0; i < tracks.length(); i++)
            {
                JSONObject trackObj = tracks.getJSONObject(i);
                Track t = parseTrack(trackObj);
                if(t != null)
                    trackList.add(t);
            }
            return new com.swyngmusic.swyng.implementations.Recommendation(trackList);

        } catch (UnirestException e) {
            return null;
        } catch (JSONException e) {
            return null;
        }
    }
}