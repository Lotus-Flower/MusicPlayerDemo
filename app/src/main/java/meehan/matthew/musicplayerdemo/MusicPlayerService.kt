package meehan.matthew.musicplayerdemo

import android.app.Service
import android.content.Intent
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
    private lateinit var mediaSessionManager: MediaSessionManager

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

        val notificationManager = MusicNotificationManager(this)

        mediaSessionManager = MediaSessionManager(MediaPlayer(), mediaSession, musicProvider, notificationManager, this)

        mediaSession.setCallback(mediaSessionManager)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mediaSessionManager.onMediaButtonEvent(intent)
        return Service.START_STICKY
    }

    override fun onLoadChildren(p0: String, p1: Result<MutableList<MediaBrowser.MediaItem>>) {
        musicProvider.getMp3Songs()
        musicProvider.sendMediaResults(p1)
    }

    override fun onGetRoot(p0: String, p1: Int, p2: Bundle?): BrowserRoot? {
        return BrowserRoot(MUSIC_ROOT_ID, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSelf()
    }

    companion object {
        const val MUSIC_ROOT_ID = "MUSIC_ROOT_ID"
        const val MUSIC_PROGRESS_UPDATE = "MUSIC_PROGRESS_UPDATE"
        @JvmField val TAG: String = MusicPlayerService::class.java.simpleName
    }
}