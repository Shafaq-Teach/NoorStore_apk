package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.Coupon
import com.example.data.local.OrderEntity
import com.example.data.local.ProductEntity
import com.example.data.local.ReviewEntity
import com.example.data.repository.NoorRepository
import com.example.ui.language.AppLanguage
import com.example.ui.language.AppStrings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminViewModel(private val repository: NoorRepository) : ViewModel() {

    private val _currentAdminPin = MutableStateFlow("1234") // Default PIN
    val currentAdminPin: StateFlow<String> = _currentAdminPin.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _syncStateJson = MutableStateFlow<String?>(null)
    val syncStateJson: StateFlow<String?> = _syncStateJson.asStateFlow()

    init {
        // Fetch global cloud Admin PIN on start
        viewModelScope.launch {
            val cloudPin = repository.fetchAdminPin()
            if (cloudPin.isNotBlank()) {
                _currentAdminPin.value = cloudPin
            }
        }
        // Poll sync state periodically
        viewModelScope.launch {
            while (true) {
                try {
                    val json = repository.fetchSyncStateJson()
                    if (!json.isNullOrBlank()) {
                        _syncStateJson.value = json
                    }
                } catch (e: Exception) {}
                kotlinx.coroutines.delay(4000)
            }
        }
    }

    fun fetchSyncState() {
        viewModelScope.launch {
            val json = repository.fetchSyncStateJson()
            if (!json.isNullOrBlank()) {
                _syncStateJson.value = json
            }
        }
    }

    fun sendSyncCommand(commandJson: String) {
        viewModelScope.launch {
            repository.sendSyncCommand(commandJson)
            kotlinx.coroutines.delay(1000)
            fetchSyncState()
        }
    }

    val orders: StateFlow<List<OrderEntity>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reviews: StateFlow<List<ReviewEntity>> = repository.allReviews
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val coupons: StateFlow<List<Coupon>> = repository.coupons

    fun updatePinInput(pin: String) {
        _pinInput.value = pin
        _errorMessage.value = null
    }

    fun login() {
        val input = _pinInput.value.trim()
        viewModelScope.launch {
            // Verify against fresh Supabase Cloud PIN
            val freshCloudPin = repository.fetchAdminPin()
            _currentAdminPin.value = freshCloudPin
            if (input == freshCloudPin) {
                _isLoggedIn.value = true
                _errorMessage.value = null
            } else {
                _errorMessage.value = "wrong_pin"
            }
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _pinInput.value = ""
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun changePin(oldPin: String, newPin: String, confirmPin: String): Boolean {
        _errorMessage.value = null
        _successMessage.value = null

        val currentPin = _currentAdminPin.value
        if (oldPin.trim() != currentPin) {
            _errorMessage.value = "current_pin_wrong"
            return false
        }
        if (newPin.isBlank() || newPin != confirmPin) {
            _errorMessage.value = "pin_mismatch"
            return false
        }

        val cleanNewPin = newPin.trim()
        _currentAdminPin.value = cleanNewPin
        _successMessage.value = "pin_changed_success"

        // Immediately update Supabase Cloud so Website & all devices invalidate old PIN
        viewModelScope.launch {
            repository.updateAdminPin(cleanNewPin)
        }
        return true
    }

    // Order Lifecycle Operations
    fun updateOrderStatus(orderId: Int, status: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, status)
        }
    }

    fun deleteOrder(orderId: Int) {
        viewModelScope.launch {
            repository.deleteOrder(orderId)
        }
    }

    fun notifyCustomerOrderStatus(context: Context, order: OrderEntity, newStatus: String, language: AppLanguage) {
        val statusText = when (newStatus) {
            "Processing" -> when (language) {
                AppLanguage.UYGHUR -> "📦 تەييارلىنىۋاتىدۇ (Processing)"
                AppLanguage.ARABIC -> "📦 قيد التجهيز (Processing)"
                AppLanguage.ENGLISH -> "📦 Processing"
            }
            "Shipped" -> when (language) {
                AppLanguage.UYGHUR -> "🚚 يوللاندى (Shipped)"
                AppLanguage.ARABIC -> "🚚 تم الشحن (Shipped)"
                AppLanguage.ENGLISH -> "🚚 Shipped"
            }
            "Completed" -> when (language) {
                AppLanguage.UYGHUR -> "✅ تاپشۇرۇلدى (Completed)"
                AppLanguage.ARABIC -> "✅ مكتمل (Completed)"
                AppLanguage.ENGLISH -> "✅ Completed"
            }
            "Cancelled" -> when (language) {
                AppLanguage.UYGHUR -> "❌ بىكار قىلىندى (Cancelled)"
                AppLanguage.ARABIC -> "❌ ملغي (Cancelled)"
                AppLanguage.ENGLISH -> "❌ Cancelled"
            }
            else -> when (language) {
                AppLanguage.UYGHUR -> "⏳ يېڭى زاكاز (Pending)"
                AppLanguage.ARABIC -> "⏳ قيد الانتظار (Pending)"
                AppLanguage.ENGLISH -> "⏳ Pending"
            }
        }

        val greeting = when (language) {
            AppLanguage.UYGHUR -> "ئەسسالامۇ ئەلەيكۇم ھۆرمەتلىك ${order.customerName}!"
            AppLanguage.ARABIC -> "مرحباً بكم عزيزنا ${order.customerName}!"
            AppLanguage.ENGLISH -> "Hello dear ${order.customerName}!"
        }

        val msg = """
            $greeting
            🛒 ${AppStrings.get("invoice_title", language)}
            🆔 #${order.id}
            📌 ${AppStrings.get("order_status", language)}: $statusText
            💰 ${AppStrings.get("total_price", language)}: $${order.totalAmount}
            📝 ${AppStrings.get("order_note", language)}: ${order.note.ifBlank { "N/A" }}
            
            📍 ${AppStrings.get("store_address", language)}
            📞 0995416715
        """.trimIndent()

        try {
            val cleanPhone = order.customerPhone.replace(Regex("[^0-9+]"), "")
            val targetUri = if (cleanPhone.isNotBlank()) {
                Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(msg)}")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(msg)}")
            }
            val intent = Intent(Intent.ACTION_VIEW, targetUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, msg)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share status").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }

    // Product Inventory & Quick Actions
    fun updateProductPrice(productId: Int, newPrice: Double) {
        if (newPrice < 0) return
        viewModelScope.launch {
            repository.updateProductPrice(productId, newPrice)
        }
    }

    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.updateProduct(product)
        }
    }

    fun addProduct(
        nameUg: String, nameAr: String, nameEn: String,
        descUg: String, descAr: String, descEn: String,
        price: Double, categoryId: String, brand: String,
        image1: String, image2: String, image3: String,
        isFeatured: Boolean, inStock: Boolean
    ) {
        viewModelScope.launch {
            val primaryName = nameUg.trim().ifBlank { nameAr.trim().ifBlank { nameEn.trim() } }
            val finalUgName = nameUg.trim().ifBlank { primaryName }
            val finalArName = nameAr.trim().ifBlank { primaryName }
            val finalEnName = nameEn.trim().ifBlank { primaryName }

            val primaryDesc = descUg.trim().ifBlank { descAr.trim().ifBlank { descEn.trim() } }
            val finalUgDesc = descUg.trim().ifBlank { primaryDesc }
            val finalArDesc = descAr.trim().ifBlank { primaryDesc }
            val finalEnDesc = descEn.trim().ifBlank { primaryDesc }

            val defaultImg = if (categoryId == "tablets") "img_tablets_1786037603482" else "img_phones_1786037591338"
            val img1 = image1.trim().ifBlank { defaultImg }

            val newProduct = ProductEntity(
                nameUg = finalUgName,
                nameAr = finalArName,
                nameEn = finalEnName,
                descriptionUg = finalUgDesc,
                descriptionAr = finalArDesc,
                descriptionEn = finalEnDesc,
                price = price,
                categoryId = categoryId,
                brand = brand.trim().ifBlank { "Generic" },
                imageResName = img1,
                imageResName2 = image2.trim(),
                imageResName3 = image3.trim(),
                isFeatured = isFeatured,
                inStock = inStock
            )
            repository.insertProduct(newProduct)
        }
    }

    fun deleteProduct(id: Int) {
        viewModelScope.launch {
            repository.deleteProduct(id)
        }
    }

    fun toggleStock(product: ProductEntity) {
        viewModelScope.launch {
            repository.updateProduct(product.copy(inStock = !product.inStock))
        }
    }

    fun toggleFeatured(product: ProductEntity) {
        viewModelScope.launch {
            repository.updateProduct(product.copy(isFeatured = !product.isFeatured))
        }
    }

    // Coupon Engine
    fun addCoupon(
        code: String,
        discountPercent: Double,
        discountAmount: Double,
        minSpend: Double,
        descUg: String,
        descAr: String,
        descEn: String
    ) {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isBlank()) return
        val primaryDesc = descUg.trim().ifBlank { descAr.trim().ifBlank { descEn.trim() } }
        val finalUg = descUg.trim().ifBlank { primaryDesc }
        val finalAr = descAr.trim().ifBlank { primaryDesc }
        val finalEn = descEn.trim().ifBlank { primaryDesc }

        val coupon = Coupon(
            code = cleanCode,
            discountPercent = discountPercent,
            discountAmount = discountAmount,
            minSpend = minSpend,
            descUg = finalUg,
            descAr = finalAr,
            descEn = finalEn
        )
        repository.addCoupon(coupon)
    }

    fun deleteCoupon(code: String) {
        repository.deleteCoupon(code)
    }

    // Reviews moderation
    fun replyToReview(reviewId: Int, reply: String) {
        if (reply.isBlank()) return
        viewModelScope.launch {
            repository.replyToReview(reviewId, reply.trim())
        }
    }

    fun deleteReview(reviewId: Int) {
        viewModelScope.launch {
            repository.deleteReview(reviewId)
        }
    }

    // Financial & Inventory Report Generator
    fun generateSalesReport(language: AppLanguage): String {
        val currentOrders = orders.value
        val currentProducts = products.value
        val totalRevenue = currentOrders.sumOf { it.totalAmount }
        val totalInventoryValue = currentProducts.sumOf { it.price }
        val completedCount = currentOrders.count { it.status.equals("Completed", ignoreCase = true) }
        val pendingCount = currentOrders.count { it.status.equals("Pending", ignoreCase = true) }
        val outOfStockCount = currentProducts.count { !it.inStock }
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        return """
            📊 ${AppStrings.get("sales_report_title", language)}
            📅 ${AppStrings.get("date", language)}: $dateStr
            ━━━━━━━━━━━━━━━━━━━━━━
            💰 ${AppStrings.get("total_sales_revenue", language)}: $$totalRevenue
            📦 ${AppStrings.get("total_inventory_value", language)}: $$totalInventoryValue
            🛒 ${AppStrings.get("total_orders_count", language)}: ${currentOrders.size}
            ⏳ ${AppStrings.get("pending_orders", language)}: $pendingCount
            ✅ ${AppStrings.get("completed_orders", language)}: $completedCount
            ⚠️ ${AppStrings.get("low_stock_warning", language)}: $outOfStockCount
            ━━━━━━━━━━━━━━━━━━━━━━
            🏬 ${AppStrings.get("store_address", language)}
            📞 0995416715 | @sensiz09985
        """.trimIndent()
    }

    fun shareSalesReport(context: Context, reportText: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, reportText)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Sales Report").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}


