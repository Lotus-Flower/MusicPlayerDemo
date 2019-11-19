package meehan.matthew.musicplayerdemo

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.current_song_bottom_sheet.*

class MainActivity : AppCompatActivity() {

    private lateinit var mediaBrowser: MediaBrowser
    private val callback: MediaBrowser.SubscriptionCallback = object : MediaBrowser.SubscriptionCallback(){
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowser.MediaItem>) {
            super.onChildrenLoaded(parentId, children)
            updateAdapter(children)
        }
    }
    private lateinit var songAdapter: SongRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupUI()

        if (checkPermissions(this)) {
            createMediaBrowser()
        }
    }

    override fun onStart() {
        super.onStart()
        if (checkPermissions(this)) {
            mediaBrowser.connect()
            mediaBrowser.subscribe(MusicPlayerService.MUSIC_ROOT_ID, callback)
        }
    }

    override fun onStop() {
        super.onStop()
        if (checkPermissions(this)) {
            mediaBrowser.unsubscribe(MusicPlayerService.MUSIC_ROOT_ID)
            mediaBrowser.disconnect()
        }
    }

    public override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    private fun setupUI() {
        setupRecyclerView()
        setBottomSheetBehavior()
        current_song_play_button.setOnClickListener {
            mediaController.transportControls.prepare()
            mediaController.transportControls.play()
        }
    }

    private fun updateAdapter(songsList: MutableList<MediaBrowser.MediaItem>) {
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

    private fun setBottomSheetBehavior() {
        val behavior = BottomSheetBehavior.from(bottom_sheet)
        behavior.isFitToContents = false
    }

    private fun checkPermissions(context: Context): Boolean {
        val currentAPIVersion = Build.VERSION.SDK_INT
        if (currentAPIVersion >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        context as Activity,
                        READ_EXTERNAL_STORAGE)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_READ_EXTERNAL_STORAGE -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createMediaBrowser()
                mediaBrowser.subscribe(MusicPlayerService.MUSIC_ROOT_ID, callback)
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

    private fun showDialog() {
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

    private fun createMediaBrowser() {
        mediaBrowser = MediaBrowser(
            this,
            ComponentName(this, MusicPlayerService::class.java),
            object : MediaBrowser.ConnectionCallback() {
                override fun onConnected() {
                    super.onConnected()

                    // Get the token for the MediaSession
                    mediaBrowser.sessionToken.also { token ->

                        // Create a MediaControllerCompat
                        val mediaController = MediaController(
                            this@MainActivity, // Context
                            token
                        )

                        // Save the controller
                        this@MainActivity.mediaController = mediaController
                    }
                }
            },
            null
        )
    }

    companion object {
        private const val REQUEST_READ_EXTERNAL_STORAGE = 123
    }
}
