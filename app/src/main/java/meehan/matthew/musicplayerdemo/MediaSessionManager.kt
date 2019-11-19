package meehan.matthew.musicplayerdemo

import android.content.Context
import android.media.MediaPlayer
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.net.Uri
import android.os.Bundle

class MediaSessionManager(
    private val player: MediaPlayer,
    private val musicProvider: MusicProvider,
    private val context: Context
) : MediaSession.Callback() {

    private var currentSong: MediaBrowser.MediaItem? = null

    override fun onPrepare() {
        super.onPrepare()

        currentSong = musicProvider.songsList.firstOrNull()

        currentSong?.description?.mediaUri?.let {
            player.setDataSource(context, it)
            player.prepare()
        }

        player.setOnCompletionListener {
            onSkipToNext()
        }
    }

    override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) {
        super.onPrepareFromUri(uri, extras)
        player.setDataSource(context, uri)
    }

    override fun onPlay() {
        super.onPlay()
        player.start()
    }

    override fun onPause() {
        super.onPause()
        player.pause()
    }

    override fun onSkipToNext() {
        super.onSkipToNext()
        if (musicProvider.songsList.indexOf(currentSong) < musicProvider.songsList.size - 1) {
            currentSong = musicProvider.songsList[musicProvider.songsList.indexOf(currentSong) + 1]
            currentSong?.description?.mediaUri?.let {
                player.setDataSource(context, it)
                player.prepare()
                onPlay()
            }
        }
    }

    override fun onSkipToPrevious() {
        super.onSkipToPrevious()
        if (musicProvider.songsList.indexOf(currentSong) > 0) {
            currentSong = musicProvider.songsList[musicProvider.songsList.indexOf(currentSong) - 1]
            currentSong?.description?.mediaUri?.let {
                player.setDataSource(context, it)
                player.prepare()
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
            player.setDataSource(context, it)
            player.prepare()
            onPlay()
        }
    }

    override fun onStop() {
        super.onStop()
        player.release()
    }

}