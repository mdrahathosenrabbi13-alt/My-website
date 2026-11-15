package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.localization.AppStrings
import com.example.localization.formatBdt
import com.example.ui.components.ProductCard
import com.example.ui.viewmodel.SortOption

@Composable
fun CatalogScreen(
    strings: AppStrings,
    products: List<Product>,
    allBrands: List<String>,
    categories: List<Category>,
    wishlistIds: Set<String>,
    searchQuery: String,
    selectedCategoryId: String?,
    selectedBrand: String?,
    onlyDiscounted: Boolean,
    maxPrice: Float,
    sortOption: SortOption,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onBrandSelected: (String?) -> Unit,
    onToggleDiscounted: (Boolean) -> Unit,
    onMaxPriceChange: (Float) -> Unit,
    onSortChange: (SortOption) -> Unit,
    onClearFilters: () -> Unit,
    onOpenProduct: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onToggleWishlist: (Product) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showFiltersPanel by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.shop,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(
                        onClick = { showFiltersPanel = !showFiltersPanel },
                        modifier = Modifier.testTag("catalog_filter_toggle")
                    ) {
                        Icon(Icons.Rounded.Tune, contentDescription = "Filters")
                    }
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("catalog_sort_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOption.entries.forEach { opt ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (strings.langCode == "bn") opt.labelBn else opt.labelEn,
                                            fontWeight = if (opt == sortOption) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        onSortChange(opt)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(strings.searchPlaceholder) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("catalog_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { onCategorySelected(null) },
                        label = { Text(if (strings.langCode == "bn") "সব" else "All") }
                    )
                }
                items(categories.filter { it.isActive }, key = { it.id }) { cat ->
                    val label = if (strings.langCode == "bn" && cat.nameBn.isNotBlank()) cat.nameBn else cat.name
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = {
                            onCategorySelected(if (selectedCategoryId == cat.id) null else cat.id)
                        },
                        label = { Text(label) }
                    )
                }
            }

            if (showFiltersPanel) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Max Price: ${formatBdt(maxPrice.toDouble())}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            TextButton(onClick = onClearFilters) {
                                Text("Reset")
                            }
                        }
                        Slider(
                            value = maxPrice,
                            onValueChange = onMaxPriceChange,
                            valueRange = 500f..50000f
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = onlyDiscounted,
                                    onClick = { onToggleDiscounted(!onlyDiscounted) },
                                    label = { Text("Discount Only") }
                                )
                            }
                            items(allBrands) { brand ->
                                FilterChip(
                                    selected = selectedBrand == brand,
                                    onClick = {
                                        onBrandSelected(if (selectedBrand == brand) null else brand)
                                    },
                                    label = { Text(brand) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (strings.langCode == "bn") "কোনো পণ্য পাওয়া যায়নি" else "No matching products",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onClearFilters) {
                        Text(if (strings.langCode == "bn") "ফিল্টার মুছুন" else "Clear All Filters")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(164.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        langCode = strings.langCode,
                        isWishlisted = wishlistIds.contains(product.id),
                        onProductClick = { onOpenProduct(product) },
                        onAddToCart = { onAddToCart(product) },
                        onToggleWishlist = { onToggleWishlist(product) }
                    )
                }
            }
        }
    }
}
