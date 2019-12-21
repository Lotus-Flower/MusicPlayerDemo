package meehan.matthew.musicplayerdemo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import android.os.Build
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import androidx.media.session.MediaButtonReceiver

class MusicNotificationManager(private val musicPlayerService: MusicPlayerService) {
    var notificationManager: NotificationManager = musicPlayerService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var playAction: Notification.Action
    private var pauseAction: Notification.Action
    private var previousAction: Notification.Action
    private var nextAction: Notification.Action

    init {
        playAction = Notification.Action(
            R.drawable.ic_play_arrow_black_24dp,
            PLAY,
            createActionIntent(musicPlayerService, KeyEvent.KEYCODE_MEDIA_PLAY)
        )

        pauseAction = Notification.Action(
            R.drawable.ic_pause_black_24dp,
            PAUSE,
            createActionIntent(musicPlayerService, KeyEvent.KEYCODE_MEDIA_PAUSE)
        )

        previousAction = Notification.Action(
            R.drawable.ic_skip_previous_black_24dp,
            PREVIOUS,
            createActionIntent(musicPlayerService, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        )

        nextAction = Notification.Action(
            R.drawable.ic_skip_next_black_24dp,
            NEXT,
            createActionIntent(musicPlayerService, KeyEvent.KEYCODE_MEDIA_NEXT)

        )
    }

    fun getNotification(songTitle: String?, songArtist: String?, token: MediaSession.Token, state: Int): Notification {
        val builder = buildNotification(songTitle, songArtist, token, state)
        return builder.build()
    }

    private fun buildNotification(songTitle: String?, songArtist: String?, token: MediaSession.Token, state: Int) : Notification.Builder {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createChannel()

        val isPlaying = (state == PlaybackStateCompat.STATE_PLAYING) or (state == PlaybackStateCompat.STATE_BUFFERING)

        val builder = Notification.Builder(musicPlayerService, CHANNEL_ID)

        builder.setStyle(Notification.MediaStyle()
            .setMediaSession(token)
            .setShowActionsInCompactView(0, 1, 2)
        )
            .setSmallIcon(R.drawable.ic_play_arrow_black_24dp)
            .setContentTitle(songTitle)
            .setContentText(songArtist)
            .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    musicPlayerService, PlaybackStateCompat.ACTION_STOP
                )
            )
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(previousAction)
            .addAction(if (isPlaying) pauseAction else playAction)
            .addAction(nextAction)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)

        return builder
    }

    private fun createActionIntent(context: Context, mediaKeyCode: Int): PendingIntent {
        val intent = Intent(context, MusicPlayerService::class.java)
        intent.action = Intent.ACTION_MEDIA_BUTTON
        intent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, mediaKeyCode))
        return PendingIntent.getService(context, mediaKeyCode, intent, 0)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        if (null == notificationManager.getNotificationChannel(CHANNEL_ID)) {
            notificationManager.createNotificationChannel(NotificationChannel(CHANNEL_ID, CANONICAL_NAME, NotificationManager.IMPORTANCE_LOW))
        }
    }

    companion object {
        const val PLAY = "play"
        const val PAUSE = "pause"
        const val PREVIOUS = "previous"
        const val NEXT = "next"

        const val NOTIFICATION_ID = 999

        private val CANONICAL_NAME = MusicNotificationManager::class.java.simpleName
        private val CHANNEL_ID = "$CANONICAL_NAME.channel"
    }
}