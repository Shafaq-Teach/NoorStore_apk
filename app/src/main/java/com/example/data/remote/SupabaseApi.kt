package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

object SupabaseConfig {
    const val BASE_URL = "https://xmlesmevinugetrqtznh.supabase.co/rest/v1/"
    const val API_KEY = "sb_publishable_aXtsifwgdWh_n9kBkvb-pQ_zC2htb3E"
}

@JsonClass(generateAdapter = true)
data class SupabaseProductDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "name_ug") val nameUg: String? = null,
    @Json(name = "name_ar") val nameAr: String? = null,
    @Json(name = "name_en") val nameEn: String? = null,
    @Json(name = "description_ug") val descriptionUg: String? = null,
    @Json(name = "description_ar") val descriptionAr: String? = null,
    @Json(name = "description_en") val descriptionEn: String? = null,
    @Json(name = "price") val price: Double? = null,
    @Json(name = "original_price") val originalPrice: Double? = null,
    @Json(name = "category_id") val categoryId: String? = null,
    @Json(name = "brand") val brand: String? = null,
    @Json(name = "image_res_name") val imageResName: String? = null,
    @Json(name = "image_res_name2") val imageResName2: String? = null,
    @Json(name = "image_res_name3") val imageResName3: String? = null,
    @Json(name = "is_featured") val isFeatured: Boolean? = null,
    @Json(name = "in_stock") val inStock: Boolean? = null,
    @Json(name = "specs_ug") val specsUg: String? = null,
    @Json(name = "specs_ar") val specsAr: String? = null,
    @Json(name = "specs_en") val specsEn: String? = null,
    @Json(name = "likes_count") val likesCount: Int? = null,
    @Json(name = "hearts_count") val heartsCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseOrderDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "customer_name") val customerName: String? = null,
    @Json(name = "customer_phone") val customerPhone: String? = null,
    @Json(name = "items_json") val itemsJson: String? = null,
    @Json(name = "total_price") val totalPrice: Double? = null,
    @Json(name = "order_date") val orderDate: Long? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "note") val note: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseReviewDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "product_id") val productId: Long? = null,
    @Json(name = "user_name") val userName: String? = null,
    @Json(name = "rating") val rating: Int? = null,
    @Json(name = "comment") val comment: String? = null,
    @Json(name = "admin_reply") val adminReply: String? = null,
    @Json(name = "timestamp") val timestamp: Long? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseCouponDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "code") val code: String? = null,
    @Json(name = "discount_percent") val discountPercent: Double? = null,
    @Json(name = "discount_amount") val discountAmount: Double? = null,
    @Json(name = "min_spend") val minSpend: Double? = null,
    @Json(name = "desc_ug") val descUg: String? = null,
    @Json(name = "desc_ar") val descAr: String? = null,
    @Json(name = "desc_en") val descEn: String? = null
)

interface SupabaseApi {
    @GET("products?select=*&order=created_at.desc")
    suspend fun getProducts(
        @Header("apikey") apiKey: String = SupabaseConfig.API_KEY,
        @Header("Authorization") auth: String = "Bearer ${SupabaseConfig.API_KEY}"
    ): Response<List<SupabaseProductDto>>

    @POST("products")
    suspend fun insertProduct(
        @Header("apikey") apiKey: String = SupabaseConfig.API_KEY,
        @Header("Authorization") auth: String = "Bearer ${SupabaseConfig.API_KEY}",
        @Header("Prefer") prefer: String = "return=representation",
        @Body product: SupabaseProductDto
    ): Response<List<SupabaseProductDto>>

    @PATCH("products")
    suspend fun updateProduct(
        @Query("id") idFilter: String,
        @Header("apikey") apiKey: String = SupabaseConfig.API_KEY,
        @Header("Authorization") auth: String = "Bearer ${SupabaseConfig.API_KEY}",
        @Body product: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @DELETE("products")
    suspend fun deleteProduct(
        @Query("id") idFilter: String,
        @Header("apikey") apiKey: String = SupabaseConfig.API_KEY,
        @Header("Authorization") auth: String = "Bearer ${SupabaseConfig.API_KEY}"
    ): Response<Unit>

    // Orders
    @GET("orders?select=*&order=order_date.desc")
    suspend fun getOrders(
        @Header("apikey") apiKey: String = SupabaseConfig.API_KEY,
        @Header("Authorization") auth: String = "Bearer ${SupabaseConfig.API_KEY}"
    ): Response<List<SupabaseOrderDto>>

    @POST("orders")
    suspend fun insertOrder(
        @Header("apikey") apiKey: String = SupabaseConfig.API_KEY,
        @Header("Authorization") auth: String = "Bearer ${SupabaseConfig.API_KEY}",
        @Header("Prefer") prefer: String = "return=representation",
        @Body order: SupabaseOrderDto
    ): Response<List<SupabaseOrderDto>>

    @POST("orders?on_conflict=id")
    suspend fun upsertOrder(
        @Header("apikey") apiKey: String = SupabaseConfig.API_KEY,
        @Header("Authorization") auth: String = "Bearer ${SupabaseConfig.API_KEY}",
        @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation",
        @Body order: SupabaseOrderDto
    ): Response<List<SupabaseOrderDto>>

    @PATCH("orders")
    suspend fun updateOrderStatus(
        @Query("id") idFilter: String,
        @Header("apikey") apiKey: String = SupabaseConfig.API_KEY,
        @Header("Authorization") auth: String = "Bearer ${SupabaseConfig.API_KEY}",
        @Body updates: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @DELETE("orders")
    suspend fun deleteOrder(
        @Query("id") idFilter: String,
        @Header("apikey") apiKey: String = SupabaseConfig.API_KEY,
        @Header("Authorization") auth: String = "Bearer ${SupabaseConfig.API_KEY}"
    ): Response<Unit>

    // Reviews
    @GET("reviews?select=*&order=timestamp.desc")
    suspend fun getReviews(
        @Header("apikey") apiKey: String = SupabaseConfig.API_KEY,
        @Header("Authorization") auth: String = "Bearer ${SupabaseConfig.API_KEY}"
    ): Response<List<SupabaseReviewDto>>

    @GET("reviews?user_name=eq.__ADMIN_PIN__&limit=1")
    suspend fun getAdminPin(
        @Header("apikey") apiKey: String = SupabaseConfig.API_KEY,
        @Header("Authorization") auth: String = "Bearer ${SupabaseConfig.API_KEY}"
    ): Response<List<SupabaseReviewDto>>

    @GET("reviews?user_name=eq.__SYNC_STATE__&limit=1")
    suspend fun getSyncState(
        @Header("apikey") apiKey: String = SupabaseConfig.API_KEY,
        @Header("Authorization") auth: String = "Bearer ${SupabaseConfig.API_KEY}"
    ): Response<List<SupabaseReviewDto>>

    @PATCH("reviews")
    suspend fun pushSyncCommand(
        @Query("id") idFilter: String = "eq.999999",
        @Header("apikey") apiKey: String = SupabaseConfig.API_KEY,
        @Header("Authorization") auth: String = "Bearer ${SupabaseConfig.API_KEY}",
        @Body updates: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @POST("reviews")
    suspend fun insertReview(
        @Header("apikey") apiKey: String = SupabaseConfig.API_KEY,
        @Header("Authorization") auth: String = "Bearer ${SupabaseConfig.API_KEY}",
        @Header("Prefer") prefer: String = "return=representation",
        @Body review: SupabaseReviewDto
    ): Response<List<SupabaseReviewDto>>

    @POST("reviews?on_conflict=id")
    suspend fun upsertReview(
        @Header("apikey") apiKey: String = SupabaseConfig.API_KEY,
        @Header("Authorization") auth: String = "Bearer ${SupabaseConfig.API_KEY}",
        @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation",
        @Body review: SupabaseReviewDto
    ): Response<List<SupabaseReviewDto>>

    @PATCH("reviews")
    suspend fun updateReview(
        @Query("id") idFilter: String,
        @Header("apikey") apiKey: String = SupabaseConfig.API_KEY,
        @Header("Authorization") auth: String = "Bearer ${SupabaseConfig.API_KEY}",
        @Body updates: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @DELETE("reviews")
    suspend fun deleteReview(
        @Query("id") idFilter: String,
        @Header("apikey") apiKey: String = SupabaseConfig.API_KEY,
        @Header("Authorization") auth: String = "Bearer ${SupabaseConfig.API_KEY}"
    ): Response<Unit>

    // Coupons
    @GET("coupons?select=*&order=id.asc")
    suspend fun getCoupons(
        @Header("apikey") apiKey: String = SupabaseConfig.API_KEY,
        @Header("Authorization") auth: String = "Bearer ${SupabaseConfig.API_KEY}"
    ): Response<List<SupabaseCouponDto>>

    @POST("coupons")
    suspend fun insertCoupon(
        @Header("apikey") apiKey: String = SupabaseConfig.API_KEY,
        @Header("Authorization") auth: String = "Bearer ${SupabaseConfig.API_KEY}",
        @Header("Prefer") prefer: String = "return=representation",
        @Body coupon: SupabaseCouponDto
    ): Response<List<SupabaseCouponDto>>

    @DELETE("coupons")
    suspend fun deleteCoupon(
        @Query("code") codeFilter: String,
        @Header("apikey") apiKey: String = SupabaseConfig.API_KEY,
        @Header("Authorization") auth: String = "Bearer ${SupabaseConfig.API_KEY}"
    ): Response<Unit>
}

object SupabaseClient {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(logging)
        .build()

    val api: SupabaseApi by lazy {
        Retrofit.Builder()
            .baseUrl(SupabaseConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(SupabaseApi::class.java)
    }
}
