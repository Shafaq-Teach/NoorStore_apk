package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.NoorDatabase
import com.example.data.repository.NoorRepository
import com.example.ui.components.AIAssistantDialog
import com.example.ui.components.BottomNavBar
import com.example.ui.components.HeaderBar
import com.example.ui.language.AppLanguage
import com.example.ui.screens.*
import com.example.ui.theme.NoorStoreTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.StoreViewModel

class MainActivity : ComponentActivity() {
    private val storeViewModel: StoreViewModel by lazy {
        val db = NoorDatabase.getDatabase(applicationContext)
        val repository = NoorRepository(db)
        StoreViewModel(repository)
    }

    private val adminViewModel: AdminViewModel by lazy {
        val db = NoorDatabase.getDatabase(applicationContext)
        val repository = NoorRepository(db)
        AdminViewModel(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                android.util.Log.e("NoorStore", "Handled uncaught exception: ${throwable.message}", throwable)
            }
        } catch (e: Exception) {}

        enableEdgeToEdge()

        setContent {
            NoorStoreApp(
                storeViewModel = storeViewModel,
                adminViewModel = adminViewModel
            )
        }
    }
}

@Composable
fun NoorStoreApp(
    storeViewModel: StoreViewModel,
    adminViewModel: AdminViewModel
) {
    val context = LocalContext.current
    val currentLanguage by storeViewModel.language.collectAsStateWithLifecycle()
    val isDarkMode by storeViewModel.isDarkMode.collectAsStateWithLifecycle()
    val currentScreen by storeViewModel.currentScreen.collectAsStateWithLifecycle()
    val cartCount by storeViewModel.cartCount.collectAsStateWithLifecycle()
    val cartMap by storeViewModel.cartMap.collectAsStateWithLifecycle()
    val cartTotal by storeViewModel.cartTotal.collectAsStateWithLifecycle()
    val discountAmount by storeViewModel.discountAmount.collectAsStateWithLifecycle()
    val finalTotal by storeViewModel.finalTotal.collectAsStateWithLifecycle()
    val appliedCoupon by storeViewModel.appliedCoupon.collectAsStateWithLifecycle()
    val couponMessage by storeViewModel.couponMessage.collectAsStateWithLifecycle()
    val availableCoupons by storeViewModel.availableCoupons.collectAsStateWithLifecycle()

    val searchQuery by storeViewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategoryId by storeViewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val maxPriceFilter by storeViewModel.maxPriceFilter.collectAsStateWithLifecycle()
    val filteredProducts by storeViewModel.filteredProducts.collectAsStateWithLifecycle()
    val featuredProducts by storeViewModel.featuredProducts.collectAsStateWithLifecycle()
    val favoriteProducts by storeViewModel.favoriteProducts.collectAsStateWithLifecycle()
    val allProducts by storeViewModel.allProducts.collectAsStateWithLifecycle()
    val categories by storeViewModel.categories.collectAsStateWithLifecycle()
    val selectedProduct by storeViewModel.selectedProduct.collectAsStateWithLifecycle()
    val selectedProductId = selectedProduct?.id ?: 0
    val selectedProductReviews by remember(selectedProductId) {
        storeViewModel.getReviewsForProduct(selectedProductId)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    // Product Comparison State
    val comparedProducts by storeViewModel.comparedProducts.collectAsStateWithLifecycle()

    // AI Shopping Assistant State
    val isAiAdvisorOpen by storeViewModel.isAiAdvisorOpen.collectAsStateWithLifecycle()
    val aiChatMessages by storeViewModel.aiChatMessages.collectAsStateWithLifecycle()
    val isAiThinking by storeViewModel.isAiThinking.collectAsStateWithLifecycle()

    // Nasheed Player State
    val currentNasheedTrack by storeViewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlayingNasheed by storeViewModel.isPlayingNasheed.collectAsStateWithLifecycle()
    val isNasheedExpanded by storeViewModel.isNasheedExpanded.collectAsStateWithLifecycle()
    val isRefreshing by storeViewModel.isRefreshing.collectAsStateWithLifecycle()
    val currentTheme by storeViewModel.currentTheme.collectAsStateWithLifecycle()

    // Admin State
    val isAdminLoggedIn by adminViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val adminPinInput by adminViewModel.pinInput.collectAsStateWithLifecycle()
    val adminErrorMessage by adminViewModel.errorMessage.collectAsStateWithLifecycle()
    val adminSuccessMessage by adminViewModel.successMessage.collectAsStateWithLifecycle()
    val adminProducts by adminViewModel.products.collectAsStateWithLifecycle()
    val adminOrders by adminViewModel.orders.collectAsStateWithLifecycle()
    val adminReviews by adminViewModel.reviews.collectAsStateWithLifecycle()
    val adminCoupons by adminViewModel.coupons.collectAsStateWithLifecycle()
    val adminSyncStateJson by adminViewModel.syncStateJson.collectAsStateWithLifecycle()

    // App Update & About Us Dialog States
    var updateInfoState by remember { mutableStateOf<com.example.data.remote.AppUpdateInfo?>(null) }
    var isUpdateDismissed by remember { mutableStateOf(false) }
    var isAboutDialogOpen by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isUpdateDismissed = false
                coroutineScope.launch {
                    val info = com.example.data.remote.GitHubUpdateChecker.checkForUpdate(context)
                    if (info != null && info.isUpdateAvailable) {
                        updateInfoState = info
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides currentLanguage.layoutDirection) {
        NoorStoreTheme(appTheme = currentTheme, darkTheme = isDarkMode) {
            Scaffold(
                topBar = {
                    HeaderBar(
                        currentLanguage = currentLanguage,
                        onLanguageSelect = { storeViewModel.setLanguage(it) },
                        currentTheme = currentTheme,
                        onCycleTheme = { storeViewModel.cycleTheme() },
                        onThemeSelect = { storeViewModel.setTheme(it) },
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = { storeViewModel.toggleDarkMode() },
                        cartCount = cartCount,
                        onCartClick = { storeViewModel.navigateTo(Screen.CART) },
                        onAdminClick = { storeViewModel.navigateTo(Screen.ADMIN) },
                        onAboutClick = { isAboutDialogOpen = true },
                        onMapClick = { storeViewModel.launchMapLocation(context) }
                    )
                },
                bottomBar = {
                    BottomNavBar(
                        currentScreen = currentScreen,
                        currentLanguage = currentLanguage,
                        onScreenSelect = { storeViewModel.navigateTo(it) }
                    )
                },
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentScreen) {
                        Screen.HOME -> HomeScreen(
                            featuredProducts = featuredProducts,
                            categories = categories,
                            currentLanguage = currentLanguage,
                            isRefreshing = isRefreshing,
                            onRefresh = { storeViewModel.refreshStore() },
                            onCategoryClick = { catId ->
                                storeViewModel.selectCategory(catId)
                                storeViewModel.navigateTo(Screen.PRODUCTS)
                            },
                            onProductClick = { storeViewModel.selectProduct(it) },
                            onAddToCart = { storeViewModel.addToCart(it) },
                            onViewAllProducts = { storeViewModel.navigateTo(Screen.PRODUCTS) },
                            onCallClick = { storeViewModel.launchPhoneCall(context, "0995416715") },
                            onTelegramClick = { storeViewModel.launchTelegram(context, "sensiz09985", "Hello Noor Store!") },
                            onWhatsAppClick = { storeViewModel.launchWhatsApp(context, "0995416715", "Hello Noor Store!") },
                            onMapClick = { storeViewModel.launchMapLocation(context) },
                            onLikeClick = { storeViewModel.incrementLikes(it.id) },
                            onHeartClick = { storeViewModel.incrementHearts(it.id) },
                            onReviewClick = { storeViewModel.selectProduct(it) },
                            onOpenAiAdvisor = { storeViewModel.openAiAdvisor() },
                            onOpenCompare = { storeViewModel.navigateTo(Screen.COMPARE) },
                            onToggleCompare = { storeViewModel.toggleCompare(it) },
                            isProductCompared = { storeViewModel.isCompared(it) },
                            nasheedTracks = storeViewModel.nasheedTracks,
                            currentTrack = currentNasheedTrack,
                            isPlayingNasheed = isPlayingNasheed,
                            isNasheedExpanded = isNasheedExpanded,
                            onToggleNasheedSection = { storeViewModel.toggleNasheedSection() },
                            onPlayNasheedTrack = { storeViewModel.playNasheed(context, it) },
                            onTogglePlayPauseNasheed = { storeViewModel.togglePlayPauseNasheed() }
                        )

                        Screen.PRODUCTS -> ProductsScreen(
                            products = filteredProducts,
                            categories = categories,
                            searchQuery = searchQuery,
                            selectedCategoryId = selectedCategoryId,
                            maxPriceFilter = maxPriceFilter,
                            isRefreshing = isRefreshing,
                            onRefresh = { storeViewModel.refreshStore() },
                            currentLanguage = currentLanguage,
                            onSearchQueryChange = { storeViewModel.setSearchQuery(it) },
                            onCategorySelect = { storeViewModel.selectCategory(it) },
                            onMaxPriceSelect = { storeViewModel.setMaxPrice(it) },
                            onProductClick = { storeViewModel.selectProduct(it) },
                            onAddToCart = { storeViewModel.addToCart(it) },
                            onLikeClick = { storeViewModel.incrementLikes(it.id) },
                            onHeartClick = { storeViewModel.incrementHearts(it.id) },
                            onReviewClick = { storeViewModel.selectProduct(it) },
                            onToggleCompare = { storeViewModel.toggleCompare(it) },
                            isProductCompared = { storeViewModel.isCompared(it) }
                        )

                        Screen.FAVORITES -> FavoritesScreen(
                            favoriteProducts = favoriteProducts,
                            currentLanguage = currentLanguage,
                            isRefreshing = isRefreshing,
                            onRefresh = { storeViewModel.refreshStore() },
                            onProductClick = { storeViewModel.selectProduct(it) },
                            onAddToCart = { storeViewModel.addToCart(it) },
                            onLikeClick = { storeViewModel.incrementLikes(it.id) },
                            onHeartClick = { storeViewModel.incrementHearts(it.id) },
                            onReviewClick = { storeViewModel.selectProduct(it) }
                        )

                        Screen.COMPARE -> CompareScreen(
                            comparedProducts = comparedProducts,
                            allProducts = allProducts,
                            currentLanguage = currentLanguage,
                            onBackClick = { storeViewModel.navigateTo(Screen.HOME) },
                            onRemoveFromCompare = { storeViewModel.removeFromCompare(it) },
                            onClearCompare = { storeViewModel.clearCompare() },
                            onAddToCart = { storeViewModel.addToCart(it) },
                            onSelectProduct = { storeViewModel.selectProduct(it) },
                            onAddProductToCompare = { storeViewModel.toggleCompare(it) }
                        )

                        Screen.PRODUCT_DETAIL -> ProductDetailScreen(
                            product = selectedProduct,
                            reviews = selectedProductReviews,
                            currentLanguage = currentLanguage,
                            onBackClick = { storeViewModel.navigateTo(Screen.PRODUCTS) },
                            onAddToCart = { storeViewModel.addToCart(it) },
                            onOrderWhatsApp = { p ->
                                val name = when (currentLanguage) {
                                    AppLanguage.UYGHUR -> p.nameUg
                                    AppLanguage.ARABIC -> p.nameAr
                                    AppLanguage.ENGLISH -> p.nameEn
                                }
                                storeViewModel.launchWhatsApp(context, "0995416715", "I want to buy: $name ($${p.price})")
                            },
                            onOrderTelegram = { p ->
                                val name = when (currentLanguage) {
                                    AppLanguage.UYGHUR -> p.nameUg
                                    AppLanguage.ARABIC -> p.nameAr
                                    AppLanguage.ENGLISH -> p.nameEn
                                }
                                storeViewModel.launchTelegram(context, "sensiz09985", "I want to buy: $name ($${p.price})")
                            },
                            onCallStore = { storeViewModel.launchPhoneCall(context, "0995416715") },
                            onLikeClick = { selectedProduct?.let { storeViewModel.incrementLikes(it.id) } },
                            onHeartClick = { selectedProduct?.let { storeViewModel.incrementHearts(it.id) } },
                            onToggleCompare = { storeViewModel.toggleCompare(it) },
                            isCompared = selectedProduct?.let { storeViewModel.isCompared(it.id) } ?: false,
                            onAddReview = { name, comment ->
                                selectedProduct?.let { storeViewModel.addReview(it.id, name, comment) }
                            }
                        )

                        Screen.CART -> CartScreen(
                            cartItems = cartMap.values.toList(),
                            subtotal = cartTotal,
                            discount = discountAmount,
                            finalTotal = finalTotal,
                            appliedCoupon = appliedCoupon,
                            couponMessage = couponMessage,
                            availableCoupons = availableCoupons,
                            currentLanguage = currentLanguage,
                            onIncreaseQty = { storeViewModel.addToCart(cartMap[it]!!.product) },
                            onDecreaseQty = { storeViewModel.decreaseCartQuantity(it) },
                            onRemoveItem = { storeViewModel.removeFromCart(it) },
                            onClearCart = { storeViewModel.clearCart() },
                            onApplyCoupon = { storeViewModel.applyCoupon(it) },
                            onRemoveCoupon = { storeViewModel.removeCoupon() },
                            onShareInvoice = { invoiceText ->
                                storeViewModel.shareInvoice(context, invoiceText)
                            },
                            onSubmitOrder = { name, phone, note, channel ->
                                storeViewModel.submitOrder(name, phone, note, channel, context)
                            }
                        )

                        Screen.CONTACT -> ContactScreen(
                            currentLanguage = currentLanguage,
                            onCallClick = { storeViewModel.launchPhoneCall(context, "0995416715") },
                            onTelegramClick = { storeViewModel.launchTelegram(context, "sensiz09985", "Hello Noor Store!") },
                            onWhatsAppClick = { storeViewModel.launchWhatsApp(context, "0995416715", "Hello Noor Store!") },
                            onMapClick = { storeViewModel.launchMapLocation(context) }
                        )

                        Screen.ADMIN -> AdminScreen(
                            isLoggedIn = isAdminLoggedIn,
                            pinInput = adminPinInput,
                            errorMessage = adminErrorMessage,
                            successMessage = adminSuccessMessage,
                            products = adminProducts,
                            orders = adminOrders,
                            reviews = adminReviews,
                            coupons = adminCoupons,
                            currentLanguage = currentLanguage,
                            onPinChange = { adminViewModel.updatePinInput(it) },
                            onLogin = { adminViewModel.login() },
                            onLogout = { adminViewModel.logout() },
                            onChangePin = { oldP, newP, confP -> adminViewModel.changePin(oldP, newP, confP) },
                            onUpdateOrderStatus = { id, st -> adminViewModel.updateOrderStatus(id, st) },
                            onDeleteOrder = { id -> adminViewModel.deleteOrder(id) },
                            onNotifyCustomer = { ord, st -> adminViewModel.notifyCustomerOrderStatus(context, ord, st, currentLanguage) },
                            onUpdateProductPrice = { id, pr -> adminViewModel.updateProductPrice(id, pr) },
                            onUpdateProduct = { prod -> adminViewModel.updateProduct(prod) },
                            onAddProduct = { nUg, nAr, nEn, dUg, dAr, dEn, pr, cat, br, img1, img2, img3, feat, st ->
                                adminViewModel.addProduct(nUg, nAr, nEn, dUg, dAr, dEn, pr, cat, br, img1, img2, img3, feat, st)
                            },
                            onDeleteProduct = { adminViewModel.deleteProduct(it) },
                            onToggleStock = { adminViewModel.toggleStock(it) },
                            onToggleFeatured = { adminViewModel.toggleFeatured(it) },
                            onAddCoupon = { c, p, a, m, ug, ar, en -> adminViewModel.addCoupon(c, p, a, m, ug, ar, en) },
                            onDeleteCoupon = { adminViewModel.deleteCoupon(it) },
                            onReplyReview = { revId, reply -> adminViewModel.replyToReview(revId, reply) },
                            onDeleteReview = { revId -> adminViewModel.deleteReview(revId) },
                            onShareSalesReport = {
                                val report = adminViewModel.generateSalesReport(currentLanguage)
                                adminViewModel.shareSalesReport(context, report)
                            },
                            syncStateJson = adminSyncStateJson,
                            onSendSyncCommand = { adminViewModel.sendSyncCommand(it) },
                            onRefreshSync = { adminViewModel.fetchSyncState() }
                        )
                    }

                    // AI Shopping Assistant Modal Bottom Sheet
                    if (isAiAdvisorOpen) {
                        AIAssistantDialog(
                            messages = aiChatMessages,
                            isThinking = isAiThinking,
                            currentLanguage = currentLanguage,
                            onDismiss = { storeViewModel.closeAiAdvisor() },
                            onSendMessage = { storeViewModel.askAiAdvisor(it) },
                            onResetChat = { storeViewModel.resetAiChat() },
                            onProductClick = {
                                storeViewModel.closeAiAdvisor()
                                storeViewModel.selectProduct(it)
                            },
                            onAddToCart = { storeViewModel.addToCart(it) }
                        )
                    }

                    // In-App Auto-Update Dialog (Automated from GitHub Releases)
                    updateInfoState?.let { info ->
                        if (!isUpdateDismissed) {
                            com.example.ui.components.AppUpdateDialog(
                                updateInfo = info,
                                onDismiss = { isUpdateDismissed = true }
                            )
                        }
                    }

                    // About Us Dialog
                    if (isAboutDialogOpen) {
                        val verName = remember(context) {
                            try {
                                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.1"
                            } catch (e: Exception) {
                                "1.0.1"
                            }
                        }
                        com.example.ui.components.AboutDialog(
                            currentVersion = verName,
                            onDismiss = { isAboutDialogOpen = false }
                        )
                    }
                }
            }
        }
    }
}

