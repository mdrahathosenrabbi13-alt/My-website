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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.data.model.ProductReview
import com.example.localization.AppStrings
import com.example.localization.formatBdt
import com.example.ui.components.ProductCard
import com.example.ui.components.ProductImage
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    product: Product,
    relatedProducts: List<Product>,
    reviews: List<ProductReview>,
    wishlistIds: Set<String>,
    strings: AppStrings,
    onViewed: () -> Unit,
    onBack: () -> Unit,
    onAddToCart: (size: String, color: String, qty: Long) -> Unit,
    onBuyNow: (size: String, color: String, qty: Long) -> Unit,
    onToggleWishlist: (Product) -> Unit,
    onSubmitReview: (rating: Long, comment: String) -> Unit,
    onOpenProduct: (Product) -> Unit
) {
    LaunchedEffect(product.id) { onViewed() }

    val displayName = if (strings.langCode == "bn" && product.nameBn.isNotBlank()) product.nameBn else product.name
    val displayDesc = if (strings.langCode == "bn" && product.descriptionBn.isNotBlank()) product.descriptionBn else product.description
    val isWishlisted = wishlistIds.contains(product.id)

    var selectedSize by remember(product.id) {
        mutableStateOf(product.sizes.firstOrNull() ?: "Standard")
    }
    var selectedColor by remember(product.id) {
        mutableStateOf(product.colors.firstOrNull() ?: "Default")
    }
    var quantity by remember(product.id) { mutableLongStateOf(1L) }

    var reviewRating by remember { mutableLongStateOf(5L) }
    var reviewComment by remember { mutableStateOf("") }

    // Combine imageUrls and imageResNames for swipeable gallery
    val galleryItems: List<Pair<String, String>> = remember(product.id) {
        val urls = product.imageUrls.filter { it.isNotBlank() }.map { "" to it }
        val res = product.imageResNames.filter { it.isNotBlank() }.map { it to "" }
        (urls + res).ifEmpty { listOf("" to "") }
    }
    val pagerState = rememberPagerState(pageCount = { galleryItems.size })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product.brand, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("product_detail_back")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onToggleWishlist(product) },
                        modifier = Modifier.testTag("product_detail_wishlist")
                    ) {
                        Icon(
                            imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Wishlist",
                            tint = if (isWishlisted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onAddToCart(selectedSize, selectedColor, quantity) },
                        enabled = product.stock > 0L,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("detail_add_to_cart_btn")
                    ) {
                        Text(strings.addToCart, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onBuyNow(selectedSize, selectedColor, quantity) },
                        enabled = product.stock > 0L,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("detail_buy_now_btn")
                    ) {
                        Text(strings.buyNow, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Image Gallery
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { idx ->
                        val (resName, url) = galleryItems[idx]
                        ProductImage(
                            imageResName = resName,
                            imageUrl = url,
                            contentDescription = displayName,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (product.discountPercent > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "-${product.discountPercent}% OFF",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    if (galleryItems.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeat(galleryItems.size) { i ->
                                val active = pagerState.currentPage == i
                                Box(
                                    modifier = Modifier
                                        .size(if (active) 10.dp else 7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (active) MaterialTheme.colorScheme.primary
                                            else Color.White.copy(alpha = 0.65f)
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // Title, Pricing, Stock
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${product.categoryName} • ${product.brand}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            color = if (product.stock > 0) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (product.stock > 0) "${strings.inStock} (${product.stock})" else strings.outOfStock,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (product.stock > 0) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.1f", product.rating)} (${product.reviewCount} reviews)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatBdt(product.effectivePrice),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (product.discountPercent > 0) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = formatBdt(product.regularPrice),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                    }

                    // Size selector
                    if (product.sizes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (strings.langCode == "bn") "সাইজ নির্বাচন করুন" else "Select Size",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(product.sizes) { sz ->
                                FilterChip(
                                    selected = selectedSize == sz,
                                    onClick = { selectedSize = sz },
                                    label = { Text(sz) }
                                )
                            }
                        }
                    }

                    // Color selector
                    if (product.colors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (strings.langCode == "bn") "রঙ নির্বাচন করুন" else "Select Color",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(product.colors) { clr ->
                                FilterChip(
                                    selected = selectedColor == clr,
                                    onClick = { selectedColor = clr },
                                    label = { Text(clr) }
                                )
                            }
                        }
                    }

                    // Quantity selector
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (strings.langCode == "bn") "পরিমাণ" else "Quantity",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (quantity > 1L) quantity-- },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease quantity")
                                }
                                Text(
                                    text = "$quantity",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                IconButton(
                                    onClick = { if (quantity < product.stock.coerceAtMost(99L)) quantity++ },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Increase quantity")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (strings.langCode == "bn") "পণ্যের বিবরণ" else "Description",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = displayDesc,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Customer Reviews & Submit Review
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (strings.langCode == "bn") "গ্রাহকের রিভিউ (${reviews.size})" else "Customer Reviews (${reviews.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (strings.langCode == "bn") "রিভিউ লিখুন" else "Write a Review",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                (1L..5L).forEach { star ->
                                    IconButton(
                                        onClick = { reviewRating = star },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (star <= reviewRating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                            contentDescription = "$star stars",
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = reviewComment,
                                onValueChange = { reviewComment = it },
                                placeholder = { Text("Share your experience...") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    onSubmitReview(reviewRating, reviewComment)
                                    reviewComment = ""
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (strings.langCode == "bn") "রিভিউ পাঠান" else "Submit Review")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    reviews.forEach { rev ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = rev.reviewerName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "★ ${rev.rating}/5",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = rev.comment,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            // Related Products
            if (relatedProducts.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (strings.langCode == "bn") "সম্পর্কিত পণ্য" else "Related Products",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(relatedProducts, key = { it.id }) { rel ->
                            ProductCard(
                                product = rel,
                                langCode = strings.langCode,
                                isWishlisted = wishlistIds.contains(rel.id),
                                onProductClick = { onOpenProduct(rel) },
                                onAddToCart = { onAddToCart(rel.sizes.firstOrNull() ?: "Standard", rel.colors.firstOrNull() ?: "Default", 1L) },
                                onToggleWishlist = { onToggleWishlist(rel) },
                                modifier = Modifier.width(168.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
