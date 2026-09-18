package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.language.AppLanguage
import com.example.ui.language.AppStrings
import com.example.ui.theme.AppThemeMode

@Composable
fun HeaderBar(
    currentLanguage: AppLanguage,
    onLanguageSelect: (AppLanguage) -> Unit,
    currentTheme: AppThemeMode = AppThemeMode.SKY_BLUE,
    onCycleTheme: () -> Unit = {},
    onThemeSelect: (AppThemeMode) -> Unit = {},
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    cartCount: Int,
    onCartClick: () -> Unit,
    onAdminClick: () -> Unit,
    onAboutClick: () -> Unit = {},
    onMapClick: () -> Unit = {}
) {
    var langDropdownExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val logoResId = remember(context) {
        val id = context.resources.getIdentifier("img_app_icon_1786037564036", "drawable", context.packageName)
        if (id != 0) id else android.R.drawable.ic_dialog_info
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Store Branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Image(
                    painter = painterResource(id = logoResId),
                    contentDescription = "Noor Store Logo",
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Column {
                    Text(
                        text = AppStrings.get("app_title", currentLanguage),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                    Text(
                        text = AppStrings.get("app_subtitle", currentLanguage),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Right Action Controls: Language Switcher (Flag only), 4-Theme Switcher, Day/Night, Cart, Admin
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Language Switcher Button - Pure Flag Only (no text)
                Box {
                    IconButton(
                        onClick = { langDropdownExpanded = true },
                        modifier = Modifier
                            .size(34.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .testTag("language_button")
                    ) {
                        CountryFlag(
                            language = currentLanguage,
                            width = 24.dp,
                            height = 16.dp,
                            cornerRadius = 2.dp
                        )
                    }

                    // Flag Selection Menu with pure flag icons (no text)
                    DropdownMenu(
                        expanded = langDropdownExpanded,
                        onDismissRequest = { langDropdownExpanded = false }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            AppLanguage.values().forEach { lang ->
                                val isSelected = lang == currentLanguage
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 0.5.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else Color.Black.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            onLanguageSelect(lang)
                                            langDropdownExpanded = false
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    CountryFlag(
                                        language = lang,
                                        width = 30.dp,
                                        height = 20.dp,
                                        cornerRadius = 3.dp
                                    )
                                }
                            }
                        }
                    }
                }

                // 1-Click 4-Theme Switcher Button
                Box {
                    IconButton(
                        onClick = onCycleTheme,
                        modifier = Modifier
                            .size(34.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .border(1.5.dp, currentTheme.previewPrimary, CircleShape)
                            .testTag("theme_cycle_button")
                    ) {
                        Text(
                            text = currentTheme.iconEmoji,
                            fontSize = 15.sp
                        )
                    }
                }

                // Day / Night Theme Toggle Button
                IconButton(
                    onClick = onToggleDarkMode,
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            if (isDarkMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                        .testTag("dark_mode_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Dark/Light Mode",
                        tint = if (isDarkMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Cart Icon with badge
                BadgedBox(
                    badge = {
                        if (cartCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Text(text = cartCount.toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                ) {
                    IconButton(
                        onClick = onCartClick,
                        modifier = Modifier
                            .size(34.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .testTag("cart_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Cart",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Admin Button
                IconButton(
                    onClick = onAdminClick,
                    modifier = Modifier
                        .size(34.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .testTag("admin_icon_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // About Us Button
                IconButton(
                    onClick = onAboutClick,
                    modifier = Modifier
                        .size(34.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .testTag("about_icon_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About Us",
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
