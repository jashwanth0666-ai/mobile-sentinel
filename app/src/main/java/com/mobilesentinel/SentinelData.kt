package com.mobilesentinel

import android.content.Context
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

object SentinelData {
    private const val PREFS = "sentinel_device"
    private val db get() = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    fun deviceId(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString("id", null) ?: UUID.randomUUID().toString().also {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("id", it).apply()
        }

    fun enrollProtectedDevice(context: Context, done: (Result<Unit>) -> Unit = {}) {
        val uid = auth.currentUser?.uid ?: return done(Result.failure(IllegalStateException("Sign in first")))
        val id = deviceId(context)
        val user = db.collection("users").document(uid)
        val device = user.collection("devices").document(id)
        val now = System.currentTimeMillis()
        val name = "${Build.MANUFACTURER} ${Build.MODEL}"
        db.batch().set(user, mapOf("protectedDeviceId" to id, "protectedDeviceName" to name, "createdAt" to now))
            .set(device, mapOf("deviceId" to id, "deviceName" to name, "platform" to "android", "updatedAt" to now, "online" to true))
            .commit()
            .addOnSuccessListener { done(Result.success(Unit)) }
            .addOnFailureListener { done(Result.failure(it)) }
    }

    fun writeLocation(context: Context, lat: Double, lon: Double, accuracy: Float) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("devices").document(deviceId(context))
            .update(mapOf("latitude" to lat, "longitude" to lon, "accuracyMeters" to accuracy,
                "locationAt" to System.currentTimeMillis(), "updatedAt" to System.currentTimeMillis(), "online" to true))
    }

    fun markOffline(context: Context) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("devices").document(deviceId(context))
            .update(mapOf("online" to false, "updatedAt" to System.currentTimeMillis()))
    }
}
