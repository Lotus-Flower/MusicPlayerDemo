package meehan.matthew.musicplayerdemo

import android.app.Notification
import android.content.Intent
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import java.util.*

class MediaSessionManager(
    private val player: MediaPlayer,
    private val mediaSession: MediaSession,
    private val musicProvider: MusicProvider,
    private val notificationManager: MusicNotificationManager,
    private val musicPlayerService: MusicPlayerService
) : MediaSession.Callback() {

    private var currentSong: MediaBrowser.MediaItem? = null
    private var timer: Timer = Timer()

    private var serviceInStartedState: Boolean = false
    private var notification: Notification? = null

    override fun onPrepare() {
        super.onPrepare()

        if (currentSong == null) {
            currentSong = musicProvider.songsList.firstOrNull()

            currentSong?.description?.mediaUri?.let {
                player.reset()
                player.setDataSource(musicPlayerService, it)
                player.prepare()
            }

            player.setOnCompletionListener {
                onSkipToNext()
            }
            player.setOnErrorListener { _, _, _ ->
                true
            }
            setSessionPlaybackState(PlaybackState.STATE_PAUSED)
        }

        setMetadata()
    }

    override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
        super.onPlayFromUri(uri, extras)
        uri?.let {
            player.reset()
            player.setDataSource(musicPlayerService, uri)
            player.prepare()
            setMetadata()
            onPlay()
        }
    }

    override fun onPlay() {
        super.onPlay()
        player.start()
        mediaSession.isActive = true
        setSessionPlaybackState(PlaybackState.STATE_PLAYING)
        startMusicProgress()
        if (!serviceInStartedState) startService() else updateNotification()
    }

    override fun onPause() {
        super.onPause()
        player.pause()
        setSessionPlaybackState(PlaybackState.STATE_PAUSED)
        stopMusicProgress()
        pauseNotification()
    }

    override fun onSkipToNext() {
        super.onSkipToNext()
        val currentIndex = musicProvider.songsList.indexOfFirst {
            it.mediaId == currentSong?.mediaId
        }
        if (currentIndex < musicProvider.songsList.size - 1) {
            currentSong = musicProvider.songsList[currentIndex + 1]
            currentSong?.description?.mediaUri?.let {
                player.reset()
                player.setDataSource(musicPlayerService, it)
                player.prepare()
                setMetadata()
                onPlay()
            }
        }
    }

    override fun onSkipToPrevious() {
        super.onSkipToPrevious()
        val currentIndex = musicProvider.songsList.indexOfFirst {
            it.mediaId == currentSong?.mediaId
        }
        if (currentIndex > 0) {
            currentSong = musicProvider.songsList[currentIndex - 1]
            currentSong?.description?.mediaUri?.let {
                player.reset()
                player.setDataSource(musicPlayerService, it)
                player.prepare()
                setMetadata()
                onPlay()
            }
        }
    }

    override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
        super.onPlayFromMediaId(mediaId, extras)
        currentSong = musicProvider.songsList.find {
            it.mediaId == mediaId
        }
        currentSong?.description?.mediaUri?.let {
            player.reset()
            player.setDataSource(musicPlayerService, it)
            player.prepare()
            setMetadata()
            onPlay()
        }
    }

    override fun onSkipToQueueItem(id: Long) {
        super.onSkipToQueueItem(id)
        currentSong = musicProvider.songsList.find {
            it.mediaId?.toLong() == id
        }
        currentSong?.description?.mediaUri?.let {
            player.reset()
            player.setDataSource(musicPlayerService, it)
            player.prepare()
            onPlay()
        }
    }

    override fun onStop() {
        super.onStop()
        player.release()
    }

    override fun onSeekTo(pos: Long) {
        super.onSeekTo(pos)
        player.seekTo(pos.toInt())
    }

    private fun setMetadata() {
        mediaSession.setMetadata(MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, currentSong?.description?.title.toString())
            .putString(MediaMetadata.METADATA_KEY_ARTIST, currentSong?.description?.subtitle.toString())
            .putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, currentSong?.description?.iconUri.toString())
            .putLong(MediaMetadata.METADATA_KEY_DURATION, player.duration.toLong())
            .build())
    }

    private fun setSessionPlaybackState(playbackState: Int) {
        mediaSession.setPlaybackState(PlaybackState.Builder().setState(playbackState, player.currentPosition.toLong(), player.playbackParams.speed).build())
    }

    private fun startService() {
        ContextCompat.startForegroundService(musicPlayerService, Intent(musicPlayerService, MusicPlayerService::class.java))
        serviceInStartedState = true
        playNotification()
    }

    private fun playNotification() {

        notification = notificationManager.getNotification(currentSong?.description?.title.toString(),
            currentSong?.description?.subtitle.toString(), mediaSession.sessionToken,
            PlaybackStateCompat.STATE_PLAYING)

        notification?.let {
            musicPlayerService.startForeground(MusicNotificationManager.NOTIFICATION_ID, it)
        }
    }

    private fun updateNotification() {

       notification = notificationManager.getNotification(currentSong?.description?.title.toString(),
           currentSong?.description?.subtitle.toString(), mediaSession.sessionToken,
           PlaybackStateCompat.STATE_PLAYING)

        notification?.let { notificationManager.notificationManager.notify(MusicNotificationManager.NOTIFICATION_ID, it) }
    }

    private fun pauseNotification() {
        notification = notificationManager.getNotification(currentSong?.description?.title.toString(),
            currentSong?.description?.subtitle.toString(), mediaSession.sessionToken,
            PlaybackStateCompat.STATE_PAUSED)

        notification?.let {
            notificationManager.notificationManager.notify(MusicNotificationManager.NOTIFICATION_ID, it)
            musicPlayerService.stopForeground(false)
            serviceInStartedState = false
        }
    }

    private fun startMusicProgress() {
        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                val bundle = Bundle()
                bundle.putInt(MusicPlayerService.MUSIC_PROGRESS_UPDATE, player.currentPosition)
                mediaSession.sendSessionEvent(MusicPlayerService.MUSIC_PROGRESS_UPDATE, bundle)
            }
        }, 0, 1000)
    }

    private fun stopMusicProgress() {
        timer.cancel()
    }

    override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
        val event = mediaButtonEvent?.extras?.get(Intent.EXTRA_KEY_EVENT)
        event?.let {
            val keyEvent = event as KeyEvent
            when (keyEvent.action == KeyEvent.ACTION_DOWN) {
                true -> {
                    when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            when (mediaSession.controller.playbackState?.state) {
                                PlaybackState.STATE_PLAYING -> this.onPause()
                                PlaybackState.STATE_PAUSED -> this.onPlay()
                            }
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY -> this.onPlay()
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> this.onPause()
                        KeyEvent.KEYCODE_MEDIA_NEXT -> this.onSkipToNext()
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> this.onSkipToPrevious()
                    }
                }
            }
        }
        return super.onMediaButtonEvent(mediaButtonEvent)
    }
}