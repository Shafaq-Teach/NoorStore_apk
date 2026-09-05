package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ProductEntity
import com.example.data.local.ReviewEntity
import com.example.ui.components.ProductImageView
import com.example.ui.language.AppLanguage
import com.example.ui.language.AppStrings
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SapphireBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProductDetailScreen(
    product: ProductEntity?,
    reviews: List<ReviewEntity> = emptyList(),
    currentLanguage: AppLanguage,
    onBackClick: () -> Unit,
    onAddToCart: (ProductEntity) -> Unit,
    onOrderWhatsApp: (ProductEntity) -> Unit,
    onOrderTelegram: (ProductEntity) -> Unit,
    onCallStore: () -> Unit,
    onLikeClick: () -> Unit = {},
    onHeartClick: () -> Unit = {},
    onToggleCompare: ((ProductEntity) -> Unit)? = null,
    isCompared: Boolean = false,
    onAddReview: (userName: String, comment: String) -> Unit = { _, _ -> }
) {
    if (product == null) return

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showWriteReviewDialog by remember { mutableStateOf(false) }
    var reviewerName by remember { mutableStateOf("") }
    var reviewComment by remember { mutableStateOf("") }

    val name = remember(product, currentLanguage) {
        when (currentLanguage) {
            AppLanguage.UYGHUR -> product.nameUg
            AppLanguage.ARABIC -> product.nameAr
            AppLanguage.ENGLISH -> product.nameEn
        }
    }

    val description = remember(product, currentLanguage) {
        when (currentLanguage) {
            AppLanguage.UYGHUR -> product.descriptionUg
            AppLanguage.ARABIC -> product.descriptionAr
            AppLanguage.ENGLISH -> product.descriptionEn
        }
    }

    val specs = remember(product, currentLanguage) {
        when (currentLanguage) {
            AppLanguage.UYGHUR -> product.specsUg
            AppLanguage.ARABIC -> product.specsAr
            AppLanguage.ENGLISH -> product.specsEn
        }
    }

    val allImages = remember(product) { product.getAllImages() }
    var selectedImageIndex by remember { mutableStateOf(0) }
    val currentImage = allImages.getOrElse(selectedImageIndex) { allImages.first() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Top Image Preview with Back Button & Multi-Image Gallery
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                ProductImageDisplay(
                    imagePath = currentImage,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Back Button
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .testTag("detail_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Image counter badge if multiple images
                if (allImages.size > 1) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.BottomEnd)
                    ) {
                        Text(
                            text = "${selectedImageIndex + 1} / ${allImages.size}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Thumbnail selector row for up to 3 images
            if (allImages.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                ) {
                    allImages.forEachIndexed { index, imgPath ->
                        val isSelected = index == selectedImageIndex
                        Surface(
                            onClick = { selectedImageIndex = index },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) GoldPrimary else Color.Transparent
                            ),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            ProductImageDisplay(
                                imagePath = imgPath,
                                contentDescription = "Thumb ${index + 1}",
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }

        // Details Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Brand & Stock status badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = GoldPrimary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = product.brand,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = if (product.inStock) Color(0xFF10B981) else Color(0xFFEF4444),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (product.inStock) AppStrings.get("in_stock", currentLanguage) else AppStrings.get("out_of_stock", currentLanguage),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text = name,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Price Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "$${product.price}",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GoldPrimary
                )

                if (product.originalPrice > product.price) {
                    Text(
                        text = "$${product.originalPrice}",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Description
            Text(
                text = description,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (specs.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = AppStrings.get("specifications", currentLanguage),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = specs,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Reaction & Review Row (👍, ❤️, 💬 - 3 icons side-by-side in one row)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Button (👍)
                Surface(
                    onClick = onLikeClick,
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f).testTag("like_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👍", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${product.likesCount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                    }
                }

                // Heart Button (❤️)
                Surface(
                    onClick = onHeartClick,
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f).testTag("heart_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("❤️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${product.heartsCount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                    }
                }

                // Write Review Button (💬)
                Surface(
                    onClick = { showWriteReviewDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    color = GoldPrimary,
                    modifier = Modifier.weight(1.3f).testTag("write_review_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💬", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = AppStrings.get("write_review", currentLanguage),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Reviews Display Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.get("reviews", currentLanguage) + " (${reviews.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary
                )
                TextButton(onClick = { showWriteReviewDialog = true }) {
                    Text("+ " + AppStrings.get("write_review", currentLanguage), fontSize = 12.sp, color = GoldPrimary)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (reviews.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = AppStrings.get("no_reviews_yet", currentLanguage),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    reviews.forEach { rev ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "👤 ${rev.userName}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val dateStr = remember(rev.timestamp) {
                                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                        sdf.format(Date(rev.timestamp))
                                    }
                                    Text(
                                        text = dateStr,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = rev.comment,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // Admin Reply Box
                                if (rev.adminReply.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        color = GoldPrimary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = "🛡️ " + AppStrings.get("admin_reply", currentLanguage) + ":",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldPrimary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = rev.adminReply,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Add to Cart Primary Button & Compare Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onAddToCart(product) },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("detail_add_to_cart_btn")
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppStrings.get("add_to_cart", currentLanguage),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                if (onToggleCompare != null) {
                    OutlinedButton(
                        onClick = { onToggleCompare(product) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isCompared) GoldPrimary.copy(alpha = 0.2f) else Color.Transparent
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldPrimary),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("detail_compare_btn")
                    ) {
                        Text("⚖️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCompared) AppStrings.get("compared", currentLanguage) else AppStrings.get("compare", currentLanguage),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Direct Order Options (Telegram, WhatsApp, Call)
            Text(
                text = AppStrings.get("order_now", currentLanguage) + ":",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onOrderTelegram(product) },
                    colors = ButtonDefaults.buttonColors(containerColor = SapphireBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("detail_telegram_order_btn")
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Telegram", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Telegram", fontSize = 11.sp, color = Color.White)
                }

                Button(
                    onClick = { onOrderWhatsApp(product) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("detail_whatsapp_order_btn")
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp", fontSize = 11.sp, color = Color.White)
                }

                IconButton(
                    onClick = onCallStore,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .size(48.dp)
                        .testTag("detail_call_btn")
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Call", tint = GoldPrimary)
                }
            }
        }
    }

    if (showWriteReviewDialog) {
        AlertDialog(
            onDismissRequest = { showWriteReviewDialog = false },
            title = {
                Text(
                    text = "💬 " + AppStrings.get("write_review", currentLanguage),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = reviewerName,
                        onValueChange = { reviewerName = it },
                        label = { Text(AppStrings.get("your_name", currentLanguage), fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("review_name_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { reviewComment = it },
                        label = { Text(AppStrings.get("your_review", currentLanguage), fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().height(100.dp).testTag("review_comment_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reviewComment.isNotBlank()) {
                            onAddReview(reviewerName, reviewComment)
                            reviewComment = ""
                            showWriteReviewDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    modifier = Modifier.testTag("submit_review_dialog_btn")
                ) {
                    Text(AppStrings.get("submit_review", currentLanguage), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWriteReviewDialog = false }) {
                    Text(AppStrings.get("cancel", currentLanguage))
                }
            }
        )
    }
}

@Composable
fun ProductImageDisplay(
    imagePath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    ProductImageView(
        imageSource = imagePath,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}

