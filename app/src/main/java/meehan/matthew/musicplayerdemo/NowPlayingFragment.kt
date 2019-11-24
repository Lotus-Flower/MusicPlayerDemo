package meehan.matthew.musicplayerdemo

import android.content.ComponentName
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_now_playing.*

class NowPlayingFragment : Fragment() {

    private lateinit var mediaBrowser: MediaBrowser
    private val callback: MediaBrowser.SubscriptionCallback = object : MediaBrowser.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowser.MediaItem>) {
            super.onChildrenLoaded(parentId, children)
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
            updateButtonUI()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createMediaBrowser()
    }

    override fun onStart() {
        super.onStart()

        mediaBrowser.connect()
        mediaBrowser.subscribe(MusicPlayerService.MUSIC_ROOT_ID, callback)
        requireActivity().mediaController?.registerCallback(controllerCallback)
        updateButtonUI()
    }

    override fun onResume() {
        super.onResume()
        requireActivity().volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onStop() {
        super.onStop()

        requireActivity().mediaController?.unregisterCallback(controllerCallback)
        mediaBrowser.subscribe(MusicPlayerService.MUSIC_ROOT_ID, callback)
        mediaBrowser.disconnect()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_now_playing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupUI()
    }

    private fun setupUI() {
        now_playing_previous_button.setOnClickListener {
            requireActivity().mediaController.transportControls.skipToPrevious()
        }

        now_playing_play_button.setOnClickListener {
            when (requireActivity().mediaController?.playbackState?.state) {
                PlaybackState.STATE_PAUSED -> {
                    requireActivity().mediaController.transportControls.play()
                }
                PlaybackState.STATE_PLAYING -> {
                    requireActivity().mediaController.transportControls.pause()
                }
            }
        }

        now_playing_next_button.setOnClickListener {
            requireActivity().mediaController.transportControls.skipToNext()
        }
    }

    private fun updateButtonUI() {
        when (requireActivity().mediaController?.playbackState?.state) {
            PlaybackState.STATE_PAUSED -> {
                now_playing_play_button.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_play_arrow_black_24dp))
            }
            PlaybackState.STATE_PLAYING -> {
                now_playing_play_button.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_pause_black_24dp))
            }
        }
    }

    private fun setupMetadataUI(metadata: MediaMetadata?) {
        now_playing_title_text_view.text = metadata?.description?.title
        now_playing_artist_text_view.text = metadata?.description?.subtitle
        now_playing_image_view.setImageURI(metadata?.description?.iconUri)
    }

    private fun createMediaBrowser() {
        mediaBrowser = MediaBrowser(
            context,
            ComponentName(requireActivity(), MusicPlayerService::class.java),
            object : MediaBrowser.ConnectionCallback() {
                override fun onConnected() {
                    super.onConnected()

                    // Get the token for the MediaSession
                    mediaBrowser.sessionToken.also { token ->

                        if (requireActivity().mediaController == null) {
                            // Create a MediaControllerCompat
                            val mediaController = MediaController(
                                this@NowPlayingFragment.requireContext(), // Context
                                token
                            )

                            mediaController.registerCallback(controllerCallback)
                            // Save the controller
                            requireActivity().mediaController = mediaController
                        } else {
                            requireActivity().mediaController.registerCallback(controllerCallback)
                        }
                    }
                }
            },
            null
        )
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}