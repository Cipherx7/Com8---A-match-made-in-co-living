package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object CoM8PermissionHelper {

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasCoarseLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun missingPostOnboardingPermissions(context: Context): List<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(context)) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (!hasCoarseLocationPermission(context)) {
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }
}

object CoM8NotificationHelper {
    private const val CHANNEL_ID = "com8_matches_messages"
    private const val CHANNEL_NAME = "Matches & Messages"
    private const val CHANNEL_DESC = "Notifications when you match with a flatmate or receive a message."

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = CHANNEL_DESC
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun postMatchOrMessageNotification(
        context: Context,
        title: String,
        body: String,
        notificationId: Int,
        userSettingEnabled: Boolean
    ) {
        if (!userSettingEnabled || !CoM8PermissionHelper.hasNotificationPermission(context)) {
            return
        }
        ensureChannel(context)
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        kotlin.runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }
}

object CoM8LocationHelper {
    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    suspend fun detectApproximateCity(
        context: Context,
        supportedCities: List<String>
    ): String? = withContext(Dispatchers.IO) {
        if (!CoM8PermissionHelper.hasCoarseLocationPermission(context)) {
            return@withContext null
        }
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext null
        val location = kotlin.runCatching {
            manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: manager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        }.getOrNull() ?: return@withContext null

        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = kotlin.runCatching {
            geocoder.getFromLocation(location.latitude, location.longitude, 1)
        }.getOrNull().orEmpty()
        val first = addresses.firstOrNull() ?: return@withContext null
        val rawCity = first.locality ?: first.subAdminArea ?: first.adminArea ?: return@withContext null
        supportedCities.firstOrNull { candidate ->
            rawCity.contains(candidate, ignoreCase = true) || candidate.contains(rawCity, ignoreCase = true)
        } ?: rawCity
    }
}
