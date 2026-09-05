package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.language.AppLanguage
import com.example.ui.language.AppStrings
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SapphireBlue
import com.example.data.local.Coupon
import com.example.ui.components.ProductImageView
import com.example.ui.viewmodel.CartItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    cartItems: List<CartItem>,
    subtotal: Double,
    discount: Double,
    finalTotal: Double,
    appliedCoupon: Coupon?,
    couponMessage: String?,
    availableCoupons: List<Coupon>,
    currentLanguage: AppLanguage,
    onIncreaseQty: (Int) -> Unit,
    onDecreaseQty: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onClearCart: () -> Unit,
    onApplyCoupon: (String) -> Unit,
    onRemoveCoupon: () -> Unit,
    onShareInvoice: (invoiceText: String) -> Unit,
    onSubmitOrder: (customerName: String, customerPhone: String, note: String, channel: String) -> Unit
) {
    val context = LocalContext.current
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var orderNote by remember { mutableStateOf("") }
    var promoCodeInput by remember { mutableStateOf("") }
    var showInvoicePreviewDialog by remember { mutableStateOf(false) }
    var generatedInvoicePreview by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = AppStrings.get("cart_title", currentLanguage),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (cartItems.isNotEmpty()) {
                TextButton(onClick = onClearCart, modifier = Modifier.testTag("clear_cart_btn")) {
                    Text(
                        text = AppStrings.get("clear_cart", currentLanguage),
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.RemoveShoppingCart,
                        contentDescription = "Empty Cart",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = AppStrings.get("cart_empty", currentLanguage),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Cart items list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cartItems) { item ->
                    val pName = when (currentLanguage) {
                        AppLanguage.UYGHUR -> item.product.nameUg
                        AppLanguage.ARABIC -> item.product.nameAr
                        AppLanguage.ENGLISH -> item.product.nameEn
                    }
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().testTag("cart_item_${item.product.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ProductImageView(
                                imageSource = item.product.imageResName,
                                contentDescription = pName,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$${item.product.price.toInt()} x ${item.quantity} = $${(item.product.price * item.quantity).toInt()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                            }

                            // Quantity controls
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onDecreaseQty(item.product.id) },
                                    modifier = Modifier.size(28.dp).testTag("decrease_qty_${item.product.id}")
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp))
                                }

                                Text(
                                    text = item.quantity.toString(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                IconButton(
                                    onClick = { onIncreaseQty(item.product.id) },
                                    modifier = Modifier.size(28.dp).testTag("increase_qty_${item.product.id}")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(14.dp))
                                }

                                IconButton(
                                    onClick = { onRemoveItem(item.product.id) },
                                    modifier = Modifier.size(28.dp).testTag("remove_item_${item.product.id}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                // Coupon & Promo Code Section
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "🏷️ " + AppStrings.get("promo_code", currentLanguage),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )

                            // Quick available coupon chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                availableCoupons.forEach { coup ->
                                    val isApplied = appliedCoupon?.code == coup.code
                                    val desc = when (currentLanguage) {
                                        AppLanguage.UYGHUR -> coup.descUg
                                        AppLanguage.ARABIC -> coup.descAr
                                        AppLanguage.ENGLISH -> coup.descEn
                                    }
                                    Surface(
                                        onClick = {
                                            if (isApplied) onRemoveCoupon() else onApplyCoupon(coup.code)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isApplied) GoldPrimary else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (isApplied) GoldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = coup.code,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (isApplied) Color.Black else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "($desc)",
                                                fontSize = 9.sp,
                                                color = if (isApplied) Color.Black.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (isApplied) {
                                                Icon(Icons.Default.Check, contentDescription = "Active", tint = Color.Black, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // Input bar for custom code
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = promoCodeInput,
                                    onValueChange = { promoCodeInput = it },
                                    placeholder = { Text("Code: NOOR10, YENGILIK...", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )

                                Button(
                                    onClick = {
                                        if (promoCodeInput.isNotBlank()) {
                                            onApplyCoupon(promoCodeInput.trim())
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(46.dp)
                                ) {
                                    Text(AppStrings.get("apply_code", currentLanguage), color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Status message
                            if (!couponMessage.isNullOrBlank()) {
                                Text(
                                    text = couponMessage,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (appliedCoupon != null) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }

                // Summary & Totals Breakdown Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = AppStrings.get("subtotal", currentLanguage), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "$${subtotal.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            if (discount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${AppStrings.get("discount", currentLanguage)} (${appliedCoupon?.code}):",
                                        fontSize = 12.sp,
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(text = "-$${discount.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = AppStrings.get("total_price", currentLanguage),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$${finalTotal.toInt()}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldPrimary
                                )
                            }
                        }
                    }
                }

                // Order Form Inputs
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text(AppStrings.get("customer_name", currentLanguage), fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("order_name_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text(AppStrings.get("customer_phone", currentLanguage), fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("order_phone_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = orderNote,
                            onValueChange = { orderNote = it },
                            label = { Text(AppStrings.get("order_note", currentLanguage), fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("order_note_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Action Buttons: Share Invoice + WhatsApp + Telegram
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        // Share Invoice Button
                        OutlinedButton(
                            onClick = {
                                val invoice = buildCartInvoiceText(
                                    items = cartItems,
                                    subtotal = subtotal,
                                    discount = discount,
                                    total = finalTotal,
                                    customerName = customerName.ifBlank { "خېرىدار" },
                                    customerPhone = customerPhone.ifBlank { "N/A" },
                                    note = orderNote,
                                    appliedCoupon = appliedCoupon,
                                    lang = currentLanguage
                                )
                                generatedInvoicePreview = invoice
                                showInvoicePreviewDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("share_invoice_btn")
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = AppStrings.get("share_invoice", currentLanguage),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )
                        }

                        // Order Channels (WhatsApp / Telegram)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onSubmitOrder(
                                        customerName.ifBlank { "Khéridar" },
                                        customerPhone.ifBlank { "N/A" },
                                        orderNote,
                                        "whatsapp"
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp).testTag("submit_whatsapp_order_btn")
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WhatsApp", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    onSubmitOrder(
                                        customerName.ifBlank { "Khéridar" },
                                        customerPhone.ifBlank { "N/A" },
                                        orderNote,
                                        "telegram"
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SapphireBlue),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp).testTag("submit_telegram_order_btn")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Telegram", tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Telegram", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Invoice Share & Preview Dialog
    if (showInvoicePreviewDialog) {
        AlertDialog(
            onDismissRequest = { showInvoicePreviewDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = GoldPrimary)
                    Text(
                        text = AppStrings.get("invoice_title", currentLanguage),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                }
            },
            text = {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = generatedInvoicePreview,
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Noor Store Invoice", generatedInvoicePreview)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, AppStrings.get("invoice_copied", currentLanguage), Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            onShareInvoice(generatedInvoicePreview)
                            showInvoicePreviewDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStrings.get("share_invoice", currentLanguage), color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showInvoicePreviewDialog = false }) {
                    Text(AppStrings.get("cancel", currentLanguage))
                }
            }
        )
    }
}

private fun buildCartInvoiceText(
    items: List<CartItem>,
    subtotal: Double,
    discount: Double,
    total: Double,
    customerName: String,
    customerPhone: String,
    note: String,
    appliedCoupon: Coupon?,
    lang: AppLanguage
): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    val dateStr = sdf.format(java.util.Date())
    val orderId = "NOOR-" + (10000..99999).random()

    val sb = StringBuilder()
    sb.append("═══════════════════════════════\n")
    sb.append(" 📱 NOOR STORE - ELECTRONIC INVOICE\n")
    sb.append(" «نۇرلۇق» تېلېفونچىلىقى - ئېلېكترونلۇق تالون\n")
    sb.append("═══════════════════════════════\n\n")

    sb.append("📋 ${AppStrings.get("order_id", lang)}: #$orderId\n")
    sb.append("📅 ${AppStrings.get("date", lang)}: $dateStr\n")
    sb.append("👤 ${AppStrings.get("customer_name", lang)}: $customerName\n")
    sb.append("📞 ${AppStrings.get("customer_phone", lang)}: $customerPhone\n\n")

    sb.append("─────────── ITEMS ───────────\n")
    items.forEachIndexed { idx, item ->
        val pName = when (lang) {
            AppLanguage.UYGHUR -> item.product.nameUg
            AppLanguage.ARABIC -> item.product.nameAr
            AppLanguage.ENGLISH -> item.product.nameEn
        }
        sb.append("${idx + 1}. $pName\n")
        sb.append("   • Qty: ${item.quantity}  ×  $${item.product.price.toInt()}  =  $${(item.product.price * item.quantity).toInt()}\n")
    }
    sb.append("─────────────────────────────\n")
    sb.append("💵 ${AppStrings.get("subtotal", lang)}: $${subtotal.toInt()}\n")
    if (discount > 0) {
        sb.append("🏷️ ${AppStrings.get("discount", lang)} (${appliedCoupon?.code}): -$${discount.toInt()}\n")
    }
    sb.append("⭐ ${AppStrings.get("total_price", lang)}: $${total.toInt()}\n\n")

    if (note.isNotBlank()) {
        sb.append("📝 ${AppStrings.get("order_note", lang)}: $note\n\n")
    }

    sb.append("🏢 ${AppStrings.get("store_address", lang)}\n")
    sb.append("🕒 ${AppStrings.get("business_hours", lang)}\n")
    sb.append("☎️ WhatsApp: 0995416715 | Telegram: @sensiz09985\n")
    sb.append("═══════════════════════════════\n")
    return sb.toString()
}


