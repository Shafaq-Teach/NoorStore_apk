package com.example.data.repository

import com.example.data.local.*
import com.example.data.remote.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NoorRepository(private val db: NoorDatabase) {
    val allProducts: Flow<List<ProductEntity>> = db.productDao().getAllProducts()
    val featuredProducts: Flow<List<ProductEntity>> = db.productDao().getFeaturedProducts()
    val allCategories: Flow<List<CategoryEntity>> = db.categoryDao().getAllCategories()
    val allOrders: Flow<List<OrderEntity>> = db.orderDao().getAllOrders()
    val allReviews: Flow<List<ReviewEntity>> = db.reviewDao().getAllReviews()

    // Dynamic Coupons Store
    private val _coupons = kotlinx.coroutines.flow.MutableStateFlow<List<Coupon>>(
        listOf(
            Coupon("NOOR10", discountPercent = 10.0, minSpend = 0.0, descUg = "بارلىق ماللارغا 10% ئېتىبار", descAr = "خصم 10% على كل السلة", descEn = "10% Off All Orders"),
            Coupon("YENGILIK", discountAmount = 100.0, minSpend = 1000.0, descUg = "1000$ دىن ئاشسا 100$ كېمەيتىش", descAr = "خصم 100$ للطلبات فوق 1000$", descEn = "$100 Off for orders over $1000"),
            Coupon("VIP2026", discountAmount = 200.0, minSpend = 2000.0, descUg = "2000$ دىن ئاشسا 200$ كېمەيتىش", descAr = "خصم 200$ للطلبات فوق 2000$", descEn = "$200 Off for orders over $2000"),
            Coupon("TEZLIK", discountAmount = 50.0, minSpend = 500.0, descUg = "500$ دىن ئاشسا 50$ كېمەيتىش + تېز يەتكۈزۈش", descAr = "خصم 50$ + توصيل سريع", descEn = "$50 Off + Express Delivery")
        )
    )
    val coupons: kotlinx.coroutines.flow.StateFlow<List<Coupon>> = _coupons

    private val api: SupabaseApi = SupabaseClient.api

    fun addCoupon(coupon: Coupon) {
        val current = _coupons.value.toMutableList()
        current.removeAll { it.code.equals(coupon.code, ignoreCase = true) }
        current.add(0, coupon)
        _coupons.value = current

        // Sync to Supabase
        CoroutineScope(Dispatchers.IO).launch {
            try {
                api.insertCoupon(
                    coupon = SupabaseCouponDto(
                        code = coupon.code,
                        discountPercent = coupon.discountPercent,
                        discountAmount = coupon.discountAmount,
                        minSpend = coupon.minSpend,
                        descUg = coupon.descUg,
                        descAr = coupon.descAr,
                        descEn = coupon.descEn
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("NoorRepository", "Supabase coupon sync error: ${e.message}")
            }
        }
    }

    fun deleteCoupon(code: String) {
        val current = _coupons.value.toMutableList()
        current.removeAll { it.code.equals(code, ignoreCase = true) }
        _coupons.value = current

        // Sync to Supabase
        CoroutineScope(Dispatchers.IO).launch {
            try {
                api.deleteCoupon(codeFilter = "eq.$code")
            } catch (e: Exception) {
                android.util.Log.e("NoorRepository", "Supabase coupon delete error: ${e.message}")
            }
        }
    }

    suspend fun updateOrderStatus(orderId: Int, status: String) {
        db.orderDao().updateOrderStatus(orderId, status)
        try {
            api.updateOrderStatus(
                idFilter = "eq.$orderId",
                updates = mapOf("status" to status)
            )
        } catch (e: Exception) {
            android.util.Log.e("NoorRepository", "Supabase updateOrderStatus error: ${e.message}")
        }
    }

    suspend fun deleteOrder(orderId: Int) {
        db.orderDao().deleteOrder(orderId)
        try {
            api.deleteOrder(idFilter = "eq.$orderId")
        } catch (e: Exception) {
            android.util.Log.e("NoorRepository", "Supabase deleteOrder error: ${e.message}")
        }
    }

    suspend fun updateProductPrice(productId: Int, price: Double) {
        db.productDao().updateProductPrice(productId, price)
        try {
            api.updateProduct(
                idFilter = "eq.$productId",
                product = mapOf("price" to price)
            )
        } catch (e: Exception) {
            android.util.Log.e("NoorRepository", "Supabase updateProductPrice error: ${e.message}")
        }
    }

    fun getReviewsForProduct(productId: Int): Flow<List<ReviewEntity>> {
        return db.reviewDao().getReviewsForProduct(productId)
    }

    suspend fun addReview(review: ReviewEntity): Long {
        val id = db.reviewDao().insertReview(review)
        try {
            api.insertReview(
                review = SupabaseReviewDto(
                    id = if (id > 0) id else null,
                    productId = review.productId.toLong(),
                    userName = review.userName,
                    rating = review.rating,
                    comment = review.comment,
                    adminReply = review.adminReply,
                    timestamp = review.timestamp
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("NoorRepository", "Supabase addReview error: ${e.message}")
        }
        return id
    }

    suspend fun replyToReview(reviewId: Int, reply: String) {
        db.reviewDao().updateAdminReply(reviewId, reply)
        try {
            api.updateReview(
                idFilter = "eq.$reviewId",
                updates = mapOf("admin_reply" to reply)
            )
        } catch (e: Exception) {
            android.util.Log.e("NoorRepository", "Supabase replyToReview error: ${e.message}")
        }
    }

    suspend fun deleteReview(reviewId: Int) {
        db.reviewDao().deleteReview(reviewId)
        try {
            api.deleteReview(idFilter = "eq.$reviewId")
        } catch (e: Exception) {
            android.util.Log.e("NoorRepository", "Supabase deleteReview error: ${e.message}")
        }
    }

    suspend fun incrementLikes(productId: Int) {
        db.productDao().incrementLikes(productId)
        val product = db.productDao().getProductById(productId)
        if (product != null) {
            try {
                api.updateProduct(
                    idFilter = "eq.$productId",
                    product = mapOf("likes_count" to product.likesCount)
                )
            } catch (e: Exception) {
                android.util.Log.e("NoorRepository", "Supabase incrementLikes error: ${e.message}")
            }
        }
    }

    suspend fun incrementHearts(productId: Int) {
        db.productDao().incrementHearts(productId)
        val product = db.productDao().getProductById(productId)
        if (product != null) {
            try {
                api.updateProduct(
                    idFilter = "eq.$productId",
                    product = mapOf("hearts_count" to product.heartsCount)
                )
            } catch (e: Exception) {
                android.util.Log.e("NoorRepository", "Supabase incrementHearts error: ${e.message}")
            }
        }
    }

    fun getProductsByCategory(catId: String): Flow<List<ProductEntity>> {
        return db.productDao().getProductsByCategory(catId)
    }

    suspend fun getProductById(id: Int): ProductEntity? {
        return db.productDao().getProductById(id)
    }

    suspend fun insertProduct(product: ProductEntity): Long {
        val nextId = if (product.id > 0) product.id.toLong() else (System.currentTimeMillis() / 1000)
        try {
            val response = api.insertProduct(
                product = SupabaseProductDto(
                    id = nextId,
                    nameUg = product.nameUg,
                    nameAr = product.nameAr,
                    nameEn = product.nameEn,
                    descriptionUg = product.descriptionUg,
                    descriptionAr = product.descriptionAr,
                    descriptionEn = product.descriptionEn,
                    price = product.price,
                    originalPrice = product.originalPrice,
                    categoryId = product.categoryId,
                    brand = product.brand,
                    imageResName = product.imageResName,
                    imageResName2 = product.imageResName2,
                    imageResName3 = product.imageResName3,
                    isFeatured = product.isFeatured,
                    inStock = product.inStock,
                    specsUg = product.specsUg,
                    specsAr = product.specsAr,
                    specsEn = product.specsEn,
                    likesCount = product.likesCount,
                    heartsCount = product.heartsCount
                )
            )
            if (response.isSuccessful) {
                val insertedList = response.body().orEmpty()
                if (insertedList.isNotEmpty()) {
                    val remote = insertedList[0]
                    val remoteId = remote.id?.toInt() ?: 0
                    val entityWithRemoteId = product.copy(id = if (remoteId > 0) remoteId else nextId.toInt())
                    return db.productDao().insertProduct(entityWithRemoteId)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NoorRepository", "Supabase insertProduct error: ${e.message}")
        }
        return db.productDao().insertProduct(product.copy(id = if (product.id > 0) product.id else nextId.toInt()))
    }

    suspend fun updateProduct(product: ProductEntity) {
        db.productDao().updateProduct(product)
        try {
            api.updateProduct(
                idFilter = "eq.${product.id}",
                product = mapOf(
                    "name_ug" to product.nameUg,
                    "name_ar" to product.nameAr,
                    "name_en" to product.nameEn,
                    "description_ug" to product.descriptionUg,
                    "description_ar" to product.descriptionAr,
                    "description_en" to product.descriptionEn,
                    "price" to product.price,
                    "original_price" to product.originalPrice,
                    "category_id" to product.categoryId,
                    "brand" to product.brand,
                    "image_res_name" to product.imageResName,
                    "image_res_name2" to product.imageResName2,
                    "image_res_name3" to product.imageResName3,
                    "is_featured" to product.isFeatured,
                    "in_stock" to product.inStock,
                    "specs_ug" to product.specsUg,
                    "specs_ar" to product.specsAr,
                    "specs_en" to product.specsEn,
                    "likes_count" to product.likesCount,
                    "hearts_count" to product.heartsCount
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("NoorRepository", "Supabase updateProduct error: ${e.message}")
        }
    }

    suspend fun deleteProduct(id: Int) {
        db.productDao().deleteProductById(id)
        try {
            api.deleteProduct(idFilter = "eq.$id")
        } catch (e: Exception) {
            android.util.Log.e("NoorRepository", "Supabase deleteProduct error: ${e.message}")
        }
    }

    suspend fun insertOrder(order: OrderEntity): Long {
        val nextOrderId = if (order.id > 0) order.id.toLong() else (System.currentTimeMillis() / 1000)
        val entityWithId = order.copy(id = nextOrderId.toInt())
        val localId = db.orderDao().insertOrder(entityWithId)
        try {
            api.insertOrder(
                order = SupabaseOrderDto(
                    id = nextOrderId,
                    customerName = order.customerName,
                    customerPhone = order.customerPhone,
                    itemsJson = order.orderSummary,
                    totalPrice = order.totalAmount,
                    orderDate = order.orderDate,
                    status = order.status,
                    note = order.note
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("NoorRepository", "Supabase insertOrder error: ${e.message}")
        }
        return localId
    }

    suspend fun syncFromSupabase() {
        try {
            val response = api.getProducts()
            if (response.isSuccessful) {
                val remoteList = response.body().orEmpty()
                if (remoteList.isNotEmpty()) {
                    db.productDao().deleteAll()
                    for (item in remoteList) {
                        val entity = ProductEntity(
                            id = item.id?.toInt() ?: 0,
                            nameUg = item.nameUg ?: "مەھسۇلات",
                            nameAr = item.nameAr ?: item.nameUg ?: "منتج",
                            nameEn = item.nameEn ?: item.nameUg ?: "Product",
                            descriptionUg = item.descriptionUg ?: "",
                            descriptionAr = item.descriptionAr ?: "",
                            descriptionEn = item.descriptionEn ?: "",
                            price = item.price ?: 0.0,
                            originalPrice = item.originalPrice ?: ((item.price ?: 0.0) * 1.1),
                            categoryId = item.categoryId ?: "phones",
                            brand = item.brand ?: "Generic",
                            imageResName = item.imageResName ?: "img_phones_1786037591338",
                            imageResName2 = item.imageResName2 ?: "",
                            imageResName3 = item.imageResName3 ?: "",
                            isFeatured = item.isFeatured ?: false,
                            inStock = item.inStock ?: true,
                            specsUg = item.specsUg ?: "",
                            specsAr = item.specsAr ?: "",
                            specsEn = item.specsEn ?: "",
                            likesCount = item.likesCount ?: 0,
                            heartsCount = item.heartsCount ?: 0
                        )
                        db.productDao().insertProduct(entity)
                    }
                }
            }

            // Sync Orders from Supabase to Local
            val ordersResp = api.getOrders()
            if (ordersResp.isSuccessful) {
                val remoteOrders = ordersResp.body().orEmpty()
                for (o in remoteOrders) {
                    val orderEntity = OrderEntity(
                        id = o.id?.toInt() ?: 0,
                        customerName = o.customerName ?: "خېرىدار",
                        customerPhone = o.customerPhone ?: "",
                        orderSummary = o.itemsJson ?: "",
                        totalAmount = o.totalPrice ?: 0.0,
                        orderDate = o.orderDate ?: System.currentTimeMillis(),
                        status = o.status ?: "Pending",
                        note = o.note ?: ""
                    )
                    db.orderDao().insertOrder(orderEntity)
                }
            }

            // Sync Reviews from Supabase to Local
            val reviewsResp = api.getReviews()
            if (reviewsResp.isSuccessful) {
                val remoteReviews = reviewsResp.body().orEmpty().filter { 
                    it.userName != "__ADMIN_PIN__" && it.userName != "__SYNC_STATE__" 
                }
                for (r in remoteReviews) {
                    val reviewEntity = ReviewEntity(
                        id = r.id?.toInt() ?: 0,
                        productId = r.productId?.toInt() ?: 0,
                        userName = r.userName ?: "خېرىدار",
                        rating = r.rating ?: 5,
                        comment = r.comment ?: "",
                        adminReply = r.adminReply ?: "",
                        timestamp = r.timestamp ?: System.currentTimeMillis()
                    )
                    db.reviewDao().insertReview(reviewEntity)
                }
            }

            // Sync Coupons from Supabase
            val couponsResp = api.getCoupons()
            if (couponsResp.isSuccessful) {
                val remoteCoupons = couponsResp.body().orEmpty()
                if (remoteCoupons.isNotEmpty()) {
                    val list = remoteCoupons.map {
                        Coupon(
                            code = it.code ?: "",
                            discountPercent = it.discountPercent ?: 0.0,
                            discountAmount = it.discountAmount ?: 0.0,
                            minSpend = it.minSpend ?: 0.0,
                            descUg = it.descUg ?: "",
                            descAr = it.descAr ?: "",
                            descEn = it.descEn ?: ""
                        )
                    }
                    _coupons.value = list
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NoorRepository", "Supabase sync error: ${e.message}")
        }
    }

    suspend fun fetchSharedCartFromSupabase(): Map<Int, Int> {
        try {
            val response = api.getOrders()
            if (response.isSuccessful) {
                val cartOrder = response.body()?.firstOrNull { it.id == 999999L || it.status == "Cart" }
                val jsonStr = cartOrder?.itemsJson
                if (!jsonStr.isNullOrBlank()) {
                    val map = mutableMapOf<Int, Int>()
                    val pattern = Regex(""""id"\s*:\s*([0-9]+).*?"qty"\s*:\s*([0-9]+)""")
                    for (match in pattern.findAll(jsonStr)) {
                        val pid = match.groupValues[1].toIntOrNull()
                        val qty = match.groupValues[2].toIntOrNull()
                        if (pid != null && qty != null) {
                            map[pid] = qty
                        }
                    }
                    return map
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NoorRepository", "fetchSharedCart error: ${e.message}")
        }
        return emptyMap()
    }

    suspend fun syncCartToSupabase(cartItems: List<Pair<ProductEntity, Int>>) {
        try {
            val jsonBuilder = StringBuilder("[")
            cartItems.forEachIndexed { index, pair ->
                if (index > 0) jsonBuilder.append(",")
                val product = pair.first
                val qty = pair.second
                val safeName = (product.nameUg ?: "مەھسۇلات").replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ")
                val safeImage = (product.imageResName ?: "").replace("\\", "\\\\").replace("\"", "\\\"")
                jsonBuilder.append("""{"id":${product.id},"name":"$safeName","price":${product.price},"qty":$qty,"image":"$safeImage"}""")
            }
            jsonBuilder.append("]")
            val total = cartItems.sumOf { it.first.price * it.second }
            val jsonStr = jsonBuilder.toString()
            android.util.Log.i("NOOR_CART", "Syncing cart to Supabase: $jsonStr")

            val response = api.upsertOrder(
                order = SupabaseOrderDto(
                    id = 999999L,
                    customerName = "ئورتاق سىۋەت (Shared Cart)",
                    customerPhone = "shared_cart",
                    itemsJson = jsonStr,
                    totalPrice = total,
                    orderDate = System.currentTimeMillis(),
                    status = "Cart",
                    note = "Shared Cart across Web & Mobile App"
                )
            )
            android.util.Log.i("NOOR_CART", "Upsert cart response code: ${response.code()}")
        } catch (e: Exception) {
            android.util.Log.e("NoorRepository", "syncCartToSupabase error: ${e.message}", e)
        }
    }

    suspend fun seedInitialDataIfEmpty() {
        try {
            val currentCategories = db.categoryDao().getAllCategories().first()
            if (currentCategories.isEmpty()) {
                val categories = listOf(
                    CategoryEntity("phones", "تىلىپۇنلار", "الهواتف", "Phones", "phone_iphone"),
                    CategoryEntity("tablets", "پەدلەر", "الأجهزة اللوحية", "Tablets / Pads", "tablet_mac"),
                    CategoryEntity("accessories", "زاپچاسلار", "الملحقات", "Accessories", "headphones"),
                    CategoryEntity("watches", "ئەقلىي سائەتلەر", "الساعات الذكية", "Smart Watches", "watch")
                )
                db.categoryDao().insertCategories(categories)
            }

            // Directly sync latest data from Supabase without injecting or pushing mock products
            syncFromSupabase()
        } catch (e: Exception) {
            android.util.Log.e("NoorRepository", "Failed to seed data: ${e.message}", e)
        }
    }

    suspend fun refreshStoreData() {
        seedInitialDataIfEmpty()
        syncFromSupabase()
    }

    suspend fun fetchAdminPin(): String {
        return try {
            val resp = api.getAdminPin()
            if (resp.isSuccessful) {
                val list = resp.body()
                if (!list.isNullOrEmpty() && !list[0].comment.isNullOrBlank()) {
                    list[0].comment!!.trim()
                } else {
                    "1234"
                }
            } else {
                "1234"
            }
        } catch (e: Exception) {
            android.util.Log.e("NoorRepository", "Failed fetching Admin PIN: ${e.message}")
            "1234"
        }
    }

    suspend fun updateAdminPin(newPin: String): Boolean {
        return try {
            val cleanPin = newPin.trim()
            val dto = SupabaseReviewDto(
                id = 888888L,
                productId = 1L,
                userName = "__ADMIN_PIN__",
                rating = 5,
                comment = cleanPin,
                timestamp = System.currentTimeMillis()
            )
            val resp = api.upsertReview(review = dto)
            resp.isSuccessful
        } catch (e: Exception) {
            android.util.Log.e("NoorRepository", "Failed updating Admin PIN in Supabase: ${e.message}")
            false
        }
    }

    suspend fun fetchSyncStateJson(): String? {
        return try {
            val resp = api.getSyncState()
            if (resp.isSuccessful) {
                val list = resp.body()
                if (!list.isNullOrEmpty() && !list[0].comment.isNullOrBlank()) {
                    list[0].comment
                } else null
            } else null
        } catch (e: Exception) {
            android.util.Log.e("NoorRepository", "Failed fetching Sync State: ${e.message}")
            null
        }
    }

    suspend fun sendSyncCommand(commandJson: String): Boolean {
        return try {
            val resp = api.pushSyncCommand(updates = mapOf("admin_reply" to commandJson))
            resp.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}


