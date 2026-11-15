package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.data.model.StoreSettings
import com.example.localization.AppStrings
import com.example.ui.components.ProductCard
import com.example.ui.components.resolveDrawableId
import com.example.ui.theme.GoldBright
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    strings: AppStrings,
    productsState: UiState<List<Product>>,
    categoriesState: UiState<List<Category>>,
    storeSettings: StoreSettings,
    wishlistIds: Set<String>,
    recentlyViewedIds: List<String>,
    cartCount: Int,
    unreadNotifCount: Int,
    isAdmin: Boolean,
    onToggleLanguage: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenCart: () -> Unit,
    onOpenProfile: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onOpenProduct: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onToggleWishlist: (Product) -> Unit,
    onSeeAllCatalog: () -> Unit,
    onSeedCatalogIfAdmin: () -> Unit
) {
    val products = (productsState as? UiState.Success)?.data ?: emptyList()
    val categories = (categoriesState as? UiState.Success)?.data ?: emptyList()

    // Flash sale countdown ticker
    var remainingSeconds by remember { mutableLongStateOf(5 * 3600L + 42 * 60L + 18L) }
    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        }
    }
    val hrs = remainingSeconds / 3600
    val mins = (remainingSeconds % 3600) / 60
    val secs = remainingSeconds % 60
    val countdownText = String.format("%02d:%02d:%02d", hrs, mins, secs)

    // Auto-scroll hero banner
    val banners = storeSettings.banners
    val pagerState = rememberPagerState(pageCount = { banners.size.coerceAtLeast(1) })
    LaunchedEffect(banners.size) {
        if (banners.size > 1) {
            while (true) {
                delay(4200)
                val next = (pagerState.currentPage + 1) % banners.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Brand Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_app_icon_fg),
                            contentDescription = strings.appName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = storeSettings.appName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = storeSettings.tagline,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onToggleLanguage)
                                .testTag("home_lang_toggle")
                        ) {
                            Text(
                                text = if (strings.langCode == "bn") "EN" else "বাং",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("home_notifications_btn")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotifCount > 0) {
                                        Badge { Text("$unreadNotifCount") }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Notifications,
                                    contentDescription = strings.notifications
                                )
                            }
                        }

                        IconButton(
                            onClick = onOpenCart,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("home_cart_btn")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (cartCount > 0) {
                                        Badge { Text("$cartCount") }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ShoppingCart,
                                    contentDescription = strings.cart
                                )
                            }
                        }

                        IconButton(
                            onClick = onOpenProfile,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("home_profile_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = strings.profile
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onOpenSearch)
                        .testTag("home_search_bar")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.searchPlaceholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Promo Coupon Ticker Strip
        item {
            val promoText = if (strings.langCode == "bn" && storeSettings.promoBannerTextBn.isNotBlank()) {
                storeSettings.promoBannerTextBn
            } else {
                storeSettings.promoBannerText
            }
            if (promoText.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalOffer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = promoText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Animated Promotional Hero Slider
        item {
            if (banners.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        pageSpacing = 12.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(196.dp)
                    ) { index ->
                        val banner = banners[index]
                        val resId = resolveDrawableId(banner.imageResName) ?: R.drawable.img_hero_fashion
                        val title = if (strings.langCode == "bn" && banner.titleBn.isNotBlank()) banner.titleBn else banner.title
                        val subtitle = if (strings.langCode == "bn" && banner.subtitleBn.isNotBlank()) banner.subtitleBn else banner.subtitle

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp))
                                .clickable(onClick = onSeeAllCatalog)
                        ) {
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.78f),
                                                Color.Black.copy(alpha = 0.25f)
                                            )
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    color = GoldBright,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = banner.badgeText.ifBlank { "OFFER" },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E1400),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.88f),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = onSeeAllCatalog,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .height(36.dp)
                                            .testTag("hero_shop_now_button")
                                    ) {
                                        Text(
                                            text = strings.shopNow,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(banners.size) { i ->
                            val selected = pagerState.currentPage == i
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .height(6.dp)
                                    .width(if (selected) 20.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Loading / Empty Catalog Handling
        if (productsState is UiState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (products.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (strings.langCode == "bn") "স্টোর ক্যাটালগ প্রস্তুত হচ্ছে" else "Storefront Catalog Initializing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isAdmin) "Tap below to seed Best Shoping BD starter products & categories into Firestore."
                            else "Please check back shortly or contact support at 01787962913.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isAdmin) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onSeedCatalogIfAdmin) {
                                Text("Seed Store Catalog Now")
                            }
                        }
                    }
                }
            }
        }

        // Categories Row
        if (categories.isNotEmpty()) {
            item {
                SectionHeader(
                    title = strings.categories,
                    actionText = strings.seeAll,
                    onAction = onSeeAllCatalog
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categories.filter { it.isActive }, key = { it.id }) { cat ->
                        val catTitle = if (strings.langCode == "bn" && cat.nameBn.isNotBlank()) cat.nameBn else cat.name
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            tonalElevation = 2.dp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onSelectCategory(cat.id) }
                                .testTag("home_category_${cat.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = categoryIcon(cat.iconName),
                                        contentDescription = catTitle,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = catTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Flash Sale Section
        val flashProducts = products.filter { it.isFlashSale }
        if (flashProducts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.flashSale,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = countdownText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    TextButton(onClick = onSeeAllCatalog) {
                        Text(strings.seeAll)
                    }
                }
                HorizontalProductStrip(
                    products = flashProducts,
                    langCode = strings.langCode,
                    wishlistIds = wishlistIds,
                    onOpenProduct = onOpenProduct,
                    onAddToCart = onAddToCart,
                    onToggleWishlist = onToggleWishlist
                )
            }
        }

        // Featured Products
        val featured = products.filter { it.isFeatured }
        if (featured.isNotEmpty()) {
            item {
                SectionHeader(strings.featuredProducts, strings.seeAll, onSeeAllCatalog)
                HorizontalProductStrip(
                    products = featured,
                    langCode = strings.langCode,
                    wishlistIds = wishlistIds,
                    onOpenProduct = onOpenProduct,
                    onAddToCart = onAddToCart,
                    onToggleWishlist = onToggleWishlist
                )
            }
        }

        // New Arrivals
        val arrivals = products.filter { it.isNewArrival }
        if (arrivals.isNotEmpty()) {
            item {
                SectionHeader(strings.newArrivals, strings.seeAll, onSeeAllCatalog)
                HorizontalProductStrip(
                    products = arrivals,
                    langCode = strings.langCode,
                    wishlistIds = wishlistIds,
                    onOpenProduct = onOpenProduct,
                    onAddToCart = onAddToCart,
                    onToggleWishlist = onToggleWishlist
                )
            }
        }

        // Best Sellers
        val bestSellers = products.filter { it.isBestSeller }
        if (bestSellers.isNotEmpty()) {
            item {
                SectionHeader(strings.bestSellers, strings.seeAll, onSeeAllCatalog)
                HorizontalProductStrip(
                    products = bestSellers,
                    langCode = strings.langCode,
                    wishlistIds = wishlistIds,
                    onOpenProduct = onOpenProduct,
                    onAddToCart = onAddToCart,
                    onToggleWishlist = onToggleWishlist
                )
            }
        }

        // Special Offers
        val offers = products.filter { it.discountPercent >= 20 }
        if (offers.isNotEmpty()) {
            item {
                SectionHeader(strings.specialOffers, strings.seeAll, onSeeAllCatalog)
                HorizontalProductStrip(
                    products = offers,
                    langCode = strings.langCode,
                    wishlistIds = wishlistIds,
                    onOpenProduct = onOpenProduct,
                    onAddToCart = onAddToCart,
                    onToggleWishlist = onToggleWishlist
                )
            }
        }

        // Recommended Products
        if (products.isNotEmpty()) {
            item {
                SectionHeader(strings.recommended, strings.seeAll, onSeeAllCatalog)
                HorizontalProductStrip(
                    products = products.sortedByDescending { it.rating },
                    langCode = strings.langCode,
                    wishlistIds = wishlistIds,
                    onOpenProduct = onOpenProduct,
                    onAddToCart = onAddToCart,
                    onToggleWishlist = onToggleWishlist
                )
            }
        }

        // Recently Viewed
        val recentlyViewed = recentlyViewedIds.mapNotNull { id -> products.firstOrNull { it.id == id } }
        if (recentlyViewed.isNotEmpty()) {
            item {
                SectionHeader(strings.recentlyViewed, strings.seeAll, onSeeAllCatalog)
                HorizontalProductStrip(
                    products = recentlyViewed,
                    langCode = strings.langCode,
                    wishlistIds = wishlistIds,
                    onOpenProduct = onOpenProduct,
                    onAddToCart = onAddToCart,
                    onToggleWishlist = onToggleWishlist
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        TextButton(onClick = onAction) {
            Text(text = actionText)
        }
    }
}

@Composable
private fun HorizontalProductStrip(
    products: List<Product>,
    langCode: String,
    wishlistIds: Set<String>,
    onOpenProduct: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onToggleWishlist: (Product) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(products, key = { it.id }) { product ->
            ProductCard(
                product = product,
                langCode = langCode,
                isWishlisted = wishlistIds.contains(product.id),
                onProductClick = { onOpenProduct(product) },
                onAddToCart = { onAddToCart(product) },
                onToggleWishlist = { onToggleWishlist(product) },
                modifier = Modifier.width(172.dp)
            )
        }
    }
}

private fun categoryIcon(name: String): ImageVector = when (name) {
    "checkroom" -> Icons.Rounded.Checkroom
    "devices" -> Icons.Rounded.Devices
    "spa" -> Icons.Rounded.Spa
    "directions_run" -> Icons.AutoMirrored.Rounded.DirectionsRun
    "watch" -> Icons.Rounded.Watch
    else -> Icons.Rounded.LocalOffer
}
