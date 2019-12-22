package meehan.matthew.musicplayerdemo

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.net.Uri
import android.provider.MediaStore
import android.service.media.MediaBrowserService
import android.webkit.MimeTypeMap

class MusicProvider(private val context: Context) {

    var songsList = ArrayList<MediaBrowser.MediaItem>()

    fun getMp3Songs() {
        songsList = ArrayList()
        val allSongsUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selectionMimeType = MediaStore.Files.FileColumns.MIME_TYPE + "=?"
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3")
        val selectionArgsMp3 = arrayOf(mimeType)
        val cursor =
            context.contentResolver.query(allSongsUri, null, selectionMimeType, selectionArgsMp3, null)

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                    val albumArtUri = ContentUris.withAppendedId(
                        sArtworkUri,
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)).toLong()
                    )

                    getMetadata(cursor, albumArtUri)?.description?.let {
                        songsList.add(MediaBrowser.MediaItem(it, MediaBrowser.MediaItem.FLAG_PLAYABLE))
                    }

                } while (cursor.moveToNext())
            }
            cursor.close()
        }
    }

    fun sendMediaResults(result: MediaBrowserService.Result<MutableList<MediaBrowser.MediaItem>>) {
        result.sendResult(songsList)
    }

    private fun getMetadata(cursor: Cursor, albumArtUri: Uri): MediaMetadata? {
        val metadataBuilder = MediaMetadata.Builder()

        metadataBuilder.putString(MediaMetadata.METADATA_KEY_MEDIA_ID, cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID)))
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_MEDIA_URI, cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)))
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)))
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST, cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)))
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM, cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)))
        metadataBuilder.putLong(MediaMetadata.METADATA_KEY_DURATION, cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)))
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, albumArtUri.toString())

        return metadataBuilder.build()
    }
}