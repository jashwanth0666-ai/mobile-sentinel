package com.mobilesentinel

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/** User-started command standby. It keeps an owner-scoped Firestore listener alive. */
class LostModeService : Service() {
    private val client by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var commandListener: ListenerRegistration? = null
    private val processing = mutableSetOf<String>()
    private var lostMode = false
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { SentinelData.writeLocation(this@LostModeService, it.latitude, it.longitude, it.accuracy) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        lostMode = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_LOST_MODE, false)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Remote protection", NotificationManager.IMPORTANCE_LOW))
        startForeground(NOTIFICATION_ID, notification())
        startLocationUpdates()
        listenForCommands()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LOST_MODE -> setLostMode(true)
            ACTION_ARM -> setLostMode(false)
        }
        return START_STICKY
    }

    private fun notification(): Notification = NotificationCompat.Builder(this, CHANNEL)
        .setSmallIcon(android.R.drawable.ic_lock_lock)
        .setContentTitle(if (lostMode) "Mobile Sentinel · Lost mode" else "Mobile Sentinel · Armed")
        .setContentText(if (lostMode) "Sharing location and listening for commands" else "Listening for remote commands; periodic location is on")
        .setOngoing(true)
        .build()

    private fun setLostMode(enabled: Boolean) {
        lostMode = enabled
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_LOST_MODE, enabled).apply()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification())
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return
        client.removeLocationUpdates(locationCallback)
        val interval = if (lostMode) 30_000L else 15 * 60_000L
        val minimum = if (lostMode) 15_000L else 5 * 60_000L
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, interval)
            .setMinUpdateIntervalMillis(minimum).build()
        try { client.requestLocationUpdates(request, locationCallback, mainLooper) }
        catch (_: SecurityException) { /* Permission can be revoked while the service is active. */ }
    }

    private fun listenForCommands() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val deviceId = SentinelData.deviceId(this)
        commandListener = FirebaseFirestore.getInstance().collection("users").document(uid).collection("commands")
            .whereEqualTo("deviceId", deviceId).whereEqualTo("status", "queued")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documents?.forEach { command ->
                    if (processing.add(command.id)) processCommand(uid, command.id, command.getString("action"), command.getLong("createdAt"))
                }
            }
    }

    private fun processCommand(uid: String, commandId: String, action: String?, createdAt: Long?) {
        val command = FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("commands").document(commandId)
        val now = System.currentTimeMillis()
        if (createdAt == null || now - createdAt > COMMAND_MAX_AGE_MS || createdAt > now + 30_000L) {
            command.update(mapOf("status" to "expired", "completedAt" to now))
                .addOnCompleteListener { processing.remove(commandId) }
            return
        }
        val result = try {
            when (action) {
                "LOCK" -> {
                    val policy = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                    val admin = ComponentName(this, SentinelAdminReceiver::class.java)
                    if (!policy.isAdminActive(admin)) "needs_admin" else { policy.lockNow(); "completed" }
                }
                "ALARM" -> {
                    val intent = Intent(this, AlarmService::class.java)
                    if (Build.VERSION.SDK_INT >= 26) ContextCompat.startForegroundService(this, intent) else startService(intent)
                    "started"
                }
                "STOP_ALARM" -> { stopService(Intent(this, AlarmService::class.java)); "stopped" }
                "START_LOST_MODE" -> { setLostMode(true); "tracking" }
                "STOP_LOST_MODE" -> { setLostMode(false); "armed" }
                else -> "invalid_command"
            }
        } catch (_: Exception) { "failed" }
        command.update(mapOf("status" to result, "completedAt" to System.currentTimeMillis()))
            .addOnCompleteListener { processing.remove(commandId) }
    }

    override fun onDestroy() {
        commandListener?.remove()
        client.removeLocationUpdates(locationCallback)
        SentinelData.markOffline(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val PREFS = "sentinel_device"
        private const val KEY_LOST_MODE = "lost_mode"
        private const val CHANNEL = "lost_mode"
        private const val NOTIFICATION_ID = 42
        private const val COMMAND_MAX_AGE_MS = 10 * 60 * 1000L
        const val ACTION_ARM = "com.mobilesentinel.ARM"
        const val ACTION_START_LOST_MODE = "com.mobilesentinel.START_LOST_MODE"
    }
}
