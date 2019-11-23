package meehan.matthew.musicplayerdemo

import android.content.Context
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle

class MediaSessionManager(
    private val player: MediaPlayer,
    private val mediaSession: MediaSession,
    private val musicProvider: MusicProvider,
    private val context: Context
) : MediaSession.Callback() {

    private var currentSong: MediaBrowser.MediaItem? = null

    override fun onPrepare() {
        super.onPrepare()

        currentSong = musicProvider.songsList.firstOrNull()

        setMetadata()

        currentSong?.description?.mediaUri?.let {
            player.setDataSource(context, it)
            player.prepare()
        }

        player.setOnCompletionListener {
            onSkipToNext()
        }
        setSessionPlaybackState(PlaybackState.STATE_PAUSED)
    }

    override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) {
        super.onPrepareFromUri(uri, extras)
        uri?.let {
            player.reset()
            player.setDataSource(context, uri)
            player.prepare()
            onPlay()
        }
    }

    override fun onPlay() {
        super.onPlay()
        player.start()
        setSessionPlaybackState(PlaybackState.STATE_PLAYING)
    }

    override fun onPause() {
        super.onPause()
        player.pause()
        setSessionPlaybackState(PlaybackState.STATE_PAUSED)
    }

    override fun onSkipToNext() {
        super.onSkipToNext()
        if (musicProvider.songsList.indexOf(currentSong) < musicProvider.songsList.size - 1) {
            currentSong = musicProvider.songsList[musicProvider.songsList.indexOf(currentSong) + 1]
            currentSong?.description?.mediaUri?.let {
                player.reset()
                player.setDataSource(context, it)
                player.prepare()
                setMetadata()
                onPlay()
            }
        }
    }

    override fun onSkipToPrevious() {
        super.onSkipToPrevious()
        if (musicProvider.songsList.indexOf(currentSong) > 0) {
            currentSong = musicProvider.songsList[musicProvider.songsList.indexOf(currentSong) - 1]
            currentSong?.description?.mediaUri?.let {
                player.reset()
                player.setDataSource(context, it)
                player.prepare()
                setMetadata()
                onPlay()
            }
        }
    }

    override fun onSkipToQueueItem(id: Long) {
        super.onSkipToQueueItem(id)
        currentSong = musicProvider.songsList.find {
            it.mediaId?.toLong() == id
        }
        currentSong?.description?.mediaUri?.let {
            player.reset()
            player.setDataSource(context, it)
            player.prepare()
            onPlay()
        }
    }

    override fun onStop() {
        super.onStop()
        player.release()
    }

    private fun setMetadata() {
        mediaSession.setMetadata(MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, currentSong?.description?.title.toString())
            .putString(MediaMetadata.METADATA_KEY_ARTIST, currentSong?.description?.subtitle.toString())
            .putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, currentSong?.description?.iconUri.toString())
            .build())
    }

    private fun setSessionPlaybackState(playbackState: Int) {
        mediaSession.setPlaybackState(PlaybackState.Builder().setState(playbackState, player.currentPosition.toLong(), player.playbackParams.speed).build())
    }

}