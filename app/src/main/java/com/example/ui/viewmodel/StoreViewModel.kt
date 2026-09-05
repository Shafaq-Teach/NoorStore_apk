package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
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
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class NasheedTrack(
    val id: String,
    val title: String,
    val rawResId: Int
)

enum class Screen {
    HOME, PRODUCTS, FAVORITES, PRODUCT_DETAIL, CART, CONTACT, ADMIN, COMPARE
}

data class CartItem(
    val product: ProductEntity,
    val quantity: Int
)

data class AIChatMessage(
    val isUser: Boolean,
    val text: String,
    val recommendedProducts: List<ProductEntity> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

class StoreViewModel(private val repository: NoorRepository) : ViewModel() {

    private val _language = MutableStateFlow(AppLanguage.UYGHUR)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false) // Default to light mode (day)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _currentTheme = MutableStateFlow(AppThemeMode.SKY_BLUE)
    val currentTheme: StateFlow<AppThemeMode> = _currentTheme.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _currentScreen = MutableStateFlow(Screen.HOME)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _maxPriceFilter = MutableStateFlow<Double?>(null)
    val maxPriceFilter: StateFlow<Double?> = _maxPriceFilter.asStateFlow()

    private val _selectedProduct = MutableStateFlow<ProductEntity?>(null)
    val selectedProduct: StateFlow<ProductEntity?> = _selectedProduct.asStateFlow()

    private val _cartMap = MutableStateFlow<Map<Int, CartItem>>(emptyMap())
    val cartMap: StateFlow<Map<Int, CartItem>> = _cartMap.asStateFlow()

    // Device Comparison State
    private val _comparedProductIds = MutableStateFlow<List<Int>>(emptyList())
    val comparedProductIds: StateFlow<List<Int>> = _comparedProductIds.asStateFlow()

    // Coupon & Promo Code State
    val availableCoupons: StateFlow<List<Coupon>> = repository.coupons

    private val _appliedCoupon = MutableStateFlow<Coupon?>(null)
    val appliedCoupon: StateFlow<Coupon?> = _appliedCoupon.asStateFlow()

    private val _couponMessage = MutableStateFlow<String?>(null)
    val couponMessage: StateFlow<String?> = _couponMessage.asStateFlow()

    // AI Shopping Assistant State
    private val _isAiAdvisorOpen = MutableStateFlow(false)
    val isAiAdvisorOpen: StateFlow<Boolean> = _isAiAdvisorOpen.asStateFlow()

    private val _aiChatMessages = MutableStateFlow<List<AIChatMessage>>(emptyList())
    val aiChatMessages: StateFlow<List<AIChatMessage>> = _aiChatMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    fun openAiAdvisor() {
        _isAiAdvisorOpen.value = true
    }

    fun closeAiAdvisor() {
        _isAiAdvisorOpen.value = false
    }

    // Last Placed Order for Receipt/Invoice Sharing
    private val _lastPlacedInvoice = MutableStateFlow<String?>(null)
    val lastPlacedInvoice: StateFlow<String?> = _lastPlacedInvoice.asStateFlow()

    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val comparedProducts: StateFlow<List<ProductEntity>> = combine(allProducts, _comparedProductIds) { list, ids ->
        ids.mapNotNull { id -> list.find { it.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val featuredProducts: StateFlow<List<ProductEntity>> = repository.featuredProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteProducts: StateFlow<List<ProductEntity>> = allProducts
        .map { list -> list.filter { it.heartsCount > 0 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        allProducts,
        _searchQuery,
        _selectedCategoryId,
        _maxPriceFilter
    ) { products, query, catId, maxPrice ->
        products.filter { p ->
            val matchesQuery = query.isBlank() ||
                    p.nameUg.contains(query, ignoreCase = true) ||
                    p.nameAr.contains(query, ignoreCase = true) ||
                    p.nameEn.contains(query, ignoreCase = true) ||
                    p.brand.contains(query, ignoreCase = true)

            val matchesCat = catId == null || p.categoryId == catId
            val matchesPrice = maxPrice == null || p.price <= maxPrice

            matchesQuery && matchesCat && matchesPrice
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartTotal: StateFlow<Double> = _cartMap.map { map ->
        map.values.sumOf { it.product.price * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val discountAmount: StateFlow<Double> = combine(cartTotal, _appliedCoupon) { total, coupon ->
        if (coupon == null || total < coupon.minSpend) 0.0
        else {
            if (coupon.discountPercent > 0) {
                (total * coupon.discountPercent / 100.0)
            } else {
                coupon.discountAmount.coerceAtMost(total)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val finalTotal: StateFlow<Double> = combine(cartTotal, discountAmount) { total, discount ->
        (total - discount).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartCount: StateFlow<Int> = _cartMap.map { map ->
        map.values.sumOf { it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            try {
                repository.seedInitialDataIfEmpty()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                repository.syncFromSupabase()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                initAiWelcomeMessage()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Periodic cloud synchronization
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(10000)
                try {
                    repository.syncFromSupabase()
                } catch (e: Exception) {
                    // silent
                }
            }
        }
    }

    fun refreshStore() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.refreshStoreData()
                val sharedCartMap = repository.fetchSharedCartFromSupabase()
                if (sharedCartMap.isNotEmpty()) {
                    val products = repository.allProducts.first()
                    val newMap = mutableMapOf<Int, CartItem>()
                    sharedCartMap.forEach { (pid, qty) ->
                        val p = products.find { it.id == pid }
                        if (p != null) {
                            newMap[pid] = CartItem(p, qty)
                        }
                    }
                    if (newMap.isNotEmpty()) {
                        _cartMap.value = newMap
                    }
                }
                delay(600)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
    }

    fun setTheme(theme: AppThemeMode) {
        _currentTheme.value = theme
    }

    fun cycleTheme() {
        val themes = AppThemeMode.values()
        val currentIndex = themes.indexOf(_currentTheme.value)
        val nextIndex = (currentIndex + 1) % themes.size
        _currentTheme.value = themes[nextIndex]
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(catId: String?) {
        _selectedCategoryId.value = catId
    }

    fun setMaxPrice(price: Double?) {
        _maxPriceFilter.value = price
    }

    fun selectProduct(product: ProductEntity) {
        _selectedProduct.value = product
        _currentScreen.value = Screen.PRODUCT_DETAIL
    }

    fun addToCart(product: ProductEntity) {
        val current = _cartMap.value.toMutableMap()
        val existing = current[product.id]
        if (existing != null) {
            current[product.id] = existing.copy(quantity = existing.quantity + 1)
        } else {
            current[product.id] = CartItem(product, 1)
        }
        _cartMap.value = current
        viewModelScope.launch {
            repository.syncCartToSupabase(current.values.map { Pair(it.product, it.quantity) })
        }
    }

    fun decreaseCartQuantity(productId: Int) {
        val current = _cartMap.value.toMutableMap()
        val existing = current[productId] ?: return
        if (existing.quantity > 1) {
            current[productId] = existing.copy(quantity = existing.quantity - 1)
        } else {
            current.remove(productId)
        }
        _cartMap.value = current
        viewModelScope.launch {
            repository.syncCartToSupabase(current.values.map { Pair(it.product, it.quantity) })
        }
    }

    fun removeFromCart(productId: Int) {
        val current = _cartMap.value.toMutableMap()
        current.remove(productId)
        _cartMap.value = current
        viewModelScope.launch {
            repository.syncCartToSupabase(current.values.map { Pair(it.product, it.quantity) })
        }
    }

    fun clearCart() {
        _cartMap.value = emptyMap()
        viewModelScope.launch {
            repository.syncCartToSupabase(emptyList())
        }
    }

    // Comparison feature methods
    fun toggleCompare(product: ProductEntity) {
        val current = _comparedProductIds.value.toMutableList()
        if (current.contains(product.id)) {
            current.remove(product.id)
        } else {
            if (current.size >= 3) {
                current.removeAt(0) // keep max 3 items
            }
            current.add(product.id)
        }
        _comparedProductIds.value = current
    }

    fun removeFromCompare(productId: Int) {
        _comparedProductIds.value = _comparedProductIds.value.filter { it != productId }
    }

    fun clearCompare() {
        _comparedProductIds.value = emptyList()
    }

    fun isCompared(productId: Int): Boolean {
        return _comparedProductIds.value.contains(productId)
    }

    // Promo code & Coupon methods
    fun applyCoupon(code: String) {
        val cleanCode = code.trim().uppercase()
        val found = availableCoupons.value.find { it.code.equals(cleanCode, ignoreCase = true) }
        val currentTotal = cartTotal.value
        val lang = _language.value

        if (found != null) {
            if (currentTotal >= found.minSpend) {
                _appliedCoupon.value = found
                _couponMessage.value = AppStrings.get("code_applied", lang)
            } else {
                _couponMessage.value = AppStrings.get("invalid_code", lang) + " (Min: $${found.minSpend.toInt()})"
            }
        } else {
            _couponMessage.value = AppStrings.get("invalid_code", lang)
        }
    }

    fun removeCoupon() {
        _appliedCoupon.value = null
        _couponMessage.value = null
    }

    // AI Shopping Advisor methods
    fun initAiWelcomeMessage() {
        if (_aiChatMessages.value.isEmpty()) {
            val lang = _language.value
            val welcomeText = when (lang) {
                AppLanguage.UYGHUR -> "ياخشىمۇسىز! مەن «نۇرلۇق» ئەقلىي مەسلىھەتچىسى. سىزگە خامچوتىڭىز، خىزمەت ياكى كۈندىلىك ئېھتىياجىڭىزغا ماس كېلىدىغان تېلېفون ۋە پەدلەرنى تەۋسىيە قىلالايمەن. تۆۋەندىكى تېز تاللاشلارنى بېسىڭ ياكى سوئالىڭىزنى يېزىڭ!"
                AppLanguage.ARABIC -> "أهلاً بك! أنا مستشارك الذكي في متجر النور. يمكنني مساعدتك باختيار أفضل هاتف أو تابلت يناسب ميزانيتك واستخدامك. اختر من الخيارات السريعة أو اكتب سؤالك!"
                AppLanguage.ENGLISH -> "Hello! I am your Noor Smart Shopping Advisor. Tell me your budget or needs (camera, battery, study, gaming), and I'll find the perfect device for you!"
            }
            _aiChatMessages.value = listOf(
                AIChatMessage(isUser = false, text = welcomeText)
            )
        }
    }

    fun askAiAdvisor(query: String) {
        if (query.isBlank()) return
        val lang = _language.value
        val userMsg = AIChatMessage(isUser = true, text = query)
        _aiChatMessages.value = _aiChatMessages.value + userMsg
        _isAiThinking.value = true

        viewModelScope.launch {
            kotlinx.coroutines.delay(400) // smooth responsive delay
            val products = allProducts.value
            val q = query.lowercase()

            val recommended = mutableListOf<ProductEntity>()
            val responseText: String

            when {
                // Camera query
                q.contains("كامېرا") || q.contains("camera") || q.contains("تصوير") || q.contains("رەسىم") -> {
                    recommended.addAll(products.filter { it.specsEn.contains("MP", ignoreCase = true) || it.specsUg.contains("كامېرا") || it.categoryId == "phones" }.take(2))
                    responseText = when (lang) {
                        AppLanguage.UYGHUR -> "سۈرەت ۋە سىن ئېلىشقا ئەڭ يۇقىرى دەرىجىلىك كۆپ كامېرالىق، ئوپتىكىلىق تۇراقلاشتۇرغۇچلۇق بايراقدار تېلېفونلارنى تەۋسىيە قىلىمەن:"
                        AppLanguage.ARABIC -> "إذا كنت تبحث عن كاميرا احترافية وتصوير بدقة فائقة، إليك أفضل الأجهزة المميزة بكاميرات متقدمة:"
                        AppLanguage.ENGLISH -> "For exceptional photography and video quality, here are our best camera flagships:"
                    }
                }
                // Tablet / Study query
                q.contains("پەد") || q.contains("tablet") || q.contains("تابلت") || q.contains("ئوقۇش") || q.contains("دراسة") || q.contains("ipad") -> {
                    recommended.addAll(products.filter { it.categoryId == "tablets" || it.specsEn.contains("inch", ignoreCase = true) }.take(3))
                    responseText = when (lang) {
                        AppLanguage.UYGHUR -> "ئوقۇش، ئىشخانا خىزمىتى، كىنو كۆرۈش ۋە رەسىم سىزىشقا چوڭ سۈزۈك ئېكرانلىق، قەلەم قوللايدىغان پەدلەر ئەڭ مۇۋاپىق:"
                        AppLanguage.ARABIC -> "للدراسة، العمل، الرسم ومشاهدة المحتوى، إليك أفضل الأجهزة اللوحية (التابلت) بشاشات واسعة:"
                        AppLanguage.ENGLISH -> "For study, remote work, drawing, and media consumption, here are our recommended tablets:"
                    }
                }
                // Budget under 3000 query
                q.contains("3000") || q.contains("ئەرزان") || q.contains("رخيص") || q.contains("budget") || q.contains("خامچوت") -> {
                    recommended.addAll(products.filter { it.price <= 3500 }.sortedBy { it.price }.take(3))
                    responseText = when (lang) {
                        AppLanguage.UYGHUR -> "باھا ۋە ئىقتىدار نىسبىتى ئەڭ يۇقىرى، 3000 يۈەن ئەتراپىدىكى تەۋسىيەلىك ئەلا سۈپەتلىك تاللاشلار:"
                        AppLanguage.ARABIC -> "إليك أفضل الهواتف والأجهزة الاقتصادية ذات الأداء العالي والمواصفات الممتازة بأفضل سعر:"
                        AppLanguage.ENGLISH -> "Here are our top high-value devices offering incredible performance within budget:"
                    }
                }
                // Battery query
                q.contains("باتارېيە") || q.contains("battery") || q.contains("بطارية") || q.contains("زەرەت") -> {
                    recommended.addAll(products.filter { it.specsEn.contains("mAh", ignoreCase = true) || it.specsUg.contains("mAh") }.take(2))
                    responseText = when (lang) {
                        AppLanguage.UYGHUR -> "بىر كۈندىن ئارتۇق بىمالال يېتىدىغان چوڭ سىغىملىق باتارېيەلىك ۋە تېز قاچىلىغۇچلۇق تېلېفونلار:"
                        AppLanguage.ARABIC -> "أجهزة ببطاريات عملاقة تدوم طويلاً مع دعم الشحن السريع الفائق:"
                        AppLanguage.ENGLISH -> "Devices equipped with large-capacity batteries and ultra-fast charging:"
                    }
                }
                // Gaming & Performance
                q.contains("ئويۇن") || q.contains("game") || q.contains("gaming") || q.contains("ألعاب") || q.contains("سۈرئەت") -> {
                    recommended.addAll(products.filter { it.price >= 4000 || it.specsEn.contains("Pro", ignoreCase = true) || it.isFeatured }.take(2))
                    responseText = when (lang) {
                        AppLanguage.UYGHUR -> "ئېغىر دەرىجىلىك 3D ئويۇنلار ۋە يۇقىرى ئىقتىدارلىق پروگراممىلارغا ماس كېلىدىغان كۈچلۈك بىر تەرەپ قىلغۇچلۇق بايراقدارلار:"
                        AppLanguage.ARABIC -> "للألعاب الثقيلة والمهام الشاقة، إليك الأجهزة الأقوى مع أفضل المعالجات وشاشات التردد العالي:"
                        AppLanguage.ENGLISH -> "Ultimate powerhouses with top-tier processors and high refresh rate screens for gaming & multitasking:"
                    }
                }
                // Fallback / General search
                else -> {
                    val matching = products.filter {
                        it.nameUg.contains(query, ignoreCase = true) ||
                        it.nameEn.contains(query, ignoreCase = true) ||
                        it.brand.contains(query, ignoreCase = true)
                    }
                    if (matching.isNotEmpty()) {
                        recommended.addAll(matching.take(2))
                        responseText = when (lang) {
                            AppLanguage.UYGHUR -> "ئىزدىشىڭىزگە مۇناسىۋەتلىك ئامباردىكى ئەڭ ياخشى مەھسۇلاتلار تەييارلاندى:"
                            AppLanguage.ARABIC -> "إليك أفضل النتائج المتطابقة مع طلبك في متجرنا:"
                            AppLanguage.ENGLISH -> "Here are the best matching items from our store catalogue:"
                        }
                    } else {
                        recommended.addAll(products.filter { it.isFeatured }.take(2))
                        responseText = when (lang) {
                            AppLanguage.UYGHUR -> "سوئالىڭىزغا ئاساسەن دۇكىنىمىزدىكى ئەڭ ياخشى سېتىلىۋاتقان ئەلا سۈپەتلىك تاللاشلارنى تەۋسىيە قىلىمەن:"
                            AppLanguage.ARABIC -> "بناءً على طلبك، إليك أكثر الأجهزة مبيعاً وشهرة في المتجر:"
                            AppLanguage.ENGLISH -> "Based on your request, here are our most popular and highest rated devices:"
                        }
                    }
                }
            }

            _isAiThinking.value = false
            _aiChatMessages.value = _aiChatMessages.value + AIChatMessage(
                isUser = false,
                text = responseText,
                recommendedProducts = recommended
            )
        }
    }

    fun resetAiChat() {
        _aiChatMessages.value = emptyList()
        initAiWelcomeMessage()
    }

    // Invoice & Receipt Generation & Sharing methods
    fun generateInvoiceText(
        orderId: String,
        customerName: String,
        customerPhone: String,
        note: String,
        items: List<CartItem>,
        subtotal: Double,
        discount: Double,
        total: Double,
        couponCode: String?
    ): String {
        val lang = _language.value
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        val dateStr = sdf.format(java.util.Date())

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
            sb.append("   • Qty: ${item.quantity}  ×  $${item.product.price}  =  $${item.product.price * item.quantity}\n")
        }
        sb.append("─────────────────────────────\n")
        sb.append("💵 ${AppStrings.get("subtotal", lang)}: $$subtotal\n")
        if (discount > 0) {
            sb.append("🏷️ ${AppStrings.get("discount", lang)} ($couponCode): -$$discount\n")
        }
        sb.append("⭐ ${AppStrings.get("total_price", lang)}: $$total\n\n")

        if (note.isNotBlank()) {
            sb.append("📝 ${AppStrings.get("order_note", lang)}: $note\n\n")
        }

        sb.append("🏢 ${AppStrings.get("store_address", lang)}\n")
        sb.append("🕒 ${AppStrings.get("business_hours", lang)}\n")
        sb.append("☎️ WhatsApp: 0995416715 | Telegram: @sensiz09985\n")
        sb.append("═══════════════════════════════\n")
        return sb.toString()
    }

    fun shareInvoice(context: Context, invoiceText: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Noor Store Order Invoice")
                putExtra(Intent.EXTRA_TEXT, invoiceText)
            }
            context.startActivity(Intent.createChooser(intent, "Share Invoice"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun submitOrder(customerName: String, customerPhone: String, note: String, channel: String, context: Context) {
        val items = _cartMap.value.values.toList()
        if (items.isEmpty()) return

        val lang = _language.value
        val subtotal = cartTotal.value
        val discount = discountAmount.value
        val total = finalTotal.value
        val couponCode = _appliedCoupon.value?.code

        val orderNum = "NOOR-" + (10000..99999).random()

        val summaryBuilder = StringBuilder()
        items.forEach { item ->
            val pName = when (lang) {
                AppLanguage.UYGHUR -> item.product.nameUg
                AppLanguage.ARABIC -> item.product.nameAr
                AppLanguage.ENGLISH -> item.product.nameEn
            }
            summaryBuilder.append("• $pName x${item.quantity} = $${item.product.price * item.quantity}\n")
        }

        val summaryStr = summaryBuilder.toString()

        viewModelScope.launch {
            repository.insertOrder(
                OrderEntity(
                    customerName = customerName,
                    customerPhone = customerPhone,
                    orderSummary = summaryStr,
                    totalAmount = total,
                    note = note
                )
            )
        }

        val invoice = generateInvoiceText(
            orderId = orderNum,
            customerName = customerName,
            customerPhone = customerPhone,
            note = note,
            items = items,
            subtotal = subtotal,
            discount = discount,
            total = total,
            couponCode = couponCode
        )

        _lastPlacedInvoice.value = invoice

        if (channel == "whatsapp") {
            launchWhatsApp(context, "0995416715", invoice)
        } else if (channel == "telegram") {
            launchTelegram(context, "sensiz09985", invoice)
        }
    }

    fun launchWhatsApp(context: Context, phone: String, message: String) {
        try {
            val formattedPhone = if (phone.startsWith("+")) phone else "+86$phone"
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            val uri = Uri.parse("https://wa.me/0995416715?text=${Uri.encode(message)}")
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    fun launchTelegram(context: Context, username: String, message: String) {
        try {
            val cleanUsername = username.removePrefix("@")
            val uri = Uri.parse("https://t.me/$cleanUsername?text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/sensiz09985"))
            context.startActivity(intent)
        }
    }

    fun launchPhoneCall(context: Context, phone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            context.startActivity(intent)
        } catch (e: Exception) {}
    }

    fun launchMapLocation(context: Context) {
        val lat = 40.99958
        val lon = 28.79152
        val label = "ئىستانبول سافا كۆي نۇرلۇق تىلىپۇنچىلىقى باش دۇكىنى"
        try {
            val uriStr = "geo:$lat,$lon?q=$lat,$lon(${Uri.encode(label)})"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr))
            context.startActivity(intent)
        } catch (e: Exception) {
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lon")
            val intent = Intent(Intent.ACTION_VIEW, webUri)
            context.startActivity(intent)
        }
    }

    fun getReviewsForProduct(productId: Int): Flow<List<ReviewEntity>> {
        return repository.getReviewsForProduct(productId)
    }

    fun addReview(productId: Int, userName: String, comment: String) {
        if (comment.isBlank()) return
        viewModelScope.launch {
            val name = userName.trim().ifBlank { "خېرىدار" }
            repository.addReview(
                ReviewEntity(
                    productId = productId,
                    userName = name,
                    comment = comment.trim()
                )
            )
        }
    }

    fun incrementLikes(productId: Int) {
        viewModelScope.launch {
            repository.incrementLikes(productId)
        }
    }

    fun incrementHearts(productId: Int) {
        viewModelScope.launch {
            repository.incrementHearts(productId)
        }
    }

    // Nasheed Player Integration
    val nasheedTracks = listOf(
        NasheedTrack("1", "كۆك بايراق", com.example.R.raw.kok_bayraq),
        NasheedTrack("2", "بالىلىقنى سېغىندىم", com.example.R.raw.baliliqni_seghindim),
        NasheedTrack("3", "نەشىد 1", com.example.R.raw.nashid_1),
        NasheedTrack("4", "نەشىد 2", com.example.R.raw.nashid_2)
    )

    private var mediaPlayer: MediaPlayer? = null

    private val _currentTrack = MutableStateFlow<NasheedTrack?>(null)
    val currentTrack: StateFlow<NasheedTrack?> = _currentTrack.asStateFlow()

    private val _isPlayingNasheed = MutableStateFlow(false)
    val isPlayingNasheed: StateFlow<Boolean> = _isPlayingNasheed.asStateFlow()

    private val _isNasheedExpanded = MutableStateFlow(false)
    val isNasheedExpanded: StateFlow<Boolean> = _isNasheedExpanded.asStateFlow()

    fun toggleNasheedSection() {
        _isNasheedExpanded.value = !_isNasheedExpanded.value
    }

    fun playNasheed(context: Context, track: NasheedTrack) {
        android.util.Log.i("NOOR_AUDIO", "playNasheed triggered for ${track.title}, resId=${track.rawResId}")
        if (_currentTrack.value?.id == track.id && mediaPlayer != null) {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                _isPlayingNasheed.value = false
            } else {
                mediaPlayer?.start()
                _isPlayingNasheed.value = true
            }
        } else {
            try {
                try {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                } catch (e: Exception) {}
                mediaPlayer = null

                val player = MediaPlayer()
                var isInitialized = false

                try {
                    val afd = context.resources.openRawResourceFd(track.rawResId)
                    if (afd != null) {
                        player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                        isInitialized = true
                    }
                } catch (e: Exception) {
                    android.util.Log.w("NOOR_AUDIO", "openRawResourceFd failed: ${e.message}")
                }

                if (!isInitialized) {
                    val tempFile = File(context.cacheDir, "track_${track.id}.mp3")
                    if (!tempFile.exists() || tempFile.length() < 1000L) {
                        context.resources.openRawResource(track.rawResId).use { input ->
                            tempFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    val fis = java.io.FileInputStream(tempFile)
                    player.setDataSource(fis.fd, 0, tempFile.length())
                    fis.close()
                }

                player.setOnPreparedListener { mp ->
                    mp.start()
                    _isPlayingNasheed.value = true
                    android.widget.Toast.makeText(context, "ئاڭلىنىۋاتىدۇ: ${track.title} 🔊", android.widget.Toast.LENGTH_SHORT).show()
                }
                player.setOnCompletionListener {
                    _isPlayingNasheed.value = false
                }
                player.setOnErrorListener { _, what, extra ->
                    android.util.Log.e("NOOR_AUDIO", "MediaPlayer error: what=$what, extra=$extra")
                    _isPlayingNasheed.value = false
                    true
                }
                player.prepareAsync()
                mediaPlayer = player
                _currentTrack.value = track
                _isNasheedExpanded.value = false
            } catch (e: Exception) {
                android.util.Log.e("NOOR_AUDIO", "Fatal audio error: ${e.message}", e)
                android.widget.Toast.makeText(context, "خاتالىق: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                _isPlayingNasheed.value = false
            }
        }
    }

    fun togglePlayPauseNasheed() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _isPlayingNasheed.value = false
            } else {
                player.start()
                _isPlayingNasheed.value = true
            }
        }
    }

    fun stopNasheed() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _currentTrack.value = null
        _isPlayingNasheed.value = false
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

