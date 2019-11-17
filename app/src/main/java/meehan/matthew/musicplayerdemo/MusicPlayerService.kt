package meehan.matthew.musicplayerdemo

import android.media.browse.MediaBrowser
import android.os.Bundle
import android.service.media.MediaBrowserService

class MusicPlayerService : MediaBrowserService() {
    override fun onLoadChildren(p0: String, p1: Result<MutableList<MediaBrowser.MediaItem>>) {
        p1.sendResult(mutableListOf())
    }

    override fun onGetRoot(p0: String, p1: Int, p2: Bundle?): BrowserRoot? {
        return BrowserRoot(MUSIC_ROOT_ID, null)
    }

    companion object {
        const val MUSIC_ROOT_ID = "MUSIC_ROOT_ID"
    }
}