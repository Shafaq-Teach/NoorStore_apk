package com.example.ui.components

import android.media.RingtoneManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.remote.AppUpdateInfo
import com.example.data.remote.GitHubUpdateChecker
import kotlinx.coroutines.launch

@Composable
fun AppUpdateDialog(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Play celebration chime on dialog launch
    LaunchedEffect(Unit) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.play()
        } catch (e: Exception) {}
    }

    // Gentle confetti / flower scale pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "celebration")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Dialog(onDismissRequest = {
            if (!isDownloading) onDismiss()
        }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Celebration Header with Confetti & Flowers
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF059669), Color(0xFF0284C7), Color(0xFF6366F1))
                                )
                            )
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Floating Celebration Emojis & Flowers
                            Row(
                                modifier = Modifier.scale(pulseScale),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🎉", fontSize = 28.sp)
                                Text(text = "🌸", fontSize = 32.sp)
                                Text(text = "✨", fontSize = 26.sp)
                                Text(text = "🌺", fontSize = 32.sp)
                                Text(text = "🎊", fontSize = 28.sp)
                            }

                            Text(
                                text = "${updateInfo.latestVersionName} نەشرى چىقتى!",
                                fontWeight = FontWeight.Black,
                                fontSize = 21.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "دېتالنى ئەڭ يېڭى ھالەتكە يېڭىلاڭ",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.92f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Content Body: Clean, Clutter-Free
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (!isDownloading) {
                            Text(
                                text = "يېڭى نەشرگە ھازىرلا يېڭىلامسىز؟",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "دېتال ئىچىدە بىۋاستە چۈشۈرۈلۈۋاتىدۇ...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                LinearProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp),
                                    color = Color(0xFF10B981),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                Text(
                                    text = "${(downloadProgress * 100).toInt()}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        errorMessage?.let { err ->
                            Text(
                                text = err,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Action Buttons: "ھەئە" / "كېيىنچە"
                        if (!isDownloading) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // "ھەئە" Button
                                Button(
                                    onClick = {
                                        isDownloading = true
                                        errorMessage = null
                                        coroutineScope.launch {
                                            GitHubUpdateChecker.downloadAndInstallApk(
                                                context = context,
                                                downloadUrl = updateInfo.apkDownloadUrl,
                                                onProgress = { p -> downloadProgress = p },
                                                onError = { err ->
                                                    isDownloading = false
                                                    errorMessage = err
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981)
                                    )
                                ) {
                                    Text(
                                        text = "ھەئە",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                }

                                // "كېيىنچە" Button
                                OutlinedButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(
                                        text = "كېيىنچە",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
