package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.firebase.GoogleAuthHelper
import com.example.data.firebase.StorePreferences
import com.example.data.model.Product
import com.example.data.repository.CatalogRepository
import com.example.data.repository.OrderRepository
import com.example.data.repository.UserShopRepository
import com.example.localization.Strings
import com.example.ui.components.WhatsAppSupportFab
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CartScreen
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.CheckoutScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.OrdersAndTrackingScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.WishlistScreen
import com.example.ui.theme.BestShopingTheme
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.ShopViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

private enum class MainTab { HOME, SHOP, CART, WISHLIST, PROFILE }

private sealed interface SubRoute {
    data object None : SubRoute
    data class ProductDetail(val productId: String) : SubRoute
    data object Checkout : SubRoute
    data class Orders(val selectedOrderId: String? = null) : SubRoute
    data object Notifications : SubRoute
    data object Admin : SubRoute
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val prefs = remember { StorePreferences(context.applicationContext) }
            val scope = rememberCoroutineScope()

            val onboardingDone by prefs.hasCompletedOnboarding.collectAsStateWithLifecycle(initialValue = false)
            val langCode by prefs.languageCode.collectAsStateWithLifecycle(initialValue = "en")
            val themeKey by prefs.themeMode.collectAsStateWithLifecycle(initialValue = "system")

            val strings = Strings.forLang(langCode)
            val themeMode = ThemeMode.fromKey(themeKey)

            var splashFinished by rememberSaveable { mutableStateOf(false) }

            BestShopingTheme(themeMode = themeMode) {
                var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
                DisposableEffect(Unit) {
                    val listener = FirebaseAuth.AuthStateListener { auth ->
                        currentUser = auth.currentUser
                    }
                    Firebase.auth.addAuthStateListener(listener)
                    onDispose { Firebase.auth.removeAuthStateListener(listener) }
                }

                when {
                    !splashFinished -> {
                        SplashScreen(onSplashFinished = { splashFinished = true })
                    }
                    !onboardingDone -> {
                        OnboardingScreen(
                            strings = strings,
                            onToggleLanguage = {
                                scope.launch { prefs.setLanguage(if (langCode == "bn") "en" else "bn") }
                            },
                            onFinishOnboarding = {
                                scope.launch { prefs.setOnboardingDone(true) }
                            }
                        )
                    }
                    currentUser == null -> {
                        AuthScreen(
                            strings = strings,
                            onToggleLanguage = {
                                scope.launch { prefs.setLanguage(if (langCode == "bn") "en" else "bn") }
                            },
                            onAuthSuccess = {
                                currentUser = Firebase.auth.currentUser
                            }
                        )
                    }
                    else -> {
                        val user = currentUser!!
                        key(user.uid) {
                            AuthenticatedShopApp(
                                userId = user.uid,
                                userEmail = user.email,
                                userName = user.displayName,
                                userPhotoUrl = user.photoUrl?.toString(),
                                langCode = langCode,
                                themeMode = themeMode,
                                prefs = prefs
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedShopApp(
    userId: String,
    userEmail: String?,
    userName: String?,
    userPhotoUrl: String?,
    langCode: String,
    themeMode: ThemeMode,
    prefs: StorePreferences
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: ShopViewModel = viewModel(
        key = userId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val auth = Firebase.auth
                ShopViewModel(
                    catalogRepository = CatalogRepository(db, auth),
                    userShopRepository = UserShopRepository(db, auth),
                    orderRepository = OrderRepository(db, auth),
                    storePreferences = prefs,
                    currentUserId = userId,
                    userEmail = userEmail,
                    userDisplayName = userName,
                    userAvatarUrl = userPhotoUrl
                )
            }
        }
    )

    val strings = Strings.forLang(langCode)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val productsState by viewModel.productsState.collectAsStateWithLifecycle()
    val categoriesState by viewModel.categoriesState.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val storeSettings by viewModel.storeSettings.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val addresses by viewModel.addresses.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val wishlistItems by viewModel.wishlistItems.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val customerOrders by viewModel.customerOrders.collectAsStateWithLifecycle()
    val customerPayments by viewModel.customerPayments.collectAsStateWithLifecycle()
    val coupons by viewModel.coupons.collectAsStateWithLifecycle()
    val appliedCoupon by viewModel.appliedCoupon.collectAsStateWithLifecycle()
    val recentlyViewedIds by viewModel.recentlyViewedIds.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val selectedBrand by viewModel.selectedBrand.collectAsStateWithLifecycle()
    val onlyDiscounted by viewModel.onlyDiscounted.collectAsStateWithLifecycle()
    val maxPriceFilter by viewModel.maxPriceFilter.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()

    val adminAllOrders by viewModel.adminAllOrders.collectAsStateWithLifecycle()
    val adminAllPayments by viewModel.adminAllPayments.collectAsStateWithLifecycle()
    val adminAllCustomers by viewModel.adminAllCustomers.collectAsStateWithLifecycle()

    val products = (productsState as? UiState.Success)?.data ?: emptyList()
    val categories = (categoriesState as? UiState.Success)?.data ?: emptyList()
    val wishlistIdSet = remember(wishlistItems) { wishlistItems.map { it.productId }.toSet() }
    val cartCount = remember(cartItems) { cartItems.sumOf { it.quantity }.toInt() }
    val unreadNotifs = remember(notifications) { notifications.count { !it.isRead } }

    var activeTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var subRoute by remember { mutableStateOf<SubRoute>(SubRoute.None) }

    if (subRoute != SubRoute.None) {
        BackHandler { subRoute = SubRoute.None }
    } else if (activeTab != MainTab.HOME) {
        BackHandler { activeTab = MainTab.HOME }
    }

    val openProduct: (Product) -> Unit = { p ->
        subRoute = SubRoute.ProductDetail(p.id)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                if (subRoute == SubRoute.None && activeTab == MainTab.HOME) {
                    WhatsAppSupportFab(
                        phoneNumber = storeSettings.whatsappNumber,
                        onUnavailable = { fallbackMsg ->
                            scope.launch { snackbarHostState.showSnackbar(fallbackMsg) }
                        }
                    )
                }
            },
            bottomBar = {
                if (subRoute == SubRoute.None && !useRail) {
                    NavigationBar {
                        MainTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            val label = when (tab) {
                                MainTab.HOME -> strings.home
                                MainTab.SHOP -> strings.shop
                                MainTab.CART -> strings.cart
                                MainTab.WISHLIST -> strings.wishlist
                                MainTab.PROFILE -> strings.profile
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                label = { Text(label) },
                                icon = {
                                    val iconVec = when (tab) {
                                        MainTab.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
                                        MainTab.SHOP -> if (selected) Icons.Filled.Storefront else Icons.Outlined.Storefront
                                        MainTab.CART -> if (selected) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart
                                        MainTab.WISHLIST -> if (selected) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
                                        MainTab.PROFILE -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
                                    }
                                    if (tab == MainTab.CART && cartCount > 0) {
                                        BadgedBox(badge = { Badge { Text("$cartCount") } }) {
                                            Icon(iconVec, contentDescription = label)
                                        }
                                    } else {
                                        Icon(iconVec, contentDescription = label)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (subRoute == SubRoute.None && useRail) {
                    NavigationRail {
                        MainTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            val label = when (tab) {
                                MainTab.HOME -> strings.home
                                MainTab.SHOP -> strings.shop
                                MainTab.CART -> strings.cart
                                MainTab.WISHLIST -> strings.wishlist
                                MainTab.PROFILE -> strings.profile
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                label = { Text(label) },
                                icon = {
                                    val iconVec = when (tab) {
                                        MainTab.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
                                        MainTab.SHOP -> if (selected) Icons.Filled.Storefront else Icons.Outlined.Storefront
                                        MainTab.CART -> if (selected) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart
                                        MainTab.WISHLIST -> if (selected) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
                                        MainTab.PROFILE -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
                                    }
                                    Icon(iconVec, contentDescription = label)
                                }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (val route = subRoute) {
                        is SubRoute.ProductDetail -> {
                            val prod = products.firstOrNull { it.id == route.productId }
                            if (prod == null) {
                                subRoute = SubRoute.None
                            } else {
                                val reviews by viewModel.observeReviewsFor(prod.id)
                                    .collectAsStateWithLifecycle(initialValue = emptyList())
                                val related = remember(products, prod.id) {
                                    products.filter { it.id != prod.id && it.categoryId == prod.categoryId }
                                }
                                ProductDetailScreen(
                                    product = prod,
                                    relatedProducts = related,
                                    reviews = reviews,
                                    wishlistIds = wishlistIdSet,
                                    strings = strings,
                                    onViewed = { viewModel.recordProductViewed(prod.id) },
                                    onBack = { subRoute = SubRoute.None },
                                    onAddToCart = { size, color, qty ->
                                        viewModel.addToCart(prod, size, color, qty)
                                    },
                                    onBuyNow = { size, color, qty ->
                                        viewModel.addToCart(prod, size, color, qty) {
                                            subRoute = SubRoute.Checkout
                                        }
                                    },
                                    onToggleWishlist = { viewModel.toggleWishlist(it) },
                                    onSubmitReview = { rating, comment ->
                                        viewModel.submitReview(prod.id, rating, comment)
                                    },
                                    onOpenProduct = { openProduct(it) }
                                )
                            }
                        }

                        is SubRoute.Checkout -> {
                            val subtotal = cartItems.sumOf { it.unitPrice * it.quantity }
                            CheckoutScreen(
                                strings = strings,
                                userProfile = userProfile,
                                savedAddresses = addresses,
                                cartItems = cartItems,
                                storeSettings = storeSettings,
                                couponDiscount = viewModel.calculateCouponDiscount(subtotal),
                                calculateDelivery = { dist, sub -> viewModel.calculateDeliveryCharge(dist, sub) },
                                onBack = { subRoute = SubRoute.None },
                                onConfirmOrder = { name, phone, email, dist, upazila, area, addr, notes, method, trx ->
                                    viewModel.placeOrder(
                                        customerName = name,
                                        customerPhone = phone,
                                        customerEmail = email,
                                        district = dist,
                                        upazila = upazila,
                                        area = area,
                                        fullAddress = addr,
                                        orderNotes = notes,
                                        paymentMethod = method,
                                        transactionId = trx,
                                        onOrderSuccess = { orderId ->
                                            activeTab = MainTab.PROFILE
                                            subRoute = SubRoute.Orders(selectedOrderId = orderId)
                                        }
                                    )
                                }
                            )
                        }

                        is SubRoute.Orders -> {
                            OrdersAndTrackingScreen(
                                strings = strings,
                                orders = customerOrders,
                                initialSelectedOrderId = route.selectedOrderId,
                                onBack = { subRoute = SubRoute.None },
                                onCancelOrder = { viewModel.cancelOrder(it) }
                            )
                        }

                        is SubRoute.Notifications -> {
                            NotificationsScreen(
                                strings = strings,
                                notifications = notifications,
                                userProfile = userProfile,
                                onBack = { subRoute = SubRoute.None },
                                onMarkRead = { viewModel.markNotificationRead(it) },
                                onMarkAllRead = { viewModel.markAllNotificationsRead() },
                                onOpenOrder = { orderId -> subRoute = SubRoute.Orders(orderId) },
                                onUpdateNotifSettings = { orderAlerts, promoAlerts ->
                                    val p = userProfile
                                    viewModel.saveProfile(
                                        p?.fullName ?: userName ?: "Shopper",
                                        p?.phone ?: "",
                                        p?.email ?: userEmail ?: "",
                                        p?.avatarUrl ?: "",
                                        langCode,
                                        orderAlerts,
                                        promoAlerts
                                    )
                                }
                            )
                        }

                        is SubRoute.Admin -> {
                            if (viewModel.isAdmin) {
                                AdminDashboardScreen(
                                    products = products,
                                    categories = categories,
                                    orders = adminAllOrders,
                                    payments = adminAllPayments,
                                    customers = adminAllCustomers,
                                    coupons = coupons,
                                    storeSettings = storeSettings,
                                    onBack = { subRoute = SubRoute.None },
                                    onSeedDefaultCatalog = { viewModel.adminSeedDefaultCatalog() },
                                    onSaveProduct = { viewModel.adminSaveProduct(it) },
                                    onDeleteProduct = { viewModel.adminDeleteProduct(it) },
                                    onSaveCategory = { viewModel.adminSaveCategory(it) },
                                    onUpdateOrderStatus = { order, st, note, eta ->
                                        viewModel.adminUpdateOrderStatus(order, st, note, eta)
                                    },
                                    onVerifyPayment = { pay, st, note ->
                                        viewModel.adminVerifyPayment(pay, st, note)
                                    },
                                    onSaveCoupon = { viewModel.adminSaveCoupon(it) },
                                    onSaveSettings = { viewModel.adminSaveSettings(it) }
                                )
                            } else {
                                subRoute = SubRoute.None
                            }
                        }

                        SubRoute.None -> {
                            when (activeTab) {
                                MainTab.HOME -> HomeScreen(
                                    strings = strings,
                                    productsState = productsState,
                                    categoriesState = categoriesState,
                                    storeSettings = storeSettings,
                                    wishlistIds = wishlistIdSet,
                                    recentlyViewedIds = recentlyViewedIds,
                                    cartCount = cartCount,
                                    unreadNotifCount = unreadNotifs,
                                    isAdmin = viewModel.isAdmin,
                                    onToggleLanguage = {
                                        viewModel.setLanguage(if (langCode == "bn") "en" else "bn")
                                    },
                                    onOpenSearch = { activeTab = MainTab.SHOP },
                                    onOpenNotifications = { subRoute = SubRoute.Notifications },
                                    onOpenCart = { activeTab = MainTab.CART },
                                    onOpenProfile = { activeTab = MainTab.PROFILE },
                                    onSelectCategory = { catId ->
                                        viewModel.setSelectedCategory(catId)
                                        activeTab = MainTab.SHOP
                                    },
                                    onOpenProduct = openProduct,
                                    onAddToCart = { viewModel.addToCart(it) },
                                    onToggleWishlist = { viewModel.toggleWishlist(it) },
                                    onSeeAllCatalog = { activeTab = MainTab.SHOP },
                                    onSeedCatalogIfAdmin = { viewModel.adminSeedDefaultCatalog() }
                                )

                                MainTab.SHOP -> CatalogScreen(
                                    strings = strings,
                                    products = filteredProducts,
                                    allBrands = products.map { it.brand }.distinct().sorted(),
                                    categories = categories,
                                    wishlistIds = wishlistIdSet,
                                    searchQuery = searchQuery,
                                    selectedCategoryId = selectedCategoryId,
                                    selectedBrand = selectedBrand,
                                    onlyDiscounted = onlyDiscounted,
                                    maxPrice = maxPriceFilter,
                                    sortOption = sortOption,
                                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                    onCategorySelected = { viewModel.setSelectedCategory(it) },
                                    onBrandSelected = { viewModel.setSelectedBrand(it) },
                                    onToggleDiscounted = { viewModel.setOnlyDiscounted(it) },
                                    onMaxPriceChange = { viewModel.setMaxPriceFilter(it) },
                                    onSortChange = { viewModel.setSortOption(it) },
                                    onClearFilters = { viewModel.clearCatalogFilters() },
                                    onOpenProduct = openProduct,
                                    onAddToCart = { viewModel.addToCart(it) },
                                    onToggleWishlist = { viewModel.toggleWishlist(it) }
                                )

                                MainTab.CART -> {
                                    val subtotal = cartItems.sumOf { it.unitPrice * it.quantity }
                                    val defaultDist = addresses.firstOrNull { it.isDefault }?.district ?: "Gaibandha"
                                    CartScreen(
                                        strings = strings,
                                        cartItems = cartItems,
                                        availableCoupons = coupons,
                                        appliedCoupon = appliedCoupon,
                                        couponDiscount = viewModel.calculateCouponDiscount(subtotal),
                                        estimatedDeliveryCharge = viewModel.calculateDeliveryCharge(defaultDist, subtotal),
                                        onUpdateQuantity = { id, qty -> viewModel.updateCartQuantity(id, qty) },
                                        onRemoveItem = { viewModel.removeFromCart(it) },
                                        onApplyCoupon = { code, sub -> viewModel.applyCoupon(code, sub) },
                                        onClearCoupon = { viewModel.clearCoupon() },
                                        onContinueShopping = { activeTab = MainTab.SHOP },
                                        onProceedToCheckout = { subRoute = SubRoute.Checkout }
                                    )
                                }

                                MainTab.WISHLIST -> WishlistScreen(
                                    strings = strings,
                                    wishlistItems = wishlistItems,
                                    products = products,
                                    onOpenProduct = openProduct,
                                    onMoveToCart = { viewModel.moveWishlistItemToCart(it, products) },
                                    onRemoveFromWishlist = { viewModel.toggleWishlist(it) },
                                    onBrowseShop = { activeTab = MainTab.SHOP }
                                )

                                MainTab.PROFILE -> ProfileScreen(
                                    strings = strings,
                                    userProfile = userProfile,
                                    addresses = addresses,
                                    payments = customerPayments,
                                    orderCount = customerOrders.size,
                                    themeMode = themeMode,
                                    isAdmin = viewModel.isAdmin,
                                    onSaveProfile = { name, phone, email, avatar, lang ->
                                        val p = userProfile
                                        viewModel.saveProfile(
                                            name, phone, email, avatar, lang,
                                            p?.notificationsEnabled ?: true,
                                            p?.promoNotificationsEnabled ?: true
                                        )
                                    },
                                    onSaveAddress = { viewModel.saveAddress(it) },
                                    onDeleteAddress = { viewModel.deleteAddress(it) },
                                    onThemeModeChange = { viewModel.setThemeMode(it.key) },
                                    onOpenOrders = { subRoute = SubRoute.Orders() },
                                    onOpenNotifications = { subRoute = SubRoute.Notifications },
                                    onOpenAdminDashboard = { subRoute = SubRoute.Admin },
                                    onSignOut = {
                                        GoogleAuthHelper.signOut(credentialManager, scope)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
