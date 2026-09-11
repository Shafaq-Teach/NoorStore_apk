package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nameUg: String,
    val nameAr: String,
    val nameEn: String,
    val descriptionUg: String,
    val descriptionAr: String,
    val descriptionEn: String,
    val price: Double,
    val originalPrice: Double = 0.0,
    val categoryId: String, // "phones", "tablets", "accessories", "watches"
    val brand: String,
    val imageResName: String, // Primary image (Drawable resource name, URI, or URL)
    val imageResName2: String = "", // Second image
    val imageResName3: String = "", // Third image
    val isFeatured: Boolean = false,
    val inStock: Boolean = true,
    val specsUg: String = "",
    val specsAr: String = "",
    val specsEn: String = "",
    val likesCount: Int = 0,
    val heartsCount: Int = 0,
    val createdAt: String = ""
) {
    fun getAllImages(): List<String> {
        val list = mutableListOf<String>()
        if (imageResName.isNotBlank()) list.add(imageResName)
        if (imageResName2.isNotBlank()) list.add(imageResName2)
        if (imageResName3.isNotBlank()) list.add(imageResName3)
        return if (list.isEmpty()) listOf("img_phones_1786037591338") else list
    }
}
