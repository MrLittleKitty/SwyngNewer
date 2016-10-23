package com.swyngmusic.swyng;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Connectivity;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Metadata;
import com.spotify.sdk.android.player.PlaybackState;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;
import com.swyngmusic.swyng.interfaces.Artist;
import com.swyngmusic.swyng.interfaces.Recommendation;
import com.swyngmusic.swyng.interfaces.Track;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;
import java.util.Random;

public class MainActivity extends Activity implements
        Player.NotificationCallback, ConnectionStateCallback {
    //   ____                _              _
    //  / ___|___  _ __  ___| |_ __ _ _ __ | |_ ___
    // | |   / _ \| '_ \/ __| __/ _` | '_ \| __/ __|
    // | |__| (_) | | | \__ \ || (_| | | | | |_\__ \
    //  \____\___/|_| |_|___/\__\__,_|_| |_|\__|___/
    //

    @SuppressWarnings("SpellCheckingInspection")
    private static final String CLIENT_ID = "4241e25175f04a0fb675959769d0c9f4";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String REDIRECT_URI = "swyngredirect://callback";

    @SuppressWarnings("SpellCheckingInspection")
    private static String SONG_URI = "spotify:track:6KywfgRqvgvfJc3JRwaZdZ";

    /**
     * Request code that will be passed together with authentication result to the onAuthenticationResult
     */
    private static final int REQUEST_CODE = 1337;
    private Boolean playInitial = true;

    public static final String SPOTIFY_URL_BASE = "https://api.spotify.com";

    /**
     * UI controls which may only be enabled after the player has been initialized,
     * (or effectively, after the user has logged in).
     */
    private static final int[] REQUIRES_INITIALIZED_STATE = {
           R.id.play_track_button
    };

    /**
     * UI controls which should only be enabled if the player is actively playing.
     */
    private static final int[] REQUIRES_PLAYING_STATE = {
            R.id.dislike_button,
            R.id.like_button,
            R.id.skip_song_button
    };
    public static final String TAG = "Swyng";
    private Random random = new Random();
    private Track lastLikedTrack = null;
    private Track nextTrack = null;
    private boolean checkIfClicked = false;
    private String playlistUri = null;
    private String userId = null;

    //  _____ _      _     _
    // |  ___(_) ___| | __| |___
    // | |_  | |/ _ \ |/ _` / __|
    // |  _| | |  __/ | (_| \__ \
    // |_|   |_|\___|_|\__,_|___/
    //

    /**
     * The player used by this activity. There is only ever one instance of the player,
     * which is owned by the {@link com.spotify.sdk.android.player.Spotify} class and refcounted.
     * This means that you may use the Player from as many Fragments as you want, and be
     * assured that state remains consistent between them.
     * <p/>
     * However, each fragment, activity, or helper class <b>must</b> call
     * {@link com.spotify.sdk.android.player.Spotify#destroyPlayer(Object)} when they are no longer
     * need that player. Failing to do so will result in leaked resources.
     */
    private SpotifyPlayer mPlayer;

    private PlaybackState mCurrentPlaybackState;

    private AuthToken authToken;

    /**
     * Used to get notifications from the system about the current network state in order
     * to pass them along to
     * {@link SpotifyPlayer#setConnectivityStatus(Player.OperationCallback, Connectivity)}
     * Note that this implies <pre>android.permission.ACCESS_NETWORK_STATE</pre> must be
     * declared in the manifest. Not setting the correct network state in the SDK may
     * result in strange behavior.
     */
    private BroadcastReceiver mNetworkStateReceiver;

    private TextView mStatusText;

    /**
     * Used to log messages to a {@link android.widget.TextView} in this activity.
     */
    private TextView mMetadataText;


    /**
     * Used to scroll the {@link #mStatusText} to the bottom after updating text.
     */
    private ScrollView mStatusTextScrollView;
    private Metadata mMetadata;

    private final Player.OperationCallback mOperationCallback = new Player.OperationCallback() {
        @Override
        public void onSuccess() {
            logStatus("OK!");
        }

        @Override
        public void onError(Error error) {
            logStatus("ERROR:" + error);
        }
    };

    //  ___       _ _   _       _ _          _   _
    // |_ _|_ __ (_) |_(_) __ _| (_)______ _| |_(_) ___  _ __
    //  | || '_ \| | __| |/ _` | | |_  / _` | __| |/ _ \| '_ \
    //  | || | | | | |_| | (_| | | |/ / (_| | |_| | (_) | | | |
    // |___|_| |_|_|\__|_|\__,_|_|_/___\__,_|\__|_|\___/|_| |_|
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        // Get a reference to any UI widgets that we'll need to use later
        mMetadataText = (TextView) findViewById(R.id.metadata);
//        mStatusTextScrollView = (ScrollView) findViewById(R.id.status_text_container);
//        findViewById(R.id.status_text_container);
        updateView();
        logStatus("Ready");
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Set up the broadcast receiver for network events. Note that we also unregister
        // this receiver again in onPause().
        mNetworkStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mPlayer != null) {
                    Connectivity connectivity = getNetworkConnectivity(getBaseContext());
                    logStatus("Network state changed: " + connectivity.toString());
                    mPlayer.setConnectivityStatus(mOperationCallback, connectivity);
                }
            }
        };

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkStateReceiver, filter);

        if (mPlayer != null) {
            mPlayer.addNotificationCallback(MainActivity.this);
            mPlayer.addConnectionStateCallback(MainActivity.this);
        }
    }

    /**
     * Registering for connectivity changes in Android does not actually deliver them to
     * us in the delivered intent.
     *
     * @param context Android context
     * @return Connectivity state to be passed to the SDK
     */
    private Connectivity getNetworkConnectivity(Context context) {
        ConnectivityManager connectivityManager;
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            return Connectivity.fromNetworkType(activeNetwork.getType());
        } else {
            return Connectivity.OFFLINE;
        }
    }

    //     _         _   _                _   _           _   _
    //    / \  _   _| |_| |__   ___ _ __ | |_(_) ___ __ _| |_(_) ___  _ __
    //   / _ \| | | | __| '_ \ / _ \ '_ \| __| |/ __/ _` | __| |/ _ \| '_ \
    //  / ___ \ |_| | |_| | | |  __/ | | | |_| | (_| (_| | |_| | (_) | | | |
    // /_/   \_\__,_|\__|_| |_|\___|_| |_|\__|_|\___\__,_|\__|_|\___/|_| |_|
    //

    private void openLoginWindow() {
        final AuthenticationRequest request = new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI)
                .setScopes(new String[]{"user-read-private", "playlist-read", "playlist-read-private", "streaming","playlist-modify-public","playlist-modify-private"})
                .build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    onAuthenticationComplete(response);
                    break;

                // Auth flow returned an error
                case ERROR:
                    logStatus("Auth error: " + response.getError());
                    break;

                // Most likely auth flow was cancelled
                default:
                    logStatus("Auth result: " + response.getType());
            }
        }
    }

    private void localSongHelper() {
        if(userId == null)
            userId = SpotifyFactory.getUserID(authToken);

        if(userId != null)
        {
            JSONArray playlists = SpotifyFactory.getPlaylists(authToken,userId);
            if(playlists != null && playlists.length() > 1)
            {
                try {
                    String id = playlists.getJSONObject(random.nextInt(playlists.length() - 1)).getString("id");
                    nextTrack= SpotifyFactory.getTrack(authToken,userId,id);
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    private void onAuthenticationComplete(AuthenticationResponse authResponse) {
        // Once we have obtained an authorization token, we can proceed with creating a Player.
        logStatus("Got authentication token");
        authToken = new AuthToken(authResponse.getAccessToken());
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        localSongHelper();

        if (mPlayer == null) {
            Config playerConfig = new Config(getApplicationContext(), authResponse.getAccessToken(), CLIENT_ID);
            // Since the Player is a static singleton owned by the Spotify class, we pass "this" as
            // the second argument in order to refcount it properly. Note that the method
            // Spotify.destroyPlayer() also takes an Object argument, which must be the same as the
            // one passed in here. If you pass different instances to Spotify.getPlayer() and
            // Spotify.destroyPlayer(), that will definitely result in resource leaks.
            mPlayer = Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                @Override
                public void onInitialized(SpotifyPlayer player) {
                    logStatus("-- Player initialized --");
                    mPlayer.setConnectivityStatus(mOperationCallback, getNetworkConnectivity(MainActivity.this));
                    mPlayer.addNotificationCallback(MainActivity.this);
                    mPlayer.addConnectionStateCallback(MainActivity.this);

                    // Trigger UI refresh
                    updateView();

                    userId = SpotifyFactory.getUserID(authToken);
                    if( userId != null) {
                        playlistUri = SpotifyFactory.createPlaylist(authToken, userId, "Team Name Here Playlist");
                    }
                }

                @Override
                public void onError(Throwable error) {
                    logStatus("Error in initialization: " + error.getMessage());
                }
            });
        } else {
            mPlayer.login(authResponse.getAccessToken());
        }
    }

    //  _   _ ___   _____                 _
    // | | | |_ _| | ____|_   _____ _ __ | |_ ___
    // | | | || |  |  _| \ \ / / _ \ '_ \| __/ __|
    // | |_| || |  | |___ \ V /  __/ | | | |_\__ \
    //  \___/|___| |_____| \_/ \___|_| |_|\__|___/
    //

    private void updateView() {
        boolean loggedIn = isLoggedIn();
        // Login button should be the inverse of the logged in state
        Button loginButton = (Button) findViewById(R.id.login_button);
        loginButton.setText(loggedIn ? R.string.logout_button_label : R.string.login_button_label);

        // Set enabled for all widgets which depend on initialized state
        for (int id : REQUIRES_INITIALIZED_STATE) {
            findViewById(id).setEnabled(loggedIn);
        }

        if (mPlayer != null) {
            mCurrentPlaybackState = mPlayer.getPlaybackState();
            mMetadata = mPlayer.getMetadata();
        }

        // Same goes for the playing state
        boolean playing = loggedIn && mMetadata != null;
        for (int id : REQUIRES_PLAYING_STATE) {
            findViewById(id).setEnabled(playing);
        }

        if (mMetadata != null) {
            findViewById(R.id.like_button).setVisibility(View.VISIBLE);
            findViewById(R.id.dislike_button).setVisibility(View.VISIBLE);
            //findViewById(R.id.pause_button).setEnabled(mMetadata.currentTrack != null);
        }

        final ImageView coverArtView = (ImageView) findViewById(R.id.cover_art);
        if (mMetadata != null && mMetadata.currentTrack != null) {
            final double durationStr = mMetadata.currentTrack.durationMs;
            int minutes = (int)durationStr / (60 * 1000);
            int seconds = ((int)durationStr / 1000) % 60;
            String time = String.format("%d:%02d", minutes, seconds);
            mMetadataText.setText(mMetadata.currentTrack.name +  " - " + mMetadata.currentTrack.artistName + " " + time);

            Picasso.with(this)
                    .load(mMetadata.currentTrack.albumCoverWebUrl)
                    .transform(new Transformation() {
                        @Override
                        public Bitmap transform(Bitmap source) {
                            // really ugly darkening trick
                            final Bitmap copy = source.copy(source.getConfig(), true);
                            source.recycle();
                            final Canvas canvas = new Canvas(copy);
                            canvas.drawColor(0xbb000000);
                            return copy;
                        }

                        @Override
                        public String key() {
                            return "darken";
                        }
                    })
                    .into(coverArtView);
        } else {
            coverArtView.setBackground(null);
        }

    }

    private boolean isLoggedIn() {
        return mPlayer != null && mPlayer.isLoggedIn();
    }

    public void onLoginButtonClicked(View view) {
        if (!isLoggedIn()) {
            logStatus("Logging in");
            openLoginWindow();
        } else {
            mPlayer.logout();
        }
    }

    public void onPlayButtonClicked(View view) {
        if (playInitial) {
            mPlayer.playUri(mOperationCallback, nextTrack.getUri(), 0, 0);
            for (int id : REQUIRES_PLAYING_STATE) {
                findViewById(id).setEnabled(true);
            }
            playInitial = false;
            updateView();
        }
        else {
            onPauseButtonClicked(view);
        }
    }

    public void onPauseButtonClicked(View view) {
        if(!checkIfClicked)
            mMetadataText.setVisibility(View.INVISIBLE);
        if (mCurrentPlaybackState != null && mCurrentPlaybackState.isPlaying) {
            mPlayer.pause(mOperationCallback);
            Button playButton = (Button) findViewById(R.id.play_track_button);
            playButton.setText(R.string.resume_button_label_toggle);

        } else {
            mPlayer.resume(mOperationCallback);
            Button playButton = (Button) findViewById(R.id.play_track_button);
            playButton.setText(R.string.pause_button_label_toggle);
        }
    }

    public void createQueue(String artistID, String trackID) {
        Recommendation rec = SpotifyFactory.getRecommendation(authToken, artistID, "", trackID);
        List<Track> tracks = rec.getTracks();
        if(tracks != null && tracks.size() > 0)
        {
            nextTrack = tracks.get(random.nextInt(tracks.size()-1));

            if(nextTrack != null) {
                int size = (5 < tracks.size()) ? 5 : tracks.size();
                for (int i = 0; i < size; i++) {
                    mPlayer.queue(mOperationCallback, tracks.get(i).getUri());
                }
            }
        }
    }

    public void onDislikeButtonClicked(View view) {
        if(!checkIfClicked)
            mMetadataText.setVisibility(View.INVISIBLE);
        List<Artist> artists = nextTrack.getArtists();
        if (artists.size() > 0) {
            String artistID = parseIDfromURI(artists.get(0).getUri());
            String trackID = parseIDfromURI(nextTrack.getUri());

            createQueue(artistID, trackID);
            mPlayer.playUri(mOperationCallback, nextTrack.getUri(), 0, 0);
        }
    }

    public void onSkipSongButtonClicked(View view) {
        if(!checkIfClicked)
            mMetadataText.setVisibility(View.INVISIBLE);

        mPlayer.skipToNext(mOperationCallback);
    }

    private static String parseIDfromURI(String uri)
    {
        String[] args = uri.split(":");
        return args[args.length-1];
    }

    public void onLikeButtonClicked(View view) {
        mMetadataText.setVisibility(View.VISIBLE);
        String artistID = parseIDfromURI(mMetadata.currentTrack.artistUri);
        String trackID = parseIDfromURI(mMetadata.currentTrack.uri);

        if(playlistUri != null && userId != null)
            SpotifyFactory.addTrackToPlaylist(authToken,userId,parseIDfromURI(playlistUri),mMetadata.currentTrack.uri);

        createQueue(artistID, trackID);
    }


    //   ____      _ _ _                _      __  __      _   _               _
    //  / ___|__ _| | | |__   __ _  ___| | __ |  \/  | ___| |_| |__   ___   __| |___
    // | |   / _` | | | '_ \ / _` |/ __| |/ / | |\/| |/ _ \ __| '_ \ / _ \ / _` / __|
    // | |__| (_| | | | |_) | (_| | (__|   <  | |  | |  __/ |_| | | | (_) | (_| \__ \
    //  \____\__,_|_|_|_.__/ \__,_|\___|_|\_\ |_|  |_|\___|\__|_| |_|\___/ \__,_|___/
    //

    @Override
    public void onLoggedIn() {
        logStatus("Login complete");
        updateView();
    }

    @Override
    public void onLoggedOut() {
        playInitial = true;
        Button playButton = (Button) findViewById(R.id.play_track_button);
        playButton.setText(R.string.play_track_button_label);
        logStatus("Logout complete");
        updateView();
    }

    public void onLoginFailed(int error) {
        logStatus("Login error "+ error);
    }

    @Override
    public void onTemporaryError() {
        logStatus("Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(final String message) {
        logStatus("Incoming connection message: " + message);
    }

    //  _____                       _   _                 _ _ _
    // | ____|_ __ _ __ ___  _ __  | | | | __ _ _ __   __| | (_)_ __   __ _
    // |  _| | '__| '__/ _ \| '__| | |_| |/ _` | '_ \ / _` | | | '_ \ / _` |
    // | |___| |  | | | (_) | |    |  _  | (_| | | | | (_| | | | | | | (_| |
    // |_____|_|  |_|  \___/|_|    |_| |_|\__,_|_| |_|\__,_|_|_|_| |_|\__, |
    //                                                                 |___/

    /**
     * Print a status message from a callback (or some other place) to the TextView in this
     * activity
     *
     * @param status Status message
     */
    private void logStatus(String status) {
        Log.i(TAG, status);
    }

    //  ____            _                   _   _
    // |  _ \  ___  ___| |_ _ __ _   _  ___| |_(_) ___  _ __
    // | | | |/ _ \/ __| __| '__| | | |/ __| __| |/ _ \| '_ \
    // | |_| |  __/\__ \ |_| |  | |_| | (__| |_| | (_) | | | |
    // |____/ \___||___/\__|_|   \__,_|\___|\__|_|\___/|_| |_|
    //

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mNetworkStateReceiver);

        // Note that calling Spotify.destroyPlayer() will also remove any callbacks on whatever
        // instance was passed as the refcounted owner. So in the case of this particular example,
        // it's not strictly necessary to call these methods, however it is generally good practice
        // and also will prevent your application from doing extra work in the background when
        // paused.
        if (mPlayer != null) {
            mPlayer.removeNotificationCallback(MainActivity.this);
            mPlayer.removeConnectionStateCallback(MainActivity.this);
        }
    }

    @Override
    protected void onDestroy() {
        // *** ULTRA-IMPORTANT ***
        // ALWAYS call this in your onDestroy() method, otherwise you will leak native resources!
        // This is an unfortunate necessity due to the different memory management models of
        // Java's garbage collector and C++ RAII.
        // For more information, see the documentation on Spotify.destroyPlayer().
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    @Override
    public void onPlaybackEvent(PlayerEvent event) {
        logStatus("Event: " + event);
        mCurrentPlaybackState = mPlayer.getPlaybackState();
        mMetadata = mPlayer.getMetadata();
        Log.i(TAG, "Player state: " + mCurrentPlaybackState);
        Log.i(TAG, "Metadata: " + mMetadata);
        updateView();
    }

    @Override
    public void onPlaybackError(Error error) {
        logStatus("Err: " + error);
    }
}