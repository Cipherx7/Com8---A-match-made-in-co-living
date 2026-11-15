package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoM8ThemeTokens
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.util.CoM8PermissionHelper

@Composable
fun PermissionsPrePromptScreen(
    onFinished: (notificationsGranted: Boolean, coarseLocationGranted: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokens = CoM8ThemeTokens.colors
    val missingPermissions = CoM8PermissionHelper.missingPostOnboardingPermissions(context)
    val needsNotifications = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        !CoM8PermissionHelper.hasNotificationPermission(context)
    val needsLocation = !CoM8PermissionHelper.hasCoarseLocationPermission(context)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            results[Manifest.permission.POST_NOTIFICATIONS]
                ?: CoM8PermissionHelper.hasNotificationPermission(context)
        } else {
            true
        }
        val locGranted = results[Manifest.permission.ACCESS_COARSE_LOCATION]
            ?: CoM8PermissionHelper.hasCoarseLocationPermission(context)
        onFinished(notifGranted, locGranted)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(tokens.successBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = "Permissions setup",
                        tint = tokens.successFg,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Stay in the loop",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        lineHeight = 32.sp,
                        letterSpacing = (-0.3).sp,
                        color = tokens.onSurface
                    )
                    Text(
                        text = "Allow these two optional permissions so CoM8 can alert you when flatmates match and tailor neighborhood context.",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        lineHeight = 22.5.sp,
                        color = tokens.onSurfaceMuted
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (needsNotifications) {
                    PermissionReasonCard(
                        icon = Icons.Outlined.NotificationsActive,
                        title = "Match & chat notifications",
                        subtitle = "Know right away when someone likes your profile or sends a message."
                    )
                }

                if (needsLocation) {
                    PermissionReasonCard(
                        icon = Icons.Outlined.LocationOn,
                        title = "Approximate location",
                        subtitle = "Highlights nearby neighborhoods without tracking your exact address."
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = tokens.successBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = "Privacy protected",
                            tint = tokens.successFg,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "You can change these anytime in Android Settings or CoM8 Settings.",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = tokens.successFg
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        if (missingPermissions.isEmpty()) {
                            onFinished(
                                CoM8PermissionHelper.hasNotificationPermission(context),
                                CoM8PermissionHelper.hasCoarseLocationPermission(context)
                            )
                        } else {
                            permissionLauncher.launch(missingPermissions.toTypedArray())
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("allow_permissions_button"),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Allow permissions",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable {
                            onFinished(
                                CoM8PermissionHelper.hasNotificationPermission(context),
                                CoM8PermissionHelper.hasCoarseLocationPermission(context)
                            )
                        }
                        .padding(horizontal = 20.dp)
                        .testTag("skip_permissions_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Not now",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = tokens.onSurfaceMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionReasonCard(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val tokens = CoM8ThemeTokens.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = tokens.surface,
        border = BorderStroke(1.dp, tokens.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(tokens.successBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tokens.successFg,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = tokens.onSurface
                )
                Text(
                    text = subtitle,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = tokens.onSurfaceMuted
                )
            }
        }
    }
}
