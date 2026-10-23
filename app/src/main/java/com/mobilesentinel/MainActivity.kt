package com.mobilesentinel

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.DateFormat
import java.util.Date

private val Ink = Color(0xFF1C1C1E)
private val SurfaceDark = Color(0xFFFFFFFF)
private val Muted = Color(0xFF6D6D72)
private val Mint = Color(0xFF34C759)
private val Gold = Color(0xFFFF9F0A)
private val SystemGrouped = Color(0xFFF2F2F7)
private val SystemBlue = Color(0xFF007AFF)
private val SystemRed = Color(0xFFFF3B30)
private val InterFamily = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Medium),
    Font(R.font.inter_variable, FontWeight.SemiBold),
    Font(R.font.inter_variable, FontWeight.Bold)
)
private val SentinelTypography = Typography().let { t ->
    val sans = InterFamily
    t.copy(
        displayLarge = t.displayLarge.copy(fontFamily = sans), displayMedium = t.displayMedium.copy(fontFamily = sans), displaySmall = t.displaySmall.copy(fontFamily = sans),
        headlineLarge = t.headlineLarge.copy(fontFamily = sans), headlineMedium = t.headlineMedium.copy(fontFamily = sans), headlineSmall = t.headlineSmall.copy(fontFamily = sans),
        titleLarge = t.titleLarge.copy(fontFamily = sans), titleMedium = t.titleMedium.copy(fontFamily = sans), titleSmall = t.titleSmall.copy(fontFamily = sans),
        bodyLarge = t.bodyLarge.copy(fontFamily = sans), bodyMedium = t.bodyMedium.copy(fontFamily = sans), bodySmall = t.bodySmall.copy(fontFamily = sans),
        labelLarge = t.labelLarge.copy(fontFamily = sans), labelMedium = t.labelMedium.copy(fontFamily = sans), labelSmall = t.labelSmall.copy(fontFamily = sans)
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = true
        setContent { SentinelApp(this) }
    }
}

private data class DeviceRecord(
    val id: String, val name: String, val online: Boolean, val lat: Double?, val lon: Double?, val locationAt: Long?, val lastSeenAt: Long?, val current: Boolean
)

@Composable
private fun SentinelApp(activity: ComponentActivity) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Mint, onPrimary = Color.White, background = SystemGrouped, surface = SurfaceDark,
        onSurface = Ink, secondary = SystemBlue, error = SystemRed), typography = SentinelTypography) {
      CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = InterFamily)) {
        Surface(Modifier.fillMaxSize(), color = Color.Transparent) {
          Box(Modifier.fillMaxSize()) {
            Image(painter = painterResource(R.drawable.mobilesentinel_wallpaper), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            val auth = remember { FirebaseAuth.getInstance() }
            var user by remember { mutableStateOf(auth.currentUser) }
            var busy by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }
            DisposableEffect(auth) { val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }; auth.addAuthStateListener(listener); onDispose { auth.removeAuthStateListener(listener) } }
            if (user == null) AuthScreen(busy, error,
                onSignIn = { email, pass -> busy = true; error = null; auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { busy = false; if (!it.isSuccessful) error = it.exception?.localizedMessage } },
                onCreate = { email, pass -> busy = true; error = null; auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { busy = false; if (!it.isSuccessful) error = it.exception?.localizedMessage } })
            else Dashboard(activity, user!!.uid) { auth.signOut() }
          }
        }
      }
    }
}

@Composable
private fun AuthScreen(busy: Boolean, error: String?, onSignIn: (String, String) -> Unit, onCreate: (String, String) -> Unit) {
    var showAbout by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), verticalArrangement = Arrangement.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(R.drawable.mobilesentinel_logo), contentDescription = "Mobile Sentinel emblem", contentScale = ContentScale.Fit, modifier = Modifier.size(76.dp))
            Column(Modifier.padding(start = 14.dp)) { Text("Mobile Sentinel", color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Personal device security", color = Muted, style = MaterialTheme.typography.bodySmall) }
        }
        Spacer(Modifier.height(42.dp)); Text("Sign in", color = Ink, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        Text("Use the same account on your spare phone to reach your protected device.", color = Muted, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
        IOSGroup {
            OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true, shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(12.dp)); OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(12.dp))
        }
        error?.let { Text(it, color = SystemRed, modifier = Modifier.padding(top = 10.dp)) }
        Spacer(Modifier.height(18.dp))
        Button(onClick = { onSignIn(email.trim(), password) }, enabled = !busy && email.isNotBlank() && password.length >= 6, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Sign in")
        }
        TextButton(onClick = { onCreate(email.trim(), password) }, enabled = !busy && email.isNotBlank() && password.length >= 6, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(48.dp)) { Text("Create personal account", color = SystemBlue) }
        Text("Commands are limited to the one device registered to this account.", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 18.dp))
        TextButton(onClick = { showAbout = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("About Mobile Sentinel", color = SystemBlue) }
    }
    if (showAbout) AboutDialog(onDismiss = { showAbout = false })
}

@Composable
private fun Dashboard(activity: ComponentActivity, uid: String, signOut: () -> Unit) {
    var showAbout by remember { mutableStateOf(false) }
    val context = activity
    val currentId = remember { SentinelData.deviceId(context) }
    val devices = remember { mutableStateListOf<DeviceRecord>() }
    var protectedDeviceId by remember { mutableStateOf<String?>(null) }
    var profileLoaded by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var requestedLostMode by remember { mutableStateOf(false) }
    var locationEnabled by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    ) }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        locationEnabled = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationEnabled) startProtectionService(context, requestedLostMode) else message = "Location access was not granted."
    }
    val beginProtection: (Boolean) -> Unit = { lostMode ->
        requestedLostMode = lostMode
        val hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasLocation) startProtectionService(context, lostMode)
        else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    val admin = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val adminComponent = ComponentName(context, SentinelAdminReceiver::class.java)
    val adminEnabled = remember { mutableStateOf(admin.isAdminActive(adminComponent)) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                adminEnabled.value = admin.isAdminActive(adminComponent)
                locationEnabled = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(uid) {
        val profile = FirebaseFirestore.getInstance().collection("users").document(uid)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) { message = exception.localizedMessage; profileLoaded = true; return@addSnapshotListener }
                protectedDeviceId = snapshot?.getString("protectedDeviceId")
                profileLoaded = true
            }
        onDispose { profile.remove() }
    }

    DisposableEffect(uid, protectedDeviceId) {
        devices.clear()
        val id = protectedDeviceId
        if (id == null) return@DisposableEffect onDispose { }
        val registration: ListenerRegistration = FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("devices").document(id).addSnapshotListener { doc, exception ->
                if (exception != null) { message = exception.localizedMessage; return@addSnapshotListener }
                devices.clear()
                if (doc != null && doc.exists()) devices.add(DeviceRecord(doc.id, doc.getString("deviceName") ?: "Android phone",
                    doc.getBoolean("online") == true, doc.getDouble("latitude"), doc.getDouble("longitude"),
                    doc.getLong("locationAt"), doc.getLong("updatedAt"), doc.id == currentId))
            }
        onDispose { registration.remove() }
    }

    val isProtectedPhone = protectedDeviceId != null && protectedDeviceId == currentId

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("Mobile Sentinel", color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(if (isProtectedPhone) "Protected phone" else "Controller", color = Muted, style = MaterialTheme.typography.bodyMedium) }
            TextButton(onClick = { if (isProtectedPhone) context.stopService(Intent(context, LostModeService::class.java)); signOut() }) { Text("Sign out", color = SystemBlue) }
        }
        Row(Modifier.padding(top = 20.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color(0xFFE8F8EC), shape = CircleShape, modifier = Modifier.size(48.dp)) { androidx.compose.material3.Icon(Icons.Default.Security, null, Modifier.padding(12.dp), tint = Mint) }
            Column(Modifier.padding(start = 14.dp)) {
                Text(if (protectedDeviceId == null) "No protected phone" else if (isProtectedPhone) "This is your protected phone" else "Controller mode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(if (protectedDeviceId == null) "Enroll your original phone once" else "One protected phone on this account", color = Muted)
            }
        }
        if (!profileLoaded) {
            Text("Loading your device setupâ€¦", color = Muted, modifier = Modifier.padding(top = 24.dp))
        } else if (protectedDeviceId == null) {
            Text("Choose your original phone and enroll it as the only protected device. Sign in on your spare phone afterward to use it as a controller; it will not register itself.", color = Muted, modifier = Modifier.padding(top = 18.dp))
            Button(onClick = {
                message = "Enrolling this phoneâ€¦"
                SentinelData.enrollProtectedDevice(context) { result ->
                    message = if (result.isSuccess) "This phone is now the protected device." else result.exceptionOrNull()?.localizedMessage ?: "Could not enroll this phone."
                }
            }, modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(52.dp)) { Text("Enroll this phone as protected") }
        } else if (!isProtectedPhone) {
            Text("This phone is a controller only. It is not registered or tracked. Use the controls below to locate and secure your protected phone.", color = Muted, modifier = Modifier.padding(top = 18.dp))
        }
        if (isProtectedPhone) {
        SectionTitle("THIS PHONE")
        IOSGroup {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusBlock("Battery", "${(context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager).getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceAtLeast(0)}%", Modifier.weight(1f))
            StatusBlock("Remote lock", if (adminEnabled.value) "Ready" else "Set up", Modifier.weight(1f))
            StatusBlock("Location", if (locationEnabled) "On" else "Off", Modifier.weight(1f))
        }
        }
        SectionTitle("PROTECTION")
        IOSGroup {
        ActionRow("Enable remote lock", if (adminEnabled.value) "Device admin is active" else "Required once on every protected phone", "Set up") {
            if (!adminEnabled.value) context.startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent).putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Allow Mobile Sentinel to lock this device when you request it from your account."))
            adminEnabled.value = admin.isAdminActive(adminComponent)
        }
        ActionRow("Arm remote protection", "Keep a visible service listening for commands; location reports every 15 minutes", "Arm") { beginProtection(false) }
        ActionRow("Start lost mode", "Increase location reports to about every 30 seconds", "Start") { beginProtection(true) }
        ActionRow("Disarm remote protection", "Stops the command listener and location service", "Stop") { context.stopService(Intent(context, LostModeService::class.java)) }
        ActionRow("Allow all the time", "Improves location access if Android restarts the service", "Settings") {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        }
        ActionRow("Allow notifications", "Show the persistent armed-service notification", "Settings") {
            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }
        }
        }
        message?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }

        Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("PROTECTED PHONE")
            Text("Refresh", color = SystemBlue, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(8.dp))
        }
        if (profileLoaded && protectedDeviceId != null && devices.isEmpty()) Text("Waiting for the protected phone recordâ€¦", color = Muted, modifier = Modifier.padding(vertical = 12.dp))
        if (devices.isNotEmpty()) IOSGroup { devices.forEach { DeviceRow(it, context, currentId, onResult = { message = it }) } }
        if (protectedDeviceId != null) Text("Remote commands need internet. The protected phone must be armed before it is lost; Android requires setup for remote locking and location access.", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
        TextButton(onClick = { showAbout = true }, modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)) { Text("About Mobile Sentinel", color = SystemBlue) }
    }
    if (showAbout) AboutDialog(onDismiss = { showAbout = false })
}


@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mobile Sentinel") },
        text = {
            Column {
                Text("Purpose", fontWeight = FontWeight.SemiBold)
                Text("A personal Android anti-theft companion. Register one protected phone, then use another signed-in phone as a controller to view its last reported location and send supported security commands.", modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
                Text("Developer", fontWeight = FontWeight.SemiBold)
                Text("B Jashwanth Sai", modifier = Modifier.padding(top = 4.dp))
                Text("AI ? Apps ? Automation | Python ? Java ? ML", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))
                TextButton(onClick = { uriHandler.openUri("https://github.com/jashwanth0666-ai") }) { Text("GitHub profile", color = SystemBlue) }
                TextButton(onClick = { uriHandler.openUri("https://jashwanthonline.netlify.app/") }) { Text("Portfolio website", color = SystemBlue) }
                TextButton(onClick = { uriHandler.openUri("https://www.instagram.com/jashwanth1_17") }) { Text("Instagram", color = SystemBlue) }
                TextButton(onClick = { uriHandler.openUri("https://www.linkedin.com/in/jashwanth-sai-57601a300") }) { Text("LinkedIn", color = SystemBlue) }
                Text("Version 1.0.1", color = Muted)
                Text("Remote actions require the protected phone to be armed, online, and configured with the required Android permissions. Location is reported periodically; this app does not guarantee recovery of a lost device.", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 10.dp))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable private fun SectionTitle(text: String) {
    Text(text, color = Muted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium,
        letterSpacing = 0.35.sp, modifier = Modifier.padding(start = 14.dp, top = 18.dp, bottom = 7.dp))
}

@Composable
private fun IOSGroup(content: @Composable ColumnScope.() -> Unit) {
    val glassShape = RoundedCornerShape(20.dp)
    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).animateContentSize()
        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.82f), Color.White.copy(alpha = 0.58f))), glassShape)
        .border(1.dp, Color.White.copy(alpha = 0.82f), glassShape)
        .padding(horizontal = 16.dp, vertical = 12.dp), content = content)
}

@Composable
private fun StatusBlock(label: String, value: String, modifier: Modifier) {
    Column(modifier.background(Color.White.copy(alpha = 0.46f), RoundedCornerShape(14.dp)).border(1.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 12.dp)) {
        Text(label, color = Muted, style = MaterialTheme.typography.labelSmall); Text(value, color = Ink, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun ActionRow(title: String, detail: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) { Text(title, color = Ink, fontWeight = FontWeight.Medium); Text(detail, color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 3.dp)) }
        TextButton(onClick = onClick, modifier = Modifier.padding(start = 6.dp)) { Text(action, color = SystemBlue) }
    }
}

@Composable
private fun DeviceRow(device: DeviceRecord, context: Context, currentId: String, onResult: (String) -> Unit) {
    val time = device.locationAt?.let { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it)) } ?: "No location yet"
    val seen = device.lastSeenAt?.let { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it)) }
    val recentlySeen = device.online && device.lastSeenAt?.let { System.currentTimeMillis() - it < 180_000L } == true
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(Icons.Default.Smartphone, null, tint = if (recentlySeen || device.current) Mint else Muted, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(device.name, color = Ink, fontWeight = FontWeight.Medium); Text(if (device.current) "This phone" else if (seen != null) "Last contact Â· $seen" else if (recentlySeen) "Online" else "Waiting for contact", color = Muted, style = MaterialTheme.typography.bodySmall) }
        }
        if (device.lat != null && device.lon != null) {
            Row(Modifier.padding(start = 34.dp, top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(Icons.Default.LocationOn, null, tint = Mint, modifier = Modifier.size(16.dp))
                Text("${"%.5f".format(device.lat)}, ${"%.5f".format(device.lon)} Â· $time", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = {
                val label = Uri.encode(device.name)
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${device.lat},${device.lon}?q=${device.lat},${device.lon}($label)")))
            }, modifier = Modifier.padding(start = 26.dp, top = 2.dp)) { Text("Open in Maps", color = SystemBlue) }
        } else Text(time, color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 34.dp, top = 4.dp))
        if (!device.current) {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ControllerAction("Locate", SystemBlue, Modifier.weight(1f)) { sendCommand(device.id, "START_LOST_MODE", onResult) }
                ControllerAction("Lock", Ink, Modifier.weight(1f)) { sendCommand(device.id, "LOCK", onResult) }
                ControllerAction("Alarm", SystemRed, Modifier.weight(1f)) { sendCommand(device.id, "ALARM", onResult) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ControllerAction("Stop tracking", Muted, Modifier.weight(1f)) { sendCommand(device.id, "STOP_LOST_MODE", onResult) }
                ControllerAction("Silence alarm", Muted, Modifier.weight(1f)) { sendCommand(device.id, "STOP_ALARM", onResult) }
            }
        }
    }
    androidx.compose.material3.HorizontalDivider(color = Color(0xFFE5E5EA))
}

@Composable
private fun ControllerAction(label: String, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = modifier.background(SystemGrouped, RoundedCornerShape(10.dp))) {
        Text(label, color = tint, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

private fun sendCommand(deviceId: String, action: String, onResult: (String) -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser ?: return onResult("Sign in before sending a command.")
    onResult("Queueing " + action + "...")
    FirebaseFirestore.getInstance().collection("users").document(user.uid).collection("commands")
        .add(mapOf("deviceId" to deviceId, "action" to action, "status" to "queued", "createdAt" to System.currentTimeMillis()))
        .addOnSuccessListener { onResult("$action queued. The target must be armed and connected to receive it.") }
        .addOnFailureListener { onResult(it.localizedMessage ?: "Could not queue command.") }
}

private fun startProtectionService(context: Context, lostMode: Boolean) {
    val intent = Intent(context, LostModeService::class.java).setAction(
        if (lostMode) LostModeService.ACTION_START_LOST_MODE else LostModeService.ACTION_ARM)
    try { ContextCompat.startForegroundService(context, intent) }
    catch (e: Exception) { android.widget.Toast.makeText(context, "Open Sentinel to start lost mode: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show() }
}
