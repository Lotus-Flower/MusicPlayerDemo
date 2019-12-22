package meehan.matthew.musicplayerdemo

import android.Manifest
import android.app.AlertDialog
import android.content.ComponentName
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.current_song_bottom_sheet.*
import kotlinx.android.synthetic.main.fragment_song_list.*

class SongListFragment : Fragment() {

    private lateinit var songAdapter: SongRecyclerAdapter

    private var mediaBrowser: MediaBrowser? = null

    private val callback: MediaBrowser.SubscriptionCallback = object : MediaBrowser.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowser.MediaItem>) {
            super.onChildrenLoaded(parentId, children)
            updateAdapter(children)
            requireActivity().mediaController.transportControls.prepare()
        }
    }

    private val controllerCallback: MediaController.Callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            super.onMetadataChanged(metadata)
            setupMetadataUI(metadata)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            super.onPlaybackStateChanged(state)
            setupButtonUI()

            when (state?.state) {
                PlaybackState.STATE_PLAYING -> showBottomSheet()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (checkPermissions()) {
            createMediaBrowser()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_song_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)

        setupUI()
    }

    private fun setupUI() {
        setupRecyclerView()
        setBottomSheetBehavior()
        hideBottomSheet()

        current_song_play_button.setOnClickListener {
            when (requireActivity().mediaController?.playbackState?.state) {
                PlaybackState.STATE_PAUSED -> {
                    requireActivity().mediaController.transportControls.play()
                    current_song_play_button.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_pause_black_24dp))
                }
                PlaybackState.STATE_PLAYING -> {
                    requireActivity().mediaController.transportControls.pause()
                    current_song_play_button.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_play_arrow_black_24dp))
                }
            }
        }

        current_song_constraint_layout.setOnClickListener {
            requireActivity()
                .supportFragmentManager
                ?.beginTransaction()
                ?.replace(R.id.main_activity_frame_layout, NowPlayingFragment())
                ?.commit()
        }
    }

    private fun updateAdapter(songsList: MutableList<MediaBrowser.MediaItem>) {
        songAdapter = SongRecyclerAdapter(songsList) {
            requireActivity().mediaController?.transportControls?.playFromMediaId(it.mediaId, null)
        }
        song_recycler.adapter = songAdapter
    }

    private fun setupRecyclerView() {

        songAdapter = SongRecyclerAdapter(ArrayList()) {}

        song_recycler.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this.context)
            adapter = songAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setBottomSheetBehavior() {
        val behavior = BottomSheetBehavior.from(bottom_sheet_card)
        behavior.isFitToContents = false
        behavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(p0: View, p1: Float) {
            }

            override fun onStateChanged(p0: View, p1: Int) {
                when (p1) {
                    BottomSheetBehavior.STATE_DRAGGING -> behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    else -> {}
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        if (checkPermissions()) {
            mediaBrowser?.connect()
            mediaBrowser?.subscribe(MusicPlayerService.MUSIC_ROOT_ID, callback)
            requireActivity().mediaController?.registerCallback(controllerCallback)
            setupButtonUI()
        }
    }

    override fun onStop() {
        super.onStop()
        if (checkPermissions()) {
            requireActivity().mediaController?.unregisterCallback(controllerCallback)
            mediaBrowser?.subscribe(MusicPlayerService.MUSIC_ROOT_ID, callback)
            mediaBrowser?.disconnect()
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().volumeControlStream = AudioManager.STREAM_MUSIC
    }

    private fun createMediaBrowser() {
        mediaBrowser = MediaBrowser(
            context,
            ComponentName(requireActivity(), MusicPlayerService::class.java),
            object : MediaBrowser.ConnectionCallback() {
                override fun onConnected() {
                    super.onConnected()

                    // Get the token for the MediaSession
                    mediaBrowser?.sessionToken.also { token ->

                        if (requireActivity().mediaController == null) {
                            // Create a MediaControllerCompat
                            token?.let {
                                val mediaController = MediaController(
                                    this@SongListFragment.requireContext(), // Context
                                    token
                                )

                                mediaController.registerCallback(controllerCallback)
                                // Save the controller
                                requireActivity().mediaController = mediaController
                            }
                        } else {
                            requireActivity().mediaController?.registerCallback(controllerCallback)
                        }

                        when (requireActivity().mediaController?.playbackState?.state) {
                            PlaybackState.STATE_PLAYING -> showBottomSheet()
                        }
                    }
                }
            },
            null
        )
    }

    private fun checkPermissions(): Boolean {
        val currentAPIVersion = Build.VERSION.SDK_INT
        if (currentAPIVersion >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        requireActivity(),
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                ) {
                    showDialog()
                } else {
                    requestPermissions(
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
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
        if (grantResults.isNotEmpty()){
            when (requestCode) {
                REQUEST_READ_EXTERNAL_STORAGE -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    createMediaBrowser()
                    mediaBrowser?.connect()
                    mediaBrowser?.subscribe(MusicPlayerService.MUSIC_ROOT_ID, callback)
                } else {
                    Toast.makeText(
                        context, "Permission Denied",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> super.onRequestPermissionsResult(
                    requestCode, permissions,
                    grantResults
                )
            }
        }
    }

    private fun showDialog() {
        val alertBuilder = AlertDialog.Builder(context)
        alertBuilder.setCancelable(true)
        alertBuilder.setTitle("Permission necessary")
        alertBuilder.setMessage("File permission is necessary")
        alertBuilder.setPositiveButton(android.R.string.yes
        ) { _, _ ->
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(),
                REQUEST_READ_EXTERNAL_STORAGE
            )
        }
        val alert = alertBuilder.create()
        alert.show()
    }

    private fun setupMetadataUI(metadata: MediaMetadata?) {
        current_song_image_view.setImageURI(metadata?.description?.iconUri)
        current_song_title_text_view.text = metadata?.description?.title
        current_song_artist_text_view.text = metadata?.description?.subtitle
    }

    private fun setupButtonUI() {
        when (requireActivity().mediaController?.playbackState?.state) {
            PlaybackState.STATE_PAUSED -> {
                current_song_play_button.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_play_arrow_black_24dp))
            }
            PlaybackState.STATE_PLAYING -> {
                current_song_play_button.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_pause_black_24dp))
            }
        }
    }

    private fun showBottomSheet() {
        bottom_sheet_card.visibility = View.VISIBLE
        current_song_linear_layout.visibility = View.VISIBLE
        current_song_title_text_view.isSelected = true
    }

    private fun hideBottomSheet() {
        bottom_sheet_card.visibility = View.GONE
        current_song_linear_layout.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_READ_EXTERNAL_STORAGE = 123
    }
}