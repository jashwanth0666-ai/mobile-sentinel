package com.mobilesentinel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.app.PendingIntent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AlarmService : Service() {
    private var player: MediaPlayer? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopAlarm(); return START_NOT_STICKY }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("sentinel_alarm", "Sentinel alarm", NotificationManager.IMPORTANCE_HIGH))
        val notification = NotificationCompat.Builder(this, "sentinel_alarm")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Lost phone alarm")
            .setContentText("Your Mobile Sentinel account triggered this alarm")
            .addAction(0, "Stop alarm", PendingIntent.getBroadcast(this, 1,
                Intent(this, AlarmStopReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .setPriority(NotificationCompat.PRIORITY_MAX).setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(43, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else startForeground(43, notification)
        if (player == null) {
            player = MediaPlayer.create(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))?.apply { isLooping = true; start() }
        }
        return START_STICKY
    }
    private fun stopAlarm() { player?.stop(); player?.release(); player = null; stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
    override fun onDestroy() { player?.release(); player = null; super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
    companion object { const val ACTION_STOP = "com.mobilesentinel.STOP_ALARM" }
}
