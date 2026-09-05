package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CategoryEntity
import com.example.data.local.ProductEntity
import com.example.ui.components.ProductCard
import com.example.ui.language.AppLanguage
import com.example.ui.language.AppStrings
import com.example.ui.theme.GoldPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    products: List<ProductEntity>,
    categories: List<CategoryEntity>,
    searchQuery: String,
    selectedCategoryId: String?,
    maxPriceFilter: Double?,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    currentLanguage: AppLanguage,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onMaxPriceSelect: (Double?) -> Unit,
    onProductClick: (ProductEntity) -> Unit,
    onAddToCart: (ProductEntity) -> Unit,
    onLikeClick: (ProductEntity) -> Unit = {},
    onHeartClick: (ProductEntity) -> Unit = {},
    onReviewClick: (ProductEntity) -> Unit = {},
    onToggleCompare: (ProductEntity) -> Unit = {},
    isProductCompared: (Int) -> Boolean = { false }
) {
    var showFilterSheet by remember { mutableStateOf(false) }

    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("products_pull_to_refresh"),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                color = GoldPrimary,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar & Filter Toggle Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_text_field"),
                placeholder = {
                    Text(
                        text = AppStrings.get("search_hint", currentLanguage),
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = GoldPrimary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Filter Button
            IconButton(
                onClick = { showFilterSheet = true },
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        if (maxPriceFilter != null) GoldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("filter_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = if (maxPriceFilter != null) Color.Black else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Category Horizontal Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = { onCategorySelect(null) },
                    label = { Text(AppStrings.get("all_categories", currentLanguage), fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldPrimary,
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.testTag("cat_chip_all")
                )
            }

            items(categories) { cat ->
                val catName = when (currentLanguage) {
                    AppLanguage.UYGHUR -> AppStrings.get(cat.id, AppLanguage.UYGHUR).ifEmpty { cat.nameUg }
                    AppLanguage.ARABIC -> cat.nameAr
                    AppLanguage.ENGLISH -> cat.nameEn
                }
                FilterChip(
                    selected = selectedCategoryId == cat.id,
                    onClick = { onCategorySelect(cat.id) },
                    label = { Text(catName, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldPrimary,
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.testTag("cat_chip_${cat.id}")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Product Count
        Text(
            text = "${products.size} ${AppStrings.get("products", currentLanguage)}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "مەھسۇلات تاپىلمىدى / لم يتم العثور على منتجات",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(products) { product ->
                    ProductCard(
                        product = product,
                        currentLanguage = currentLanguage,
                        onProductClick = { onProductClick(product) },
                        onAddToCart = { onAddToCart(product) },
                        onLikeClick = { onLikeClick(product) },
                        onHeartClick = { onHeartClick(product) },
                        onReviewClick = { onReviewClick(product) },
                        onCompareClick = { onToggleCompare(product) },
                        isCompared = isProductCompared(product.id)
                    )
                }
            }
        }
    }
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = AppStrings.get("filter_by_price", currentLanguage),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                val currentMax = maxPriceFilter ?: 12000.0
                Text(
                    text = "${AppStrings.get("max_price", currentLanguage)}: $${currentMax.toInt()}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Slider(
                    value = currentMax.toFloat(),
                    onValueChange = { onMaxPriceSelect(it.toDouble()) },
                    valueRange = 100f..12000f,
                    colors = SliderDefaults.colors(
                        thumbColor = GoldPrimary,
                        activeTrackColor = GoldPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        onMaxPriceSelect(null)
                        showFilterSheet = false
                    }) {
                        Text("Reset / ئەسلىگە كەلتۈرۈش", color = MaterialTheme.colorScheme.onSurface)
                    }

                    Button(
                        onClick = { showFilterSheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                    ) {
                        Text("OK / جەزملەش", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

