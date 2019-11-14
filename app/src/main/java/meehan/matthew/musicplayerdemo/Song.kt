package meehan.matthew.musicplayerdemo

import android.net.Uri

data class Song(var id: Int,
                var title: String,
                var artist: String,
                var album: String,
                var albumId: String,
                var art : Uri,
                var duration: String)