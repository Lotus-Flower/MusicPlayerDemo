package meehan.matthew.musicplayerdemo

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val songsList = ArrayList<Song>()

    private lateinit var songAdapter: SongRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupUI()
    }

    private fun setupUI() {
        setupRecyclerView()

        if (checkPermissions(this)) {
            getMp3Songs()
            updateAdapter()
        }
    }

    private fun updateAdapter() {
        songAdapter = SongRecyclerAdapter(songsList)
        song_recycler.adapter = songAdapter
    }

    private fun setupRecyclerView() {

        songAdapter = SongRecyclerAdapter(ArrayList())

        song_recycler.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this.context)
            adapter = songAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun checkPermissions(
        context: Context
    ): Boolean {
        val currentAPIVersion = Build.VERSION.SDK_INT
        if (currentAPIVersion >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        context as Activity,
                        READ_EXTERNAL_STORAGE
                    )
                ) {
                    showDialog()
                } else {
                    ActivityCompat
                        .requestPermissions(
                            context,
                            arrayOf(READ_EXTERNAL_STORAGE),
                            REQUEST_READ_EXTERNAL_STORAGE
                        )
                }
                return false
            } else {
                return true
            }

        } else {
            return true
        }
    }

    private fun showDialog(
    ) {
        val alertBuilder = AlertDialog.Builder(this)
        alertBuilder.setCancelable(true)
        alertBuilder.setTitle("Permission necessary")
        alertBuilder.setMessage("File permission is necessary")
        alertBuilder.setPositiveButton(android.R.string.yes
        ) { _, _ ->
            ActivityCompat.requestPermissions(
                this,
                arrayOf(),
                REQUEST_READ_EXTERNAL_STORAGE
            )
        }
        val alert = alertBuilder.create()
        alert.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_READ_EXTERNAL_STORAGE -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
               getMp3Songs()
            } else {
                Toast.makeText(
                    this, "Permission Denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> super.onRequestPermissionsResult(
                requestCode, permissions,
                grantResults
            )
        }
    }

    private fun getMp3Songs() {
        val allSongsUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selectionMimeType = MediaStore.Files.FileColumns.MIME_TYPE + "=?"
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3")
        val selectionArgsMp3 = arrayOf(mimeType)
        val cursor = this.contentResolver.query(allSongsUri, null, selectionMimeType, selectionArgsMp3, null)

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val song = Song(
                        cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)),
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)),
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)),
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)),
                        Uri.EMPTY,
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                    )

                    val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                    val albumArtUri = ContentUris.withAppendedId(sArtworkUri, cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)).toLong())

                    song.art = albumArtUri
                    songsList.add(song)
                } while (cursor.moveToNext())
            }
            cursor.close()
        }
    }

    companion object {
        private const val REQUEST_READ_EXTERNAL_STORAGE = 123
    }
}
