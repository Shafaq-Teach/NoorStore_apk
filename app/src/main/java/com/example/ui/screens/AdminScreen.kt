package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import android.graphics.BitmapFactory
import android.util.Base64
import org.json.JSONObject
import org.json.JSONArray
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.Coupon
import com.example.data.local.OrderEntity
import com.example.data.local.ProductEntity
import com.example.data.local.ReviewEntity
import com.example.ui.components.ImageStorageHelper
import com.example.ui.components.ProductImageView
import com.example.ui.language.AppLanguage
import com.example.ui.language.AppStrings
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SapphireBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    isLoggedIn: Boolean,
    pinInput: String,
    errorMessage: String?,
    successMessage: String?,
    products: List<ProductEntity>,
    orders: List<OrderEntity>,
    reviews: List<ReviewEntity> = emptyList(),
    coupons: List<Coupon> = emptyList(),
    currentLanguage: AppLanguage,
    onPinChange: (String) -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onChangePin: (oldPin: String, newPin: String, confirmPin: String) -> Boolean,
    onUpdateOrderStatus: (orderId: Int, status: String) -> Unit = { _, _ -> },
    onDeleteOrder: (orderId: Int) -> Unit = { _ -> },
    onNotifyCustomer: (order: OrderEntity, status: String) -> Unit = { _, _ -> },
    onUpdateProductPrice: (productId: Int, newPrice: Double) -> Unit = { _, _ -> },
    onUpdateProduct: (ProductEntity) -> Unit = { _ -> },
    onAddProduct: (
        nameUg: String, nameAr: String, nameEn: String,
        descUg: String, descAr: String, descEn: String,
        price: Double, categoryId: String, brand: String,
        image1: String, image2: String, image3: String,
        isFeatured: Boolean, inStock: Boolean
    ) -> Unit,
    onDeleteProduct: (Int) -> Unit,
    onToggleStock: (ProductEntity) -> Unit,
    onToggleFeatured: (ProductEntity) -> Unit,
    onAddCoupon: (
        code: String,
        discountPercent: Double,
        discountAmount: Double,
        minSpend: Double,
        descUg: String,
        descAr: String,
        descEn: String
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    onDeleteCoupon: (String) -> Unit = { _ -> },
    onReplyReview: (reviewId: Int, reply: String) -> Unit = { _, _ -> },
    onDeleteReview: (reviewId: Int) -> Unit = { _ -> },
    onShareSalesReport: () -> Unit = {},
    syncStateJson: String? = null,
    onSendSyncCommand: (String) -> Unit = {},
    onRefreshSync: () -> Unit = {}
) {
    var showAddProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var quickPriceProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showAddCouponDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showSyncModal by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Analytics, 1: Orders, 2: Products, 3: Coupons, 4: Reviews, 5: Settings

    if (!isLoggedIn) {
        // Login View
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth().testTag("admin_login_card")
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(GoldPrimary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Lock",
                            tint = GoldPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = AppStrings.get("admin_login", currentLanguage),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = AppStrings.get("enter_pin", currentLanguage),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = onPinChange,
                        placeholder = { Text(AppStrings.get("pin_placeholder", currentLanguage)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_pin_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = AppStrings.get(errorMessage, currentLanguage),
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onLogin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_login_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = AppStrings.get("login", currentLanguage),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
        return
    }

    // Logged In Super Admin Dashboard
    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(GoldPrimary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DashboardCustomize, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = AppStrings.get("admin_dashboard_title", currentLanguage),
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = AppStrings.get("admin_control_center", currentLanguage),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showSyncModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("🖥️ سىستېما كۆزنىكى", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        IconButton(
                            onClick = onShareSalesReport,
                            modifier = Modifier
                                .size(36.dp)
                                .background(GoldPrimary.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share Report", tint = GoldPrimary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Dashboard Tabs
                val tabTitles = listOf(
                    "📊 " + AppStrings.get("analytics_tab", currentLanguage),
                    "⚡ ئاپتوماتىك ماس قەدەملەش",
                    "📦 " + AppStrings.get("order_status", currentLanguage) + " (${orders.size})",
                    "📱 " + AppStrings.get("products", currentLanguage) + " (${products.size})",
                    "🎟️ " + AppStrings.get("manage_coupons", currentLanguage) + " (${coupons.size})",
                    "💬 " + AppStrings.get("reviews", currentLanguage) + " (${reviews.size})",
                    "🔐 " + AppStrings.get("change_pin", currentLanguage)
                )

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    contentColor = GoldPrimary,
                    divider = {}
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            when (selectedTab) {
                3 -> {
                    ExtendedFloatingActionButton(
                        onClick = { showAddProductDialog = true },
                        containerColor = GoldPrimary,
                        contentColor = Color.Black,
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text(AppStrings.get("add_product", currentLanguage), fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("admin_add_product_fab")
                    )
                }
                4 -> {
                    ExtendedFloatingActionButton(
                        onClick = { showAddCouponDialog = true },
                        containerColor = GoldPrimary,
                        contentColor = Color.Black,
                        icon = { Icon(Icons.Default.AddCard, contentDescription = null) },
                        text = { Text(AppStrings.get("add_coupon", currentLanguage), fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> AnalyticsOverviewTab(
                    products = products,
                    orders = orders,
                    currentLanguage = currentLanguage,
                    onToggleStock = onToggleStock,
                    onShareReport = onShareSalesReport
                )
                1 -> AutoSyncTab(
                    currentLanguage = currentLanguage,
                    onOpenSyncModal = { showSyncModal = true }
                )
                2 -> OrdersManagementTab(
                    orders = orders,
                    currentLanguage = currentLanguage,
                    onUpdateStatus = onUpdateOrderStatus,
                    onDeleteOrder = onDeleteOrder,
                    onNotifyCustomer = onNotifyCustomer
                )
                3 -> ProductsManagementTab(
                    products = products,
                    currentLanguage = currentLanguage,
                    onToggleStock = onToggleStock,
                    onToggleFeatured = onToggleFeatured,
                    onQuickPrice = { quickPriceProduct = it },
                    onEditProduct = { editingProduct = it },
                    onDeleteProduct = onDeleteProduct
                )
                4 -> CouponsManagementTab(
                    coupons = coupons,
                    currentLanguage = currentLanguage,
                    onDeleteCoupon = onDeleteCoupon
                )
                5 -> ReviewsModerationTab(
                    reviews = reviews,
                    products = products,
                    currentLanguage = currentLanguage,
                    onReplyReview = onReplyReview,
                    onDeleteReview = onDeleteReview
                )
                6 -> SettingsTab(
                    currentLanguage = currentLanguage,
                    onOpenChangePin = { showChangePinDialog = true },
                    onLogout = onLogout
                )
            }
        }
    }

    // Auto Sync Standalone Window Dialog
    if (showSyncModal) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showSyncModal = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            SyncSystemWindowModal(
                syncStateJson = syncStateJson,
                onSendSyncCommand = onSendSyncCommand,
                onRefreshSync = onRefreshSync,
                onDismiss = { showSyncModal = false }
            )
        }
    }

    // Quick Price Dialog
    quickPriceProduct?.let { prod ->
        var newPriceStr by remember { mutableStateOf(prod.price.toInt().toString()) }
        AlertDialog(
            onDismissRequest = { quickPriceProduct = null },
            title = {
                Text(
                    text = AppStrings.get("quick_price_edit", currentLanguage),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val name = when (currentLanguage) {
                        AppLanguage.UYGHUR -> prod.nameUg
                        AppLanguage.ARABIC -> prod.nameAr
                        AppLanguage.ENGLISH -> prod.nameEn
                    }
                    Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    OutlinedTextField(
                        value = newPriceStr,
                        onValueChange = { newPriceStr = it },
                        label = { Text(AppStrings.get("price", currentLanguage) + " ($)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = newPriceStr.toDoubleOrNull()
                        if (p != null && p >= 0) {
                            onUpdateProductPrice(prod.id, p)
                        }
                        quickPriceProduct = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) {
                    Text(AppStrings.get("save", currentLanguage), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { quickPriceProduct = null }) {
                    Text(AppStrings.get("cancel", currentLanguage))
                }
            }
        )
    }

    // Add / Edit Product Dialog
    if (showAddProductDialog || editingProduct != null) {
        val prodToEdit = editingProduct
        ProductFormDialog(
            isEditing = prodToEdit != null,
            initialProduct = prodToEdit,
            currentLanguage = currentLanguage,
            onDismiss = {
                showAddProductDialog = false
                editingProduct = null
            },
            onSave = { nUg, nAr, nEn, dUg, dAr, dEn, pr, cat, br, img1, img2, img3, feat, stock ->
                if (prodToEdit != null) {
                    onUpdateProduct(
                        prodToEdit.copy(
                            nameUg = nUg,
                            nameAr = nAr,
                            nameEn = nEn,
                            descriptionUg = dUg,
                            descriptionAr = dAr,
                            descriptionEn = dEn,
                            price = pr,
                            categoryId = cat,
                            brand = br,
                            imageResName = img1,
                            imageResName2 = img2,
                            imageResName3 = img3,
                            isFeatured = feat,
                            inStock = stock
                        )
                    )
                } else {
                    onAddProduct(nUg, nAr, nEn, dUg, dAr, dEn, pr, cat, br, img1, img2, img3, feat, stock)
                }
                showAddProductDialog = false
                editingProduct = null
            }
        )
    }

    // Add Coupon Dialog
    if (showAddCouponDialog) {
        AddCouponDialog(
            currentLanguage = currentLanguage,
            onDismiss = { showAddCouponDialog = false },
            onSave = { code, isPercent, amount, minSpend, descUg, descAr, descEn ->
                val discountPercent = if (isPercent) amount else 0.0
                val discountAmount = if (!isPercent) amount else 0.0
                onAddCoupon(code, discountPercent, discountAmount, minSpend, descUg, descAr, descEn)
                showAddCouponDialog = false
            }
        )
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        ChangePinDialog(
            errorMessage = errorMessage,
            successMessage = successMessage,
            currentLanguage = currentLanguage,
            onDismiss = { showChangePinDialog = false },
            onChangePin = onChangePin
        )
    }
}

// -------------------------------------------------------------
// TAB 0: Analytics & KPI Overview
// -------------------------------------------------------------
@Composable
fun AnalyticsOverviewTab(
    products: List<ProductEntity>,
    orders: List<OrderEntity>,
    currentLanguage: AppLanguage,
    onToggleStock: (ProductEntity) -> Unit,
    onShareReport: () -> Unit
) {
    val totalRevenue = orders.sumOf { it.totalAmount }
    val totalInventory = products.sumOf { it.price }
    val pendingOrders = orders.filter { it.status.equals("Pending", ignoreCase = true) }
    val completedOrders = orders.filter { it.status.equals("Completed", ignoreCase = true) }
    val outOfStockProducts = products.filter { !it.inStock }
    val topTrending = products.sortedByDescending { it.likesCount + it.heartsCount }.take(5)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // KPI Metric Cards Grid
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiStatCard(
                    title = AppStrings.get("total_sales_revenue", currentLanguage),
                    value = "$${totalRevenue.toInt()}",
                    subtitle = "${orders.size} " + AppStrings.get("total_orders_count", currentLanguage),
                    icon = Icons.Default.MonetizationOn,
                    iconColor = GoldPrimary,
                    modifier = Modifier.weight(1f)
                )
                KpiStatCard(
                    title = AppStrings.get("total_inventory_value", currentLanguage),
                    value = "$${totalInventory.toInt()}",
                    subtitle = "${products.size} " + AppStrings.get("products", currentLanguage),
                    icon = Icons.Default.Inventory2,
                    iconColor = SapphireBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiStatCard(
                    title = AppStrings.get("pending_orders", currentLanguage),
                    value = "${pendingOrders.size}",
                    subtitle = AppStrings.get("status_pending", currentLanguage),
                    icon = Icons.Default.HourglassTop,
                    iconColor = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                KpiStatCard(
                    title = AppStrings.get("completed_orders", currentLanguage),
                    value = "${completedOrders.size}",
                    subtitle = AppStrings.get("status_completed", currentLanguage),
                    icon = Icons.Default.CheckCircle,
                    iconColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Export / Share Report Button Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AppStrings.get("share_report", currentLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = AppStrings.get("share_report_desc", currentLanguage),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onShareReport,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(AppStrings.get("share", currentLanguage), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Low Stock / Out of Stock Alert section
        if (outOfStockProducts.isNotEmpty()) {
            item {
                Text(
                    text = "⚠️ " + AppStrings.get("low_stock_warning", currentLanguage) + " (${outOfStockProducts.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFFEF4444)
                )
            }
            items(outOfStockProducts) { prod ->
                val name = when (currentLanguage) {
                    AppLanguage.UYGHUR -> prod.nameUg
                    AppLanguage.ARABIC -> prod.nameAr
                    AppLanguage.ENGLISH -> prod.nameEn
                }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.08f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFF59E0B))))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("$${prod.price.toInt()} • ${prod.brand}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { onToggleStock(prod) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text(AppStrings.get("in_stock", currentLanguage), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Top Favorited / Trending Devices
        if (topTrending.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "🔥 " + AppStrings.get("top_trending", currentLanguage),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = GoldPrimary
                )
            }
            items(topTrending) { prod ->
                val name = when (currentLanguage) {
                    AppLanguage.UYGHUR -> prod.nameUg
                    AppLanguage.ARABIC -> prod.nameAr
                    AppLanguage.ENGLISH -> prod.nameEn
                }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                            Text("$${prod.price.toInt()} • ${prod.brand}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BadgeChip(text = "👍 ${prod.likesCount}", color = SapphireBlue)
                            BadgeChip(text = "❤️ ${prod.heartsCount}", color = Color(0xFFE11D48))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KpiStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(iconColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
fun BadgeChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// -------------------------------------------------------------
// TAB 1: Order Lifecycle Manager
// -------------------------------------------------------------
@Composable
fun OrdersManagementTab(
    orders: List<OrderEntity>,
    currentLanguage: AppLanguage,
    onUpdateStatus: (orderId: Int, status: String) -> Unit,
    onDeleteOrder: (orderId: Int) -> Unit,
    onNotifyCustomer: (order: OrderEntity, status: String) -> Unit
) {
    var selectedStatusFilter by remember { mutableStateOf("All") }
    val filteredOrders = if (selectedStatusFilter == "All") {
        orders
    } else {
        orders.filter { it.status.equals(selectedStatusFilter, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Status Filter Chips Row
        val statuses = listOf(
            "All" to AppStrings.get("status_all", currentLanguage),
            "Pending" to AppStrings.get("status_pending", currentLanguage),
            "Processing" to AppStrings.get("status_processing", currentLanguage),
            "Shipped" to AppStrings.get("status_shipped", currentLanguage),
            "Completed" to AppStrings.get("status_completed", currentLanguage),
            "Cancelled" to AppStrings.get("status_cancelled", currentLanguage)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            statuses.forEach { (key, label) ->
                val isSel = selectedStatusFilter == key
                FilterChip(
                    selected = isSel,
                    onClick = { selectedStatusFilter = key },
                    label = { Text(label, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldPrimary,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(AppStrings.get("no_orders_yet", currentLanguage), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredOrders, key = { it.id }) { order ->
                    OrderLifecycleCard(
                        order = order,
                        currentLanguage = currentLanguage,
                        onUpdateStatus = { onUpdateStatus(order.id, it) },
                        onDeleteOrder = { onDeleteOrder(order.id) },
                        onNotifyCustomer = { onNotifyCustomer(order, order.status) }
                    )
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
fun OrderLifecycleCard(
    order: OrderEntity,
    currentLanguage: AppLanguage,
    onUpdateStatus: (String) -> Unit,
    onDeleteOrder: () -> Unit,
    onNotifyCustomer: () -> Unit
) {
    var showStatusMenu by remember { mutableStateOf(false) }

    val statusColor = when (order.status) {
        "Processing" -> Color(0xFF3B82F6)
        "Shipped" -> Color(0xFF8B5CF6)
        "Completed" -> Color(0xFF10B981)
        "Cancelled" -> Color(0xFFEF4444)
        else -> Color(0xFFF59E0B) // Pending
    }

    val statusLabel = when (order.status) {
        "Processing" -> AppStrings.get("status_processing", currentLanguage)
        "Shipped" -> AppStrings.get("status_shipped", currentLanguage)
        "Completed" -> AppStrings.get("status_completed", currentLanguage)
        "Cancelled" -> AppStrings.get("status_cancelled", currentLanguage)
        else -> AppStrings.get("status_pending", currentLanguage)
    }

    val dateFormatted = remember(order.orderDate) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        sdf.format(Date(order.orderDate))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Order ID, Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${order.id}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = GoldPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(AppStrings.get("order", currentLanguage), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Interactive Status Picker
                Box {
                    AssistChip(
                        onClick = { showStatusMenu = true },
                        label = { Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor) },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = statusColor, modifier = Modifier.size(12.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(containerColor = statusColor.copy(alpha = 0.12f))
                    )

                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        listOf("Pending", "Processing", "Shipped", "Completed", "Cancelled").forEach { st ->
                            val stName = when (st) {
                                "Processing" -> AppStrings.get("status_processing", currentLanguage)
                                "Shipped" -> AppStrings.get("status_shipped", currentLanguage)
                                "Completed" -> AppStrings.get("status_completed", currentLanguage)
                                "Cancelled" -> AppStrings.get("status_cancelled", currentLanguage)
                                else -> AppStrings.get("status_pending", currentLanguage)
                            }
                            DropdownMenuItem(
                                text = { Text(stName, fontSize = 12.sp) },
                                onClick = {
                                    onUpdateStatus(st)
                                    showStatusMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Customer Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(order.customerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("📞 ${order.customerPhone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("$${order.totalAmount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GoldPrimary)
                    Text(dateFormatted, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Order Summary Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = order.orderSummary,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (order.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "📝 " + order.note,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions Row: 1-Click WhatsApp notification & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onNotifyCustomer,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981))
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(AppStrings.get("notify_customer", currentLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onDeleteOrder,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444).copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: Products & Quick Inventory Controls
// -------------------------------------------------------------
@Composable
fun ProductsManagementTab(
    products: List<ProductEntity>,
    currentLanguage: AppLanguage,
    onToggleStock: (ProductEntity) -> Unit,
    onToggleFeatured: (ProductEntity) -> Unit,
    onQuickPrice: (ProductEntity) -> Unit,
    onEditProduct: (ProductEntity) -> Unit,
    onDeleteProduct: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf("all") }

    val filtered = products.filter { prod ->
        val matchesQuery = searchQuery.isBlank() ||
                prod.nameUg.contains(searchQuery, ignoreCase = true) ||
                prod.nameAr.contains(searchQuery, ignoreCase = true) ||
                prod.nameEn.contains(searchQuery, ignoreCase = true) ||
                prod.brand.contains(searchQuery, ignoreCase = true)
        val matchesCat = selectedCat == "all" || prod.categoryId == selectedCat
        matchesQuery && matchesCat
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Filter header
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(AppStrings.get("search_hint", currentLanguage), fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            val catFilters = listOf(
                "all" to AppStrings.get("all", currentLanguage),
                "phones" to AppStrings.get("phones", currentLanguage),
                "tablets" to AppStrings.get("tablets", currentLanguage),
                "accessories" to AppStrings.get("accessories", currentLanguage),
                "watches" to AppStrings.get("watches", currentLanguage)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                catFilters.forEach { (catId, catName) ->
                    val isSel = selectedCat == catId
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedCat = catId },
                        label = { Text(catName, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered, key = { it.id }) { product ->
                AdminProductCard(
                    product = product,
                    currentLanguage = currentLanguage,
                    onToggleStock = { onToggleStock(product) },
                    onToggleFeatured = { onToggleFeatured(product) },
                    onQuickPrice = { onQuickPrice(product) },
                    onEdit = { onEditProduct(product) },
                    onDelete = { onDeleteProduct(product.id) }
                )
            }
            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
fun AdminProductCard(
    product: ProductEntity,
    currentLanguage: AppLanguage,
    onToggleStock: () -> Unit,
    onToggleFeatured: () -> Unit,
    onQuickPrice: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val name = when (currentLanguage) {
        AppLanguage.UYGHUR -> product.nameUg
        AppLanguage.ARABIC -> product.nameAr
        AppLanguage.ENGLISH -> product.nameEn
    }

    val resId = remember(product.imageResName) {
        val cleanName = product.imageResName.substringBeforeLast(".")
        val id = context.resources.getIdentifier(cleanName, "drawable", context.packageName)
        if (id != 0) id else if (product.categoryId == "tablets") {
            context.resources.getIdentifier("img_tablets_1786037603482", "drawable", context.packageName)
        } else {
            context.resources.getIdentifier("img_phones_1786037591338", "drawable", context.packageName)
        }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product Thumbnail
                ProductImageView(
                    imageSource = product.imageResName,
                    contentDescription = name,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Info & Price
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${product.brand} • ${product.categoryId}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onQuickPrice() }
                    ) {
                        Text(
                            text = "$${product.price.toInt()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GoldPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Edit, contentDescription = "Edit Price", tint = GoldPrimary, modifier = Modifier.size(12.dp))
                    }
                }

                // Edit & Delete Action Buttons
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.EditNote, contentDescription = "Edit", tint = SapphireBlue)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            // Fast Stock & Featured Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stock Switch
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = product.inStock,
                        onCheckedChange = { onToggleStock() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF10B981)
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (product.inStock) AppStrings.get("in_stock", currentLanguage) else AppStrings.get("out_of_stock", currentLanguage),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (product.inStock) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }

                // Featured Toggle Star
                IconButton(onClick = onToggleFeatured, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (product.isFeatured) Icons.Default.Star else Icons.Default.StarOutline,
                        contentDescription = "Featured",
                        tint = if (product.isFeatured) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: Coupons & Promo Codes Engine
// -------------------------------------------------------------
@Composable
fun CouponsManagementTab(
    coupons: List<Coupon>,
    currentLanguage: AppLanguage,
    onDeleteCoupon: (String) -> Unit
) {
    if (coupons.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Discount, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(AppStrings.get("no_coupons_yet", currentLanguage), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(coupons, key = { it.code }) { coupon ->
                val desc = when (currentLanguage) {
                    AppLanguage.UYGHUR -> coupon.descUg
                    AppLanguage.ARABIC -> coupon.descAr
                    AppLanguage.ENGLISH -> coupon.descEn
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(GoldPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .border(1.dp, GoldPrimary, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = coupon.code,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = GoldPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                val discountText = if (coupon.discountPercent > 0) {
                                    "${coupon.discountPercent.toInt()}% " + AppStrings.get("discount_off", currentLanguage)
                                } else {
                                    "$${coupon.discountAmount.toInt()} " + AppStrings.get("discount_off", currentLanguage)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(discountText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            if (coupon.minSpend > 0) {
                                Text(
                                    text = "${AppStrings.get("min_spend_prefix", currentLanguage)}: $${coupon.minSpend.toInt()}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { onDeleteCoupon(coupon.code) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Coupon", tint = Color(0xFFEF4444))
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

// -------------------------------------------------------------
// TAB 4: Reviews & Moderation
// -------------------------------------------------------------
@Composable
fun ReviewsModerationTab(
    reviews: List<ReviewEntity>,
    products: List<ProductEntity>,
    currentLanguage: AppLanguage,
    onReplyReview: (reviewId: Int, reply: String) -> Unit,
    onDeleteReview: (reviewId: Int) -> Unit
) {
    if (reviews.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.RateReview, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(AppStrings.get("no_reviews_yet", currentLanguage), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(reviews, key = { it.id }) { review ->
                val prod = products.find { it.id == review.productId }
                val prodName = when (currentLanguage) {
                    AppLanguage.UYGHUR -> prod?.nameUg ?: "Device #${review.productId}"
                    AppLanguage.ARABIC -> prod?.nameAr ?: "Device #${review.productId}"
                    AppLanguage.ENGLISH -> prod?.nameEn ?: "Device #${review.productId}"
                }

                val dateStr = remember(review.timestamp) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    sdf.format(Date(review.timestamp))
                }

                var isReplying by remember { mutableStateOf(false) }
                var replyText by remember { mutableStateOf(review.adminReply) }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(prodName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GoldPrimary)
                            IconButton(onClick = { onDeleteReview(review.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444).copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(5) { idx ->
                                Icon(
                                    imageVector = if (idx < review.rating) Icons.Default.Star else Icons.Default.StarOutline,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(review.userName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("• $dateStr", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(review.comment, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)

                        if (review.adminReply.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(GoldPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "👑 " + AppStrings.get("admin_reply", currentLanguage),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary
                                    )
                                    Text(review.adminReply, fontSize = 12.sp)
                                }
                            }
                        }

                        // Reply Button / Inline Editor
                        if (isReplying) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = replyText,
                                onValueChange = { replyText = it },
                                placeholder = { Text(AppStrings.get("type_reply", currentLanguage), fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        onReplyReview(review.id, replyText)
                                        isReplying = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                                ) {
                                    Text(AppStrings.get("save", currentLanguage), color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                TextButton(onClick = { isReplying = false }) {
                                    Text(AppStrings.get("cancel", currentLanguage), fontSize = 11.sp)
                                }
                            }
                        } else {
                            TextButton(
                                onClick = { isReplying = true },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(AppStrings.get("reply", currentLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 5: Security & Settings
// -------------------------------------------------------------
@Composable
fun SettingsTab(
    currentLanguage: AppLanguage,
    onOpenChangePin: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "🔐 " + AppStrings.get("change_pin", currentLanguage),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = GoldPrimary
                )
                Text(
                    text = AppStrings.get("change_pin_desc", currentLanguage),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onOpenChangePin,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(AppStrings.get("change_pin", currentLanguage), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ℹ️ " + AppStrings.get("system_info", currentLanguage), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(AppStrings.get("app_version", currentLanguage) + ": Noor Mobile Super Admin v2.5", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("⚡ Supabase Cloud: yufuhjdmzgehwnypdpba (Active Sync)", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
                Text(AppStrings.get("store_address", currentLanguage), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(AppStrings.get("contact_info", currentLanguage) + ": 0995416715 | @sensiz09985", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Button(
            onClick = onLogout,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(AppStrings.get("logout", currentLanguage), color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// -------------------------------------------------------------
// DIALOGS: Product Form, Add Coupon, Change PIN
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormDialog(
    isEditing: Boolean,
    initialProduct: ProductEntity?,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (
        nameUg: String, nameAr: String, nameEn: String,
        descUg: String, descAr: String, descEn: String,
        price: Double, categoryId: String, brand: String,
        image1: String, image2: String, image3: String,
        isFeatured: Boolean, inStock: Boolean
    ) -> Unit
) {
    var nameUg by remember { mutableStateOf(initialProduct?.nameUg ?: "") }
    var nameAr by remember { mutableStateOf(initialProduct?.nameAr ?: "") }
    var nameEn by remember { mutableStateOf(initialProduct?.nameEn ?: "") }
    var descUg by remember { mutableStateOf(initialProduct?.descriptionUg ?: "") }
    var descAr by remember { mutableStateOf(initialProduct?.descriptionAr ?: "") }
    var descEn by remember { mutableStateOf(initialProduct?.descriptionEn ?: "") }
    var priceStr by remember { mutableStateOf(initialProduct?.price?.toInt()?.toString() ?: "") }
    var categoryId by remember { mutableStateOf(initialProduct?.categoryId ?: "phones") }
    var brand by remember { mutableStateOf(initialProduct?.brand ?: "") }
    var image1 by remember { mutableStateOf(initialProduct?.imageResName ?: "") }
    var image2 by remember { mutableStateOf(initialProduct?.imageResName2 ?: "") }
    var image3 by remember { mutableStateOf(initialProduct?.imageResName3 ?: "") }
    var isFeatured by remember { mutableStateOf(initialProduct?.isFeatured ?: false) }
    var inStock by remember { mutableStateOf(initialProduct?.inStock ?: true) }

    val context = LocalContext.current
    var pickingImageIndex by remember { mutableStateOf(1) }
    var showManualUrlInputs by remember { mutableStateOf(false) }

    // Multi-Image Gallery Launcher (Picks up to 3 images at once)
    val multiGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val savedPaths = uris.take(3).mapNotNull { uri ->
                ImageStorageHelper.saveUriToAppStorage(context, uri)
            }
            if (savedPaths.isNotEmpty()) {
                if (savedPaths.size >= 1) image1 = savedPaths[0]
                if (savedPaths.size >= 2) image2 = savedPaths[1]
                if (savedPaths.size >= 3) image3 = savedPaths[2]
            }
        }
    }

    // Single Image Gallery Launcher (For replacing an individual image)
    val singleGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val savedPath = ImageStorageHelper.saveUriToAppStorage(context, it)
            if (savedPath != null) {
                when (pickingImageIndex) {
                    1 -> image1 = savedPath
                    2 -> image2 = savedPath
                    3 -> image3 = savedPath
                }
            }
        }
    }

    fun autoFillMissingLanguages() {
        val primaryName = nameUg.trim().ifBlank { nameAr.trim().ifBlank { nameEn.trim() } }
        if (nameUg.isBlank()) nameUg = primaryName
        if (nameAr.isBlank()) nameAr = primaryName
        if (nameEn.isBlank()) nameEn = primaryName

        val primaryDesc = descUg.trim().ifBlank { descAr.trim().ifBlank { descEn.trim() } }
        if (descUg.isBlank()) descUg = primaryDesc
        if (descAr.isBlank()) descAr = primaryDesc
        if (descEn.isBlank()) descEn = primaryDesc
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) AppStrings.get("edit_product_title", currentLanguage) else AppStrings.get("add_product", currentLanguage),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = GoldPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Auto-fill banner
                OutlinedButton(
                    onClick = { autoFillMissingLanguages() },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.get("autofill_languages", currentLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Names in 3 languages
                OutlinedTextField(
                    value = nameUg,
                    onValueChange = {
                        nameUg = it
                        if (nameAr.isBlank()) nameAr = it
                        if (nameEn.isBlank()) nameEn = it
                    },
                    label = { Text(AppStrings.get("product_name_ug", currentLanguage), fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = nameAr,
                    onValueChange = {
                        nameAr = it
                        if (nameUg.isBlank()) nameUg = it
                        if (nameEn.isBlank()) nameEn = it
                    },
                    label = { Text(AppStrings.get("product_name_ar", currentLanguage), fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = nameEn,
                    onValueChange = {
                        nameEn = it
                        if (nameUg.isBlank()) nameUg = it
                        if (nameAr.isBlank()) nameAr = it
                    },
                    label = { Text(AppStrings.get("product_name_en", currentLanguage), fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Descriptions
                OutlinedTextField(
                    value = descUg,
                    onValueChange = {
                        descUg = it
                        if (descAr.isBlank()) descAr = it
                        if (descEn.isBlank()) descEn = it
                    },
                    label = { Text(AppStrings.get("description_ug", currentLanguage), fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = descAr,
                    onValueChange = {
                        descAr = it
                        if (descUg.isBlank()) descUg = it
                        if (descEn.isBlank()) descEn = it
                    },
                    label = { Text(AppStrings.get("description_ar", currentLanguage), fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = descEn,
                    onValueChange = {
                        descEn = it
                        if (descUg.isBlank()) descUg = it
                        if (descAr.isBlank()) descAr = it
                    },
                    label = { Text(AppStrings.get("description_en", currentLanguage), fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Price & Brand
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text(AppStrings.get("price", currentLanguage) + " ($)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text(AppStrings.get("brand", currentLanguage), fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Category Selection
                Text(AppStrings.get("category_select", currentLanguage), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                val categories = listOf(
                    "phones" to AppStrings.get("phones", currentLanguage),
                    "tablets" to AppStrings.get("tablets", currentLanguage),
                    "accessories" to AppStrings.get("accessories", currentLanguage),
                    "watches" to AppStrings.get("watches", currentLanguage)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { (catId, catName) ->
                        val isSel = categoryId == catId
                        FilterChip(
                            selected = isSel,
                            onClick = { categoryId = catId },
                            label = { Text(catName, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                // 3 Images Multi-Upload Header & Visual Gallery
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.get("product_images", currentLanguage),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                    TextButton(onClick = { showManualUrlInputs = !showManualUrlInputs }) {
                        Text(
                            text = if (showManualUrlInputs) "رەسىم كۆرسىتىش" else "تېكىست كىرگۈزۈش",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Prominent Multi-Image Upload Button (Select up to 3 at once)
                Button(
                    onClick = { multiGalleryLauncher.launch("image/*") },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppStrings.get("pick_all_3_images", currentLanguage),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = AppStrings.get("pick_3_images_hint", currentLanguage),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // 3-Slot Visual Image Preview Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Slot 1 (Main)
                    ImageSlotCard(
                        slotIndex = 1,
                        label = AppStrings.get("image_main", currentLanguage),
                        imagePath = image1,
                        modifier = Modifier.weight(1f),
                        onPick = {
                            pickingImageIndex = 1
                            singleGalleryLauncher.launch("image/*")
                        },
                        onClear = { image1 = "" }
                    )

                    // Slot 2
                    ImageSlotCard(
                        slotIndex = 2,
                        label = AppStrings.get("image_sec", currentLanguage),
                        imagePath = image2,
                        modifier = Modifier.weight(1f),
                        onPick = {
                            pickingImageIndex = 2
                            singleGalleryLauncher.launch("image/*")
                        },
                        onClear = { image2 = "" }
                    )

                    // Slot 3
                    ImageSlotCard(
                        slotIndex = 3,
                        label = AppStrings.get("image_thi", currentLanguage),
                        imagePath = image3,
                        modifier = Modifier.weight(1f),
                        onPick = {
                            pickingImageIndex = 3
                            singleGalleryLauncher.launch("image/*")
                        },
                        onClear = { image3 = "" }
                    )
                }

                // Optional Manual Text Inputs for Direct Image Path/URL
                if (showManualUrlInputs) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = image1,
                            onValueChange = { image1 = it },
                            label = { Text(AppStrings.get("image_1", currentLanguage) + " (Path/URL)", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = image2,
                            onValueChange = { image2 = it },
                            label = { Text(AppStrings.get("image_2", currentLanguage) + " (Path/URL)", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = image3,
                            onValueChange = { image3 = it },
                            label = { Text(AppStrings.get("image_3", currentLanguage) + " (Path/URL)", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Checkboxes
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isFeatured, onCheckedChange = { isFeatured = it })
                    Text(AppStrings.get("featured_product_check", currentLanguage), fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = inStock, onCheckedChange = { inStock = it })
                    Text(AppStrings.get("in_stock", currentLanguage), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    autoFillMissingLanguages()
                    val p = priceStr.toDoubleOrNull() ?: 0.0
                    onSave(
                        nameUg, nameAr, nameEn,
                        descUg, descAr, descEn,
                        p, categoryId, brand,
                        image1, image2, image3,
                        isFeatured, inStock
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
            ) {
                Text(AppStrings.get("save", currentLanguage), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", currentLanguage))
            }
        }
    )
}

@Composable
fun ImageSlotCard(
    slotIndex: Int,
    label: String,
    imagePath: String,
    modifier: Modifier = Modifier,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Image Preview Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.05f))
                    .clickable { onPick() },
                contentAlignment = Alignment.Center
            ) {
                if (imagePath.isNotBlank()) {
                    ProductImageView(
                        imageSource = imagePath,
                        contentDescription = label,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "+$slotIndex",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Pick/Replace",
                        tint = GoldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                if (imagePath.isNotBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddCouponDialog(
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (code: String, isPercent: Boolean, amount: Double, minSpend: Double, descUg: String, descAr: String, descEn: String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var isPercent by remember { mutableStateOf(false) }
    var amountStr by remember { mutableStateOf("") }
    var minSpendStr by remember { mutableStateOf("0") }
    var descUg by remember { mutableStateOf("") }
    var descAr by remember { mutableStateOf("") }
    var descEn by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = AppStrings.get("add_coupon", currentLanguage),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = GoldPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text(AppStrings.get("coupon_code", currentLanguage), fontSize = 11.sp) },
                    placeholder = { Text(AppStrings.get("coupon_placeholder", currentLanguage)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Discount Type: Percent vs Fixed
                Text(AppStrings.get("discount_type", currentLanguage), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isPercent,
                        onClick = { isPercent = false },
                        label = { Text(AppStrings.get("fixed_discount", currentLanguage), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldPrimary, selectedLabelColor = Color.Black)
                    )
                    FilterChip(
                        selected = isPercent,
                        onClick = { isPercent = true },
                        label = { Text(AppStrings.get("percent_discount", currentLanguage), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldPrimary, selectedLabelColor = Color.Black)
                    )
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(AppStrings.get("discount_value", currentLanguage) + if (isPercent) " (%)" else " ($)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = minSpendStr,
                    onValueChange = { minSpendStr = it },
                    label = { Text(AppStrings.get("min_spend_label", currentLanguage), fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = descUg,
                    onValueChange = {
                        descUg = it
                        if (descAr.isBlank()) descAr = it
                        if (descEn.isBlank()) descEn = it
                    },
                    label = { Text("چۈشەندۈرۈش (ئۇيغۇرچە)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    val minSpend = minSpendStr.toDoubleOrNull() ?: 0.0
                    if (code.isNotBlank() && amount > 0) {
                        onSave(code, isPercent, amount, minSpend, descUg, descAr, descEn)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
            ) {
                Text(AppStrings.get("save", currentLanguage), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", currentLanguage))
            }
        }
    )
}

@Composable
fun ChangePinDialog(
    errorMessage: String?,
    successMessage: String?,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onChangePin: (oldPin: String, newPin: String, confirmPin: String) -> Boolean
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var dialogError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = AppStrings.get("change_pin", currentLanguage),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = GoldPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { oldPin = it },
                    label = { Text(AppStrings.get("current_pin", currentLanguage), fontSize = 11.sp) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("change_pin_old_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it },
                    label = { Text(AppStrings.get("new_pin", currentLanguage), fontSize = 11.sp) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("change_pin_new_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it },
                    label = { Text(AppStrings.get("confirm_pin", currentLanguage), fontSize = 11.sp) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("change_pin_confirm_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                if (dialogError != null || errorMessage != null) {
                    val errKey = dialogError ?: errorMessage
                    Text(
                        text = AppStrings.get(errKey!!, currentLanguage),
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (successMessage != null) {
                    Text(
                        text = AppStrings.get(successMessage, currentLanguage),
                        color = Color(0xFF10B981),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val success = onChangePin(oldPin, newPin, confirmPin)
                    if (success) {
                        onDismiss()
                    } else {
                        dialogError = errorMessage
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                modifier = Modifier.testTag("save_pin_btn")
            ) {
                Text(AppStrings.get("save", currentLanguage), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", currentLanguage))
            }
        }
    )
}

@Composable
fun AutoSyncTab(
    currentLanguage: AppLanguage,
    onOpenSyncModal: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Main Auto Sync Overview Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Brush.linearGradient(listOf(GoldPrimary, Color(0xFF10B981))), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text("⚡ كۆپ سۇپىلىق ئاپتوماتىك ماس قەدەملەش", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                            Text("Telegram ➡️ Supabase ➡️ WhatsApp", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                    ) {
                        Text("100% ئاكتىپ", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                }

                // Button to open Dark System Window Modal
                Button(
                    onClick = onOpenSyncModal,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                ) {
                    Text("🖥️ سىستېما كۆزنىكىنى ئېچىش", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Channels & Groups status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Telegram
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Image(
                                        painter = painterResource(id = com.example.R.drawable.ic_telegram_3d),
                                        contentDescription = "Telegram",
                                        modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp))
                                    )
                                    Text("Telegram", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                }
                                Text("✅", fontSize = 11.sp)
                            }
                            Text("بوت: @NoorStore520_Bot", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("باشقۇرغۇچى: 7251543464", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // WhatsApp
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Image(
                                        painter = painterResource(id = com.example.R.drawable.ic_whatsapp_3d),
                                        contentDescription = "WhatsApp",
                                        modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp))
                                    )
                                    Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                                Text("✅", fontSize = 11.sp)
                            }
                            Text("خېرىدارلار گۇرۇپپىسى", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("ئاپتوماتىك تارقىتىش ئوچۇق", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Web management link
                Button(
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://shafaq-teach.github.io/Noor_Store/"))
                            context.startActivity(intent)
                        } catch (e: Exception) {}
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("🌐 تور دۇكىنىنى كۆرۈش ۋە مەھسۇلات باشقۇرۇش", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Instructions Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.08f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("💡 تېلېگرامدىن قانداق يوللايسىز؟", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                Text(
                    "تېلېگرامدىكى مەھسۇلات قانىلى ياكى گۇرۇپپىڭىزغا رەسىم بىلەن بىللە باھاسىنى تاشلاپلا قويسىڭىز، سىستېما بىرلا ۋاقىتتا سۇپابەس، تور بېكەت ۋە ۋاتساپقا تەڭ تارقىتىپ بېرىدۇ!",
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun SyncSystemWindowModal(
    syncStateJson: String?,
    onSendSyncCommand: (String) -> Unit,
    onRefreshSync: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var whatsappStatus by remember { mutableStateOf("CONNECTED") }
    var latestQrBase64 by remember { mutableStateOf<String?>(null) }
    var availableGroups by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var selectedGroupName by remember { mutableStateOf("Noor Store خېرىدارلار گۇرۇپپىسى") }
    var syncedLogs by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var geminiConfigured by remember { mutableStateOf(false) }
    var aiKeyInput by remember { mutableStateOf("") }

    LaunchedEffect(syncStateJson) {
        if (!syncStateJson.isNullOrBlank()) {
            try {
                val obj = org.json.JSONObject(syncStateJson)
                whatsappStatus = obj.optString("whatsappStatus", "CONNECTED")
                geminiConfigured = obj.optBoolean("geminiConfigured", false)
                val qrRaw = obj.optString("latestQrDataUrl", "")
                latestQrBase64 = if (qrRaw.contains("base64,")) {
                    qrRaw.substringAfter("base64,")
                } else null

                val groupsArray = obj.optJSONArray("availableGroups")
                val gList = mutableListOf<Pair<String, String>>()
                if (groupsArray != null) {
                    for (i in 0 until groupsArray.length()) {
                        val g = groupsArray.optJSONObject(i)
                        if (g != null) {
                            gList.add(Pair(g.optString("id"), g.optString("subject")))
                        }
                    }
                }
                if (gList.isNotEmpty()) {
                    availableGroups = gList
                }

                val selGroup = obj.optJSONObject("selectedGroup")
                if (selGroup != null) {
                    selectedGroupName = selGroup.optString("subject", "Noor Store خېرىدارلار گۇرۇپپىسى")
                }

                val logsArray = obj.optJSONArray("syncedLogs")
                val lList = mutableListOf<Map<String, Any>>()
                if (logsArray != null) {
                    for (i in 0 until logsArray.length()) {
                        val l = logsArray.optJSONObject(i)
                        if (l != null) {
                            lList.add(mapOf(
                                "time" to l.optString("time", ""),
                                "name" to l.optString("name", ""),
                                "price" to l.optString("price", ""),
                                "supabaseSuccess" to l.optBoolean("supabaseSuccess", true),
                                "whatsappSuccess" to l.optBoolean("whatsappSuccess", true),
                                "whatsappGroup" to l.optString("whatsappGroup", "WhatsApp")
                            ))
                        }
                    }
                }
                syncedLogs = lList
            } catch (e: Exception) {}
        }
    }

    val qrBitmap = remember(latestQrBase64) {
        if (!latestQrBase64.isNullOrBlank()) {
            try {
                val bytes = android.util.Base64.decode(latestQrBase64, android.util.Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF020617) // Deep dark slate-950
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFFF59E0B))),
                                        RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⚡", fontSize = 20.sp)
                            }
                            Column {
                                Text(
                                    "Noor Store - ئاپتوماتىك ماس قەدەملەش سىستېمىسى",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399)
                                )
                                Text(
                                    "Telegram ➡️ Supabase (تور بېكەت + ئەپ) ➡️ WhatsApp",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF1E293B), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://noor-store.yulgun353.workers.dev/"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {}
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Text("🌐 تور دۇكىنى", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = onRefreshSync,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Text("🔄 يېڭىلاش", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Status Cards (4 Cards)
            // 1. Telegram Bot
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Image(
                                painter = painterResource(id = com.example.R.drawable.ic_telegram_3d),
                                contentDescription = "Telegram",
                                modifier = Modifier.size(20.dp).clip(RoundedCornerShape(5.dp))
                            )
                            Text("Telegram Bot", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                        ) {
                            Text("✅ ئۇلاندى", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                        }
                    }
                    Text("بوت: @NoorStore520_Bot", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCBD5E1))
                    Text("قانىتىش قانىلى: @NoorStore2 (Admin ID: 7251543464)", fontSize = 10.sp, color = Color(0xFF94A3B8))
                }
            }

            // 2. Supabase Cloud
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("⚡ Supabase Cloud & تور دۇكىنى", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                        ) {
                            Text("✅ ماس قەدەملەنگەن", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                        }
                    }
                    Text("تور بەت ۋە ئەپ Supabase بىلەن بىرلا ۋاقىتتا يېڭىلىنىدۇ", fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }

            // 3. AI Assistant
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🤖 سۈنئىي ئەقىل (AI) ياردەمچىسى", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (geminiConfigured) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF0284C7).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (geminiConfigured) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFF0284C7).copy(alpha = 0.3f))
                        ) {
                            Text(
                                if (geminiConfigured) "✨ Gemini AI ئاكتىپ" else "🧠 ئىچكى ئەقلىي ماتور",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (geminiConfigured) Color(0xFF34D399) else Color(0xFF38BDF8)
                            )
                        }
                    }
                    Text("مەھسۇلات باھاسى، نامى ۋە چۈشەندۈرۈشىنى ئەقلىي پەرقلەندۈرۈپ تەييارلايدۇ.", fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }

            // 3. WhatsApp Integration
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Image(
                                painter = painterResource(id = com.example.R.drawable.ic_whatsapp_3d),
                                contentDescription = "WhatsApp",
                                modifier = Modifier.size(20.dp).clip(RoundedCornerShape(5.dp))
                            )
                            Text("WhatsApp خېرىدارلار بازىسى", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
                        }
                        val isConnected = whatsappStatus == "CONNECTED"
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isConnected) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isConnected) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFFF59E0B).copy(alpha = 0.3f))
                        ) {
                            Text(
                                if (isConnected) "✅ ئوچۇق" else "⏳ ئۇلىنىۋاتىدۇ",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) Color(0xFF34D399) else Color(0xFFFBBF24)
                            )
                        }
                    }

                    // QR Code if available
                    if (qrBitmap != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📱 WhatsApp ئارقىلىق تىزىملىتىش ئۈچۈن QR كودنى سىكاننېرلاڭ:", fontSize = 11.sp, color = Color(0xFFCBD5E1))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Image(
                                    bitmap = qrBitmap,
                                    contentDescription = "WhatsApp QR Code",
                                    modifier = Modifier.size(200.dp).padding(8.dp)
                                )
                            }
                        }
                    }

                    // Selected Target Group
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF020617),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🎯 نىشانلىق WhatsApp گۇرۇپپىسى:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                            Text(selectedGroupName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                        }
                    }

                    // WhatsApp Action Buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onSendSyncCommand("{\"command\":\"REFRESH_GROUPS\"}")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Text("🔄 گۇرۇپپىلارنى يېڭىلاش", fontSize = 10.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onSendSyncCommand("{\"command\":\"RESET_WHATSAPP\"}")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Text("🔄 QR كودنى قايتا ئۇلاش", fontSize = 10.sp, color = Color(0xFFF1F5F9), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 4. Live Synced Products Logs Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📋 ئەڭ يېڭى ماس قەدەملەنگەن مەھسۇلاتلار خاتىرىسى (${syncedLogs.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCBD5E1))

                    if (syncedLogs.isEmpty()) {
                        Text(
                            "تېخى مەھسۇلات يوللانمىدى. تېلېگرام بوتىڭىزغا مەھسۇلات رەسىمى ۋە باھاسىنى تاشلاپ سىناپ بېقىڭ!",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            syncedLogs.take(15).forEach { log ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF020617),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(log["name"].toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
                                            Text(log["time"].toString(), fontSize = 9.sp, color = Color(0xFF64748B))
                                        }
                                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text("$${log["price"]}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                            Text("💬 WhatsApp ✅", fontSize = 9.sp, color = Color(0xFF38BDF8))
                                        }
                                    }
                                }
                            }
                    }
                }
            }
        }


            // 5. Quick Usage Guide Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B).copy(alpha = 0.3f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💡 تېلېگرامدىن قانداق يوللايسىز؟", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                    Text(
                        "تېلېگرام قانال ياكى گۇرۇپپىڭىزغا رەسىم بىلەن تۆۋەندىكىدەك ھەرقانداق قېلىپتا يازسىڭىزلا سىستېما تولۇق چۈشىنىدۇ:",
                        fontSize = 11.sp,
                        color = Color(0xFFA7F3D0),
                        lineHeight = 16.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF020617).copy(alpha = 0.8f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("iPhone 16 Pro Max (512GB)", fontSize = 10.sp, color = Color(0xFFE2E8F0), fontWeight = FontWeight.Bold)
                            Text("باھاسى: 8999 يۈەن", fontSize = 10.sp, color = Color(0xFF34D399), fontWeight = FontWeight.Bold)
                            Text("رەڭگى قارا، پۈتۈنلەي يېڭى، كاپالەتلىك مەھسۇلات.", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        }
                    }
                }
            }
        }
    }
}

