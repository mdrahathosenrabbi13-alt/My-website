package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.StorePreferences
import com.example.data.firebase.unwrapFirestoreException
import com.example.data.model.CartItem
import com.example.data.model.Category
import com.example.data.model.Coupon
import com.example.data.model.DeliveryAddress
import com.example.data.model.Order
import com.example.data.model.PaymentRecord
import com.example.data.model.Product
import com.example.data.model.ProductReview
import com.example.data.model.StoreSettings
import com.example.data.model.UserNotification
import com.example.data.model.UserProfile
import com.example.data.model.WishlistItem
import com.example.data.repository.CatalogRepository
import com.example.data.repository.OrderRepository
import com.example.data.repository.UserShopRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class SortOption(val labelEn: String, val labelBn: String) {
    LATEST("Latest", "সর্বশেষ"),
    POPULARITY("Popularity", "জনপ্রিয়তা"),
    PRICE_LOW("Price: Low to High", "দাম: কম থেকে বেশি"),
    PRICE_HIGH("Price: High to Low", "দাম: বেশি থেকে কম")
}

class ShopViewModel(
    private val catalogRepository: CatalogRepository,
    private val userShopRepository: UserShopRepository,
    private val orderRepository: OrderRepository,
    private val storePreferences: StorePreferences,
    val currentUserId: String,
    val userEmail: String?,
    private val userDisplayName: String?,
    private val userAvatarUrl: String?
) : ViewModel() {

    val isAdmin: Boolean = userEmail.equals(ADMIN_EMAIL, ignoreCase = true)

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // Catalog filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _selectedBrand = MutableStateFlow<String?>(null)
    val selectedBrand: StateFlow<String?> = _selectedBrand.asStateFlow()

    private val _onlyDiscounted = MutableStateFlow(false)
    val onlyDiscounted: StateFlow<Boolean> = _onlyDiscounted.asStateFlow()

    private val _maxPriceFilter = MutableStateFlow(50000f)
    val maxPriceFilter: StateFlow<Float> = _maxPriceFilter.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.LATEST)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    // Coupon state
    private val _appliedCoupon = MutableStateFlow<Coupon?>(null)
    val appliedCoupon: StateFlow<Coupon?> = _appliedCoupon.asStateFlow()

    // Live Firestore streams
    val categoriesState: StateFlow<UiState<List<Category>>> =
        catalogRepository.observeCategories().asUiState("categories")

    val productsState: StateFlow<UiState<List<Product>>> =
        catalogRepository.observeProducts().asUiState("products")

    val coupons: StateFlow<List<Coupon>> =
        catalogRepository.observeCoupons()
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

    val storeSettings: StateFlow<StoreSettings> =
        catalogRepository.observeStoreSettings()
            .catch { emit(StoreSettings()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), StoreSettings())

    val userProfile: StateFlow<UserProfile?> =
        userShopRepository.observeUserProfile(currentUserId)
            .catch { emit(null) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), null)

    val addresses: StateFlow<List<DeliveryAddress>> =
        userShopRepository.observeAddresses(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

    val cartItems: StateFlow<List<CartItem>> =
        userShopRepository.observeCart(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

    val wishlistItems: StateFlow<List<WishlistItem>> =
        userShopRepository.observeWishlist(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

    val notifications: StateFlow<List<UserNotification>> =
        userShopRepository.observeNotifications(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

    val customerOrders: StateFlow<List<Order>> =
        orderRepository.observeCustomerOrders(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

    val customerPayments: StateFlow<List<PaymentRecord>> =
        orderRepository.observeCustomerPayments(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

    val recentlyViewedIds: StateFlow<List<String>> =
        storePreferences.recentlyViewedIds
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

    // Admin streams (strictly gated so non-admin users never attach admin queries)
    val adminAllOrders: StateFlow<List<Order>> =
        if (isAdmin) {
            orderRepository.observeAllOrders()
                .catch { emit(emptyList()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())
        } else {
            flowOf(emptyList<Order>()).stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_MS),
                emptyList()
            )
        }

    val adminAllPayments: StateFlow<List<PaymentRecord>> =
        if (isAdmin) {
            orderRepository.observeAllPayments()
                .catch { emit(emptyList()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())
        } else {
            flowOf(emptyList<PaymentRecord>()).stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_MS),
                emptyList()
            )
        }

    val adminAllCustomers: StateFlow<List<UserProfile>> =
        if (isAdmin) {
            userShopRepository.observeAllCustomers()
                .catch { emit(emptyList()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())
        } else {
            flowOf(emptyList<UserProfile>()).stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_MS),
                emptyList()
            )
        }

    // Filtered products for Catalog screen
    val filteredProducts: StateFlow<List<Product>> = combine(
        productsState,
        searchQuery,
        selectedCategoryId,
        selectedBrand,
        onlyDiscounted,
        maxPriceFilter,
        sortOption
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val state = values[0] as UiState<List<Product>>
        val query = (values[1] as String).trim()
        val catId = values[2] as String?
        val brand = values[3] as String?
        val discounted = values[4] as Boolean
        val maxPrice = values[5] as Float
        val sort = values[6] as SortOption

        val base = (state as? UiState.Success)?.data ?: emptyList()
        val filtered = base.filter { p ->
            val matchesQuery = query.isBlank() ||
                p.name.contains(query, ignoreCase = true) ||
                p.nameBn.contains(query, ignoreCase = true) ||
                p.brand.contains(query, ignoreCase = true) ||
                p.categoryName.contains(query, ignoreCase = true)
            val matchesCat = catId == null || p.categoryId == catId
            val matchesBrand = brand == null || p.brand.equals(brand, ignoreCase = true)
            val matchesDiscount = !discounted || p.discountPercent > 0
            val matchesPrice = p.effectivePrice <= maxPrice.toDouble()
            matchesQuery && matchesCat && matchesBrand && matchesDiscount && matchesPrice
        }
        when (sort) {
            SortOption.LATEST -> filtered.sortedByDescending { it.createdAt }
            SortOption.POPULARITY -> filtered.sortedByDescending { it.reviewCount + (if (it.isBestSeller) 100 else 0) }
            SortOption.PRICE_LOW -> filtered.sortedBy { it.effectivePrice }
            SortOption.PRICE_HIGH -> filtered.sortedByDescending { it.effectivePrice }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

    init {
        viewModelScope.launch {
            userShopRepository.ensureUserProfileExists(
                displayName = userDisplayName,
                email = userEmail,
                avatarUrl = userAvatarUrl
            )
            if (isAdmin) {
                catalogRepository.seedInitialStorefrontIfNeeded()
            }
        }
    }

    private fun <T> Flow<List<T>>.asUiState(name: String): StateFlow<UiState<List<T>>> =
        this.map<List<T>, UiState<List<T>>> { UiState.Success(it) }
            .catch { error ->
                val fsError = error.unwrapFirestoreException()
                if (fsError == null && error is CancellationException) throw error
                val root = fsError ?: error
                Log.w(TAG, "Error observing $name", root)
                emit(UiState.Error(root.message ?: "Failed to load $name"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_MS),
                initialValue = UiState.Loading
            )

    fun observeReviewsFor(productId: String): Flow<List<ProductReview>> =
        catalogRepository.observeProductReviews(productId).catch { emit(emptyList()) }

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setSelectedCategory(catId: String?) { _selectedCategoryId.value = catId }
    fun setSelectedBrand(brand: String?) { _selectedBrand.value = brand }
    fun setOnlyDiscounted(only: Boolean) { _onlyDiscounted.value = only }
    fun setMaxPriceFilter(max: Float) { _maxPriceFilter.value = max }
    fun setSortOption(opt: SortOption) { _sortOption.value = opt }

    fun clearCatalogFilters() {
        _searchQuery.value = ""
        _selectedCategoryId.value = null
        _selectedBrand.value = null
        _onlyDiscounted.value = false
        _maxPriceFilter.value = 50000f
        _sortOption.value = SortOption.LATEST
    }

    fun recordProductViewed(productId: String) {
        viewModelScope.launch {
            storePreferences.addRecentlyViewed(productId)
        }
    }

    fun addToCart(
        product: Product,
        size: String = product.sizes.firstOrNull() ?: "Standard",
        color: String = product.colors.firstOrNull() ?: "Default",
        quantity: Long = 1L,
        onComplete: () -> Unit = {}
    ) {
        if (product.stock <= 0L) {
            _snackbarMessage.tryEmit("Out of stock")
            return
        }
        viewModelScope.launch {
            userShopRepository.addToCart(product, size, color, quantity)
                .onSuccess {
                    _snackbarMessage.tryEmit("Added ${product.name} to cart")
                    onComplete()
                }
                .onFailure { _snackbarMessage.tryEmit(it.message ?: "Failed to add to cart") }
        }
    }

    fun updateCartQuantity(cartItemId: String, quantity: Long) {
        viewModelScope.launch {
            userShopRepository.updateCartQuantity(cartItemId, quantity)
                .onFailure { _snackbarMessage.tryEmit(it.message ?: "Could not update cart") }
        }
    }

    fun removeFromCart(cartItemId: String) {
        viewModelScope.launch {
            userShopRepository.removeFromCart(cartItemId)
        }
    }

    fun toggleWishlist(product: Product) {
        viewModelScope.launch {
            userShopRepository.toggleWishlist(product)
                .onSuccess { added ->
                    _snackbarMessage.tryEmit(
                        if (added) "Saved to wishlist" else "Removed from wishlist"
                    )
                }
                .onFailure { _snackbarMessage.tryEmit(it.message ?: "Wishlist update failed") }
        }
    }

    fun moveWishlistItemToCart(item: WishlistItem, products: List<Product>) {
        val prod = products.firstOrNull { it.id == item.productId }
        if (prod != null) {
            addToCart(prod)
        } else {
            _snackbarMessage.tryEmit("Product details unavailable")
        }
    }

    fun applyCoupon(codeInput: String, subtotal: Double) {
        val clean = codeInput.trim().uppercase()
        if (clean.isEmpty()) {
            _appliedCoupon.value = null
            return
        }
        val match = coupons.value.firstOrNull { it.code.equals(clean, ignoreCase = true) && it.isActive }
        if (match == null) {
            _snackbarMessage.tryEmit("Invalid or inactive coupon code")
            return
        }
        if (subtotal < match.minOrderBdt) {
            _snackbarMessage.tryEmit("Minimum order ৳${match.minOrderBdt.toLong()} required for ${match.code}")
            return
        }
        _appliedCoupon.value = match
        _snackbarMessage.tryEmit("Coupon ${match.code} applied (${match.discountPercent}% off)")
    }

    fun clearCoupon() {
        _appliedCoupon.value = null
    }

    fun calculateCouponDiscount(subtotal: Double): Double {
        val c = _appliedCoupon.value ?: return 0.0
        if (!c.isActive || subtotal < c.minOrderBdt) return 0.0
        val raw = subtotal * (c.discountPercent / 100.0)
        return if (c.maxDiscountBdt > 0.0) raw.coerceAtMost(c.maxDiscountBdt) else raw
    }

    fun calculateDeliveryCharge(district: String, subtotal: Double): Double {
        val settings = storeSettings.value
        if (settings.freeDeliveryThreshold > 0.0 && subtotal >= settings.freeDeliveryThreshold) {
            return 0.0
        }
        return if (district.equals("Dhaka", ignoreCase = true)) {
            settings.dhakaDeliveryCharge
        } else {
            settings.outsideDhakaDeliveryCharge
        }
    }

    fun submitReview(productId: String, rating: Long, comment: String) {
        if (comment.isBlank()) {
            _snackbarMessage.tryEmit("Please write a short review")
            return
        }
        val reviewer = userProfile.value?.fullName ?: userDisplayName ?: "Customer"
        viewModelScope.launch {
            catalogRepository.addReview(productId, reviewer, rating, comment)
                .onSuccess { _snackbarMessage.tryEmit("Thank you for your review!") }
                .onFailure { _snackbarMessage.tryEmit(it.message ?: "Could not submit review") }
        }
    }

    fun placeOrder(
        customerName: String,
        customerPhone: String,
        customerEmail: String,
        district: String,
        upazila: String,
        area: String,
        fullAddress: String,
        orderNotes: String,
        paymentMethod: String,
        transactionId: String,
        onOrderSuccess: (String) -> Unit
    ) {
        val items = cartItems.value
        if (items.isEmpty()) {
            _snackbarMessage.tryEmit("Your cart is empty")
            return
        }
        val subtotal = items.sumOf { it.unitPrice * it.quantity }
        val delivery = calculateDeliveryCharge(district, subtotal)
        val discount = calculateCouponDiscount(subtotal)
        val total = (subtotal - discount + delivery).coerceAtLeast(0.0)
        val couponCode = _appliedCoupon.value?.code ?: ""

        viewModelScope.launch {
            orderRepository.placeOrder(
                customerName = customerName,
                customerPhone = customerPhone,
                customerEmail = customerEmail,
                district = district,
                upazila = upazila,
                area = area,
                fullAddress = fullAddress,
                orderNotes = orderNotes,
                items = items,
                subtotal = subtotal,
                deliveryCharge = delivery,
                couponCode = couponCode,
                couponDiscount = discount,
                totalAmount = total,
                paymentMethod = paymentMethod,
                transactionId = transactionId
            ).onSuccess { orderId ->
                userShopRepository.clearCart()
                _appliedCoupon.value = null
                _snackbarMessage.tryEmit("Order placed successfully!")
                onOrderSuccess(orderId)
            }.onFailure {
                _snackbarMessage.tryEmit(it.message ?: "Failed to place order")
            }
        }
    }

    fun cancelOrder(order: Order) {
        viewModelScope.launch {
            orderRepository.cancelOrderByCustomer(order)
                .onSuccess { _snackbarMessage.tryEmit("Order cancelled") }
                .onFailure { _snackbarMessage.tryEmit(it.message ?: "Unable to cancel order") }
        }
    }

    fun saveProfile(
        fullName: String,
        phone: String,
        email: String,
        avatarUrl: String,
        preferredLanguage: String,
        notificationsEnabled: Boolean,
        promoNotificationsEnabled: Boolean
    ) {
        viewModelScope.launch {
            userShopRepository.updateProfile(
                fullName, phone, email, avatarUrl, preferredLanguage,
                notificationsEnabled, promoNotificationsEnabled
            ).onSuccess {
                storePreferences.setLanguage(preferredLanguage)
                _snackbarMessage.tryEmit("Profile saved")
            }.onFailure {
                _snackbarMessage.tryEmit(it.message ?: "Failed to save profile")
            }
        }
    }

    fun saveAddress(address: DeliveryAddress, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            userShopRepository.saveAddress(address)
                .onSuccess {
                    _snackbarMessage.tryEmit("Address saved")
                    onDone()
                }
                .onFailure { _snackbarMessage.tryEmit(it.message ?: "Could not save address") }
        }
    }

    fun deleteAddress(addressId: String) {
        viewModelScope.launch {
            userShopRepository.deleteAddress(addressId)
                .onSuccess { _snackbarMessage.tryEmit("Address removed") }
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch { userShopRepository.markNotificationRead(id) }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch { userShopRepository.markAllNotificationsRead() }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            storePreferences.setLanguage(lang)
            userProfile.value?.let { p ->
                userShopRepository.updateProfile(
                    p.fullName, p.phone, p.email, p.avatarUrl, lang,
                    p.notificationsEnabled, p.promoNotificationsEnabled
                )
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { storePreferences.setThemeMode(mode) }
    }

    // Admin actions
    fun adminSeedDefaultCatalog() {
        viewModelScope.launch {
            catalogRepository.seedInitialStorefrontIfNeeded()
                .onSuccess { seeded ->
                    _snackbarMessage.tryEmit(
                        if (seeded) "Default Best Shoping BD catalog seeded!" else "Catalog already populated"
                    )
                }
                .onFailure { _snackbarMessage.tryEmit(it.message ?: "Seed failed") }
        }
    }

    fun adminSaveProduct(product: Product, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            catalogRepository.saveProduct(product)
                .onSuccess {
                    _snackbarMessage.tryEmit("Product saved")
                    onDone()
                }
                .onFailure { _snackbarMessage.tryEmit(it.message ?: "Could not save product") }
        }
    }

    fun adminDeleteProduct(productId: String) {
        viewModelScope.launch {
            catalogRepository.deleteProduct(productId)
                .onSuccess { _snackbarMessage.tryEmit("Product deleted") }
                .onFailure { _snackbarMessage.tryEmit(it.message ?: "Could not delete product") }
        }
    }

    fun adminSaveCategory(category: Category, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            catalogRepository.saveCategory(category)
                .onSuccess {
                    _snackbarMessage.tryEmit("Category saved")
                    onDone()
                }
                .onFailure { _snackbarMessage.tryEmit(it.message ?: "Could not save category") }
        }
    }

    fun adminUpdateOrderStatus(order: Order, newStatus: String, note: String, estDelivery: String) {
        viewModelScope.launch {
            orderRepository.adminUpdateOrderStatus(order, newStatus, note, estDelivery)
                .onSuccess { _snackbarMessage.tryEmit("Order updated & customer notified") }
                .onFailure { _snackbarMessage.tryEmit(it.message ?: "Order update failed") }
        }
    }

    fun adminVerifyPayment(payment: PaymentRecord, newStatus: String, note: String) {
        viewModelScope.launch {
            orderRepository.adminVerifyPayment(payment, newStatus, note)
                .onSuccess { _snackbarMessage.tryEmit("Payment marked $newStatus") }
                .onFailure { _snackbarMessage.tryEmit(it.message ?: "Payment update failed") }
        }
    }

    fun adminSaveCoupon(coupon: Coupon, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            catalogRepository.saveCoupon(coupon)
                .onSuccess {
                    _snackbarMessage.tryEmit("Coupon saved")
                    onDone()
                }
                .onFailure { _snackbarMessage.tryEmit(it.message ?: "Coupon save failed") }
        }
    }

    fun adminSaveSettings(settings: StoreSettings) {
        viewModelScope.launch {
            catalogRepository.saveStoreSettings(settings)
                .onSuccess { _snackbarMessage.tryEmit("Store settings updated") }
                .onFailure { _snackbarMessage.tryEmit(it.message ?: "Failed to update settings") }
        }
    }

    companion object {
        private const val TAG = "ShopViewModel"
        private const val STOP_MS = 5000L
        const val ADMIN_EMAIL = "mdrahathosenrabbi13@gmail.com"
    }
}
