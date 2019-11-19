package meehan.matthew.musicplayerdemo

import android.media.MediaPlayer
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.service.media.MediaBrowserService

class MusicPlayerService : MediaBrowserService() {

    private var musicProvider: MusicProvider = MusicProvider(this)
    private lateinit var mediaSession: MediaSession
    private lateinit var stateBuilder: PlaybackState.Builder

    override fun onCreate() {
        super.onCreate()

        // Create a MediaSessionCompat
        mediaSession = MediaSession(baseContext, TAG).apply {

            // Enable callbacks from MediaButtons and TransportControls
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS
                    or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
            stateBuilder = PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY
                        or PlaybackState.ACTION_PLAY_PAUSE
                )
            setPlaybackState(stateBuilder.build())

            // MySessionCallback() has methods that handle callbacks from a media controller
            //setCallback(MySessionCallback())

            // Set the session's token so that client activities can communicate with it.
            setSessionToken(sessionToken)
        }

        val mediaSessionManager = MediaSessionManager(MediaPlayer(), musicProvider, this)

        mediaSession.setCallback(mediaSessionManager)
    }

    override fun onLoadChildren(p0: String, p1: Result<MutableList<MediaBrowser.MediaItem>>) {
        musicProvider.getMp3Songs()
        musicProvider.sendMediaResults(p1)
    }

    override fun onGetRoot(p0: String, p1: Int, p2: Bundle?): BrowserRoot? {
        return BrowserRoot(MUSIC_ROOT_ID, null)
    }

    companion object {
        const val MUSIC_ROOT_ID = "MUSIC_ROOT_ID"
        @JvmField val TAG: String = MusicPlayerService::class.java.simpleName
    }
}