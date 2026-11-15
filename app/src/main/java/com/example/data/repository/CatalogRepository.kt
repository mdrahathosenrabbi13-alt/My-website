package com.example.data.repository

import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.Category
import com.example.data.model.Coupon
import com.example.data.model.Product
import com.example.data.model.ProductReview
import com.example.data.model.StoreSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CatalogRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing catalog.")

    fun observeCategories(): Flow<List<Category>> =
        db.collection("categories")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Category::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.displayOrder }
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(fsError ?: error, OperationType.LIST, "categories")
                }
                throw error
            }

    fun observeProducts(): Flow<List<Product>> =
        db.collection("products")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Product::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt }
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(fsError ?: error, OperationType.LIST, "products")
                }
                throw error
            }

    fun observeProductReviews(productId: String): Flow<List<ProductReview>> =
        db.collection("products").document(productId).collection("reviews")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ProductReview::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt }
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(
                        fsError ?: error,
                        OperationType.LIST,
                        "products/$productId/reviews"
                    )
                }
                throw error
            }

    fun observeCoupons(): Flow<List<Coupon>> =
        db.collection("coupons")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Coupon::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(fsError ?: error, OperationType.LIST, "coupons")
                }
                throw error
            }

    fun observeStoreSettings(): Flow<StoreSettings> =
        db.collection("settings").document("store_config")
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    StoreSettings::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: StoreSettings()
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(
                        fsError ?: error,
                        OperationType.GET,
                        "settings/store_config"
                    )
                }
                throw error
            }

    suspend fun addReview(
        productId: String,
        reviewerName: String,
        rating: Long,
        comment: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val reviewRef = db.collection("products").document(productId)
            .collection("reviews").document()
        val payload = mapOf(
            "userId" to uid,
            "productId" to productId,
            "reviewerName" to reviewerName.trim().ifBlank { "Customer" },
            "rating" to rating.coerceIn(1L, 5L),
            "comment" to comment.trim(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        reviewRef.set(payload).await()

        // Update aggregate rating on product
        val reviewsSnap = db.collection("products").document(productId)
            .collection("reviews").get().await()
        val reviews = reviewsSnap.toObjects(ProductReview::class.java)
        if (reviews.isNotEmpty()) {
            val avg = reviews.map { it.rating.toDouble() }.average()
            db.collection("products").document(productId).update(
                mapOf(
                    "rating" to avg,
                    "reviewCount" to reviews.size.toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "products/$productId/reviews")
    }

    suspend fun saveCategory(category: Category): Result<String> = runCatching {
        requireUserId()
        val col = db.collection("categories")
        val docRef = if (category.id.isBlank()) col.document() else col.document(category.id)
        if (category.id.isBlank()) {
            docRef.set(
                mapOf(
                    "name" to category.name.trim(),
                    "nameBn" to category.nameBn.trim(),
                    "iconName" to category.iconName,
                    "displayOrder" to category.displayOrder,
                    "isActive" to category.isActive,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            docRef.update(
                mapOf(
                    "name" to category.name.trim(),
                    "nameBn" to category.nameBn.trim(),
                    "iconName" to category.iconName,
                    "displayOrder" to category.displayOrder,
                    "isActive" to category.isActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        docRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "categories/${category.id}")
    }

    suspend fun saveProduct(product: Product): Result<String> = runCatching {
        requireUserId()
        val col = db.collection("products")
        val docRef = if (product.id.isBlank()) col.document() else col.document(product.id)
        val commonFields = mutableMapOf<String, Any>(
            "name" to product.name.trim(),
            "nameBn" to product.nameBn.trim(),
            "description" to product.description.trim(),
            "descriptionBn" to product.descriptionBn.trim(),
            "categoryId" to product.categoryId,
            "categoryName" to product.categoryName,
            "brand" to product.brand.trim(),
            "regularPrice" to product.regularPrice,
            "discountPrice" to product.discountPrice,
            "stock" to product.stock,
            "imageResNames" to product.imageResNames,
            "imageUrls" to product.imageUrls,
            "sizes" to product.sizes,
            "colors" to product.colors,
            "isFeatured" to product.isFeatured,
            "isNewArrival" to product.isNewArrival,
            "isBestSeller" to product.isBestSeller,
            "isFlashSale" to product.isFlashSale,
            "rating" to product.rating,
            "reviewCount" to product.reviewCount,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (product.id.isBlank()) {
            commonFields["createdAt"] = FieldValue.serverTimestamp()
            docRef.set(commonFields).await()
        } else {
            docRef.update(commonFields).await()
        }
        docRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "products/${product.id}")
    }

    suspend fun deleteProduct(productId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("products").document(productId).delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "products/$productId")
    }

    suspend fun saveCoupon(coupon: Coupon): Result<String> = runCatching {
        requireUserId()
        val col = db.collection("coupons")
        val docRef = if (coupon.id.isBlank()) col.document() else col.document(coupon.id)
        val payload = mutableMapOf<String, Any>(
            "code" to coupon.code.trim().uppercase(),
            "discountPercent" to coupon.discountPercent,
            "maxDiscountBdt" to coupon.maxDiscountBdt,
            "minOrderBdt" to coupon.minOrderBdt,
            "isActive" to coupon.isActive,
            "description" to coupon.description.trim(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (coupon.id.isBlank()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
            docRef.set(payload).await()
        } else {
            docRef.update(payload).await()
        }
        docRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "coupons/${coupon.id}")
    }

    suspend fun saveStoreSettings(settings: StoreSettings): Result<Unit> = runCatching {
        requireUserId()
        val bannerMaps = settings.banners.map { b ->
            mapOf(
                "title" to b.title,
                "titleBn" to b.titleBn,
                "subtitle" to b.subtitle,
                "subtitleBn" to b.subtitleBn,
                "badgeText" to b.badgeText,
                "imageResName" to b.imageResName
            )
        }
        val payload = mapOf(
            "appName" to settings.appName.trim(),
            "tagline" to settings.tagline.trim(),
            "contactNumber" to settings.contactNumber.trim(),
            "companyAddress" to settings.companyAddress.trim(),
            "dhakaDeliveryCharge" to settings.dhakaDeliveryCharge,
            "outsideDhakaDeliveryCharge" to settings.outsideDhakaDeliveryCharge,
            "freeDeliveryThreshold" to settings.freeDeliveryThreshold,
            "bkashMerchantNumber" to settings.bkashMerchantNumber.trim(),
            "nagadMerchantNumber" to settings.nagadMerchantNumber.trim(),
            "rocketMerchantNumber" to settings.rocketMerchantNumber.trim(),
            "upayMerchantNumber" to settings.upayMerchantNumber.trim(),
            "enabledPaymentMethods" to settings.enabledPaymentMethods,
            "banners" to bannerMaps,
            "facebookUrl" to settings.facebookUrl.trim(),
            "whatsappNumber" to settings.whatsappNumber.trim(),
            "promoBannerText" to settings.promoBannerText.trim(),
            "promoBannerTextBn" to settings.promoBannerTextBn.trim(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("settings").document("store_config").set(payload).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "settings/store_config")
    }

    suspend fun seedInitialStorefrontIfNeeded(): Result<Boolean> = runCatching {
        requireUserId()
        val existingProducts = db.collection("products").limit(1).get().await()
        if (!existingProducts.isEmpty) return@runCatching false

        val batch = db.batch()
        val categories = listOf(
            "cat_fashion" to mapOf(
                "name" to "Fashion", "nameBn" to "ফ্যাশন", "iconName" to "checkroom",
                "displayOrder" to 1L, "isActive" to true,
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
            ),
            "cat_electronics" to mapOf(
                "name" to "Electronics", "nameBn" to "ইলেকট্রনিক্স", "iconName" to "devices",
                "displayOrder" to 2L, "isActive" to true,
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
            ),
            "cat_beauty" to mapOf(
                "name" to "Beauty & Care", "nameBn" to "রূপচর্চা", "iconName" to "spa",
                "displayOrder" to 3L, "isActive" to true,
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
            ),
            "cat_footwear" to mapOf(
                "name" to "Footwear", "nameBn" to "জুতা ও স্যান্ডেল", "iconName" to "directions_run",
                "displayOrder" to 4L, "isActive" to true,
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
            ),
            "cat_accessories" to mapOf(
                "name" to "Accessories", "nameBn" to "এক্সেসরিজ", "iconName" to "watch",
                "displayOrder" to 5L, "isActive" to true,
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        categories.forEach { (id, data) ->
            batch.set(db.collection("categories").document(id), data)
        }

        val products = listOf(
            "prod_jamdani_saree" to mapOf(
                "name" to "Emerald Gold Jamdani Silk Saree",
                "nameBn" to "এমেরাল্ড গোল্ড জামদানি সিল্ক শাড়ি",
                "description" to "Handcrafted Bangladeshi Jamdani weave in rich emerald silk with shimmering golden motif border. Ideal for Eid, weddings, and festive occasions.",
                "descriptionBn" to "ঐতিহ্যবাহী বাংলাদেশি জামদানি সিল্ক শাড়ি, গাঢ় সবুজ ও সোনালী নকশার অপূর্ব সমন্বয়।",
                "categoryId" to "cat_fashion", "categoryName" to "Fashion", "brand" to "Dhakaiya Loom",
                "regularPrice" to 6500.0, "discountPrice" to 4850.0, "stock" to 18L,
                "imageResNames" to listOf("img_prod_saree", "img_hero_fashion"),
                "imageUrls" to emptyList<String>(),
                "sizes" to listOf("12 Haat (Standard)"), "colors" to listOf("Emerald Gold", "Royal Maroon"),
                "isFeatured" to true, "isNewArrival" to true, "isBestSeller" to true, "isFlashSale" to false,
                "rating" to 4.9, "reviewCount" to 42L,
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
            ),
            "prod_anc_headphones" to mapOf(
                "name" to "Studio Pro ANC Wireless Headphones",
                "nameBn" to "স্টুডিও প্রো ওয়্যারলেস হেডফোন",
                "description" to "Active Noise Cancelling Bluetooth 5.3 over-ear headphones with 45-hour battery life, deep bass, and crystal-clear call mic.",
                "descriptionBn" to "নয়েজ ক্যান্সেলিং ব্লুটুথ ৫.৩ হেডফোন, ৪৫ ঘন্টা ব্যাটারি ব্যাকআপ।",
                "categoryId" to "cat_electronics", "categoryName" to "Electronics", "brand" to "SonicBD",
                "regularPrice" to 4500.0, "discountPrice" to 3290.0, "stock" to 25L,
                "imageResNames" to listOf("img_prod_headphones", "img_lifestyle_pack"),
                "imageUrls" to emptyList<String>(),
                "sizes" to listOf("Adjustable"), "colors" to listOf("Matte Black", "Midnight Olive"),
                "isFeatured" to true, "isNewArrival" to false, "isBestSeller" to true, "isFlashSale" to true,
                "rating" to 4.8, "reviewCount" to 64L,
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
            ),
            "prod_leather_handbag" to mapOf(
                "name" to "Classic Cognac Leather Handbag",
                "nameBn" to "ক্লাসিক লেদার হ্যান্ডব্যাগ",
                "description" to "Structured full-grain leather tote handbag with gold-tone clasp and spacious compartments for everyday elegance.",
                "descriptionBn" to "প্রিমিয়াম চামড়ার তৈরি আধুনিক ও টেকসই লেডিস হ্যান্ডব্যাগ।",
                "categoryId" to "cat_accessories", "categoryName" to "Accessories", "brand" to "Bengal Hide",
                "regularPrice" to 3800.0, "discountPrice" to 2950.0, "stock" to 14L,
                "imageResNames" to listOf("img_prod_handbag", "img_hero_fashion"),
                "imageUrls" to emptyList<String>(),
                "sizes" to listOf("Medium"), "colors" to listOf("Cognac Brown", "Jet Black"),
                "isFeatured" to true, "isNewArrival" to true, "isBestSeller" to false, "isFlashSale" to false,
                "rating" to 4.7, "reviewCount" to 29L,
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
            ),
            "prod_smartwatch_amoled" to mapOf(
                "name" to "Apex AMOLED Fitness Smartwatch",
                "nameBn" to "এপেক্স অ্যামোলেড স্মার্টওয়াচ",
                "description" to "1.9-inch AMOLED always-on display, Bluetooth calling, SpO2 & heart rate tracking, IP68 water resistance, 10-day battery.",
                "descriptionBn" to "ব্লুটুথ কলিং ও হেলথ ট্র্যাকিং সুবিধাসম্পন্ন প্রিমিয়াম স্মার্টওয়াচ।",
                "categoryId" to "cat_accessories", "categoryName" to "Accessories", "brand" to "TechPulse BD",
                "regularPrice" to 3600.0, "discountPrice" to 2490.0, "stock" to 32L,
                "imageResNames" to listOf("img_prod_watch", "img_hero_tech"),
                "imageUrls" to emptyList<String>(),
                "sizes" to listOf("44mm"), "colors" to listOf("Obsidian Black", "Starlight Silver"),
                "isFeatured" to false, "isNewArrival" to true, "isBestSeller" to true, "isFlashSale" to true,
                "rating" to 4.6, "reviewCount" to 53L,
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
            ),
            "prod_white_sneakers" to mapOf(
                "name" to "Urban Ivory Comfort Sneakers",
                "nameBn" to "আরবান আইভরি কমফোর্ট স্নিকার্স",
                "description" to "Lightweight breathable court sneakers with memory-foam insole designed for all-day walking comfort across city streets.",
                "descriptionBn" to "হালকা ও আরামদায়ক ডেইলি ওয়্যার ফ্যাশনেবল স্নিকার্স।",
                "categoryId" to "cat_footwear", "categoryName" to "Footwear", "brand" to "StepBD",
                "regularPrice" to 2400.0, "discountPrice" to 1790.0, "stock" to 40L,
                "imageResNames" to listOf("img_prod_sneakers", "img_lifestyle_pack"),
                "imageUrls" to emptyList<String>(),
                "sizes" to listOf("40", "41", "42", "43", "44"), "colors" to listOf("Warm Ivory", "Slate Grey"),
                "isFeatured" to true, "isNewArrival" to false, "isBestSeller" to true, "isFlashSale" to false,
                "rating" to 4.7, "reviewCount" to 38L,
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
            ),
            "prod_glow_serum" to mapOf(
                "name" to "Botanical Vitamin C Glow Serum",
                "nameBn" to "বোটানিক্যাল ভিটামিন সি গ্লো সিরাম",
                "description" to "Dermatologist-tested brightening serum with natural hyaluronic acid and saffron extract for radiant, hydrated skin.",
                "descriptionBn" to "ত্বকের উজ্জ্বলতা ও আর্দ্রতা বাড়াতে প্রাকৃতিক ভিটামিন সি সিরাম।",
                "categoryId" to "cat_beauty", "categoryName" to "Beauty & Care", "brand" to "Sundarban Botanics",
                "regularPrice" to 1350.0, "discountPrice" to 990.0, "stock" to 48L,
                "imageResNames" to listOf("img_prod_skincare", "img_lifestyle_pack"),
                "imageUrls" to emptyList<String>(),
                "sizes" to listOf("50ml"), "colors" to listOf("Amber Glass"),
                "isFeatured" to false, "isNewArrival" to true, "isBestSeller" to false, "isFlashSale" to true,
                "rating" to 4.8, "reviewCount" to 31L,
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
            ),
            "prod_flagship_phone" to mapOf(
                "name" to "Nova X1 5G Smartphone (8/256GB)",
                "nameBn" to "নোভা এক্স১ ৫জি স্মার্টফোন",
                "description" to "120Hz AMOLED display, 50MP OIS triple camera, 5000mAh fast charging battery, official Bangladesh warranty.",
                "descriptionBn" to "১২০ হার্জ অ্যামোলেড ডিসপ্লে ও ৫০ মেগাপিক্সেল ক্যামেরাযুক্ত অফিসিয়াল স্মার্টফোন।",
                "categoryId" to "cat_electronics", "categoryName" to "Electronics", "brand" to "NovaTech",
                "regularPrice" to 32990.0, "discountPrice" to 28990.0, "stock" to 9L,
                "imageResNames" to listOf("img_prod_smartphone", "img_hero_tech"),
                "imageUrls" to emptyList<String>(),
                "sizes" to listOf("8GB / 256GB", "12GB / 512GB"), "colors" to listOf("Emerald Black", "Titanium Gold"),
                "isFeatured" to true, "isNewArrival" to true, "isBestSeller" to false, "isFlashSale" to false,
                "rating" to 4.9, "reviewCount" to 19L,
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
            ),
            "prod_tws_earbuds" to mapOf(
                "name" to "AirSound Pro TWS Earbuds",
                "nameBn" to "এয়ারসাউন্ড প্রো ইয়ারবাডস",
                "description" to "Compact ANC true wireless earbuds with quad-mic ENC, wireless charging case, and low-latency gaming mode.",
                "descriptionBn" to "ক্রিস্টাল ক্লিয়ার সাউন্ড ও ফাস্ট চার্জিং ট্রু ওয়্যারলেস ইয়ারবাডস।",
                "categoryId" to "cat_electronics", "categoryName" to "Electronics", "brand" to "SonicBD",
                "regularPrice" to 2200.0, "discountPrice" to 1450.0, "stock" to 55L,
                "imageResNames" to listOf("img_prod_earbuds", "img_hero_tech"),
                "imageUrls" to emptyList<String>(),
                "sizes" to listOf("Universal Fit"), "colors" to listOf("Graphite", "Pearl White"),
                "isFeatured" to false, "isNewArrival" to false, "isBestSeller" to true, "isFlashSale" to true,
                "rating" to 4.6, "reviewCount" to 77L,
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        products.forEach { (id, data) ->
            batch.set(db.collection("products").document(id), data)
        }

        batch.set(
            db.collection("coupons").document("coupon_eid15"),
            mapOf(
                "code" to "EID15", "discountPercent" to 15L, "maxDiscountBdt" to 1000.0,
                "minOrderBdt" to 1500.0, "isActive" to true,
                "description" to "15% Festive Discount up to ৳1,000 (Min order ৳1,500)",
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            db.collection("coupons").document("coupon_shop10"),
            mapOf(
                "code" to "SHOP10", "discountPercent" to 10L, "maxDiscountBdt" to 500.0,
                "minOrderBdt" to 500.0, "isActive" to true,
                "description" to "10% Welcome Discount up to ৳500",
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        val defaultSettings = StoreSettings()
        batch.set(
            db.collection("settings").document("store_config"),
            mapOf(
                "appName" to defaultSettings.appName,
                "tagline" to defaultSettings.tagline,
                "contactNumber" to defaultSettings.contactNumber,
                "companyAddress" to defaultSettings.companyAddress,
                "dhakaDeliveryCharge" to defaultSettings.dhakaDeliveryCharge,
                "outsideDhakaDeliveryCharge" to defaultSettings.outsideDhakaDeliveryCharge,
                "freeDeliveryThreshold" to defaultSettings.freeDeliveryThreshold,
                "bkashMerchantNumber" to defaultSettings.bkashMerchantNumber,
                "nagadMerchantNumber" to defaultSettings.nagadMerchantNumber,
                "rocketMerchantNumber" to defaultSettings.rocketMerchantNumber,
                "upayMerchantNumber" to defaultSettings.upayMerchantNumber,
                "enabledPaymentMethods" to defaultSettings.enabledPaymentMethods,
                "banners" to defaultSettings.banners.map { b ->
                    mapOf(
                        "title" to b.title,
                        "titleBn" to b.titleBn,
                        "subtitle" to b.subtitle,
                        "subtitleBn" to b.subtitleBn,
                        "badgeText" to b.badgeText,
                        "imageResName" to b.imageResName
                    )
                },
                "facebookUrl" to defaultSettings.facebookUrl,
                "whatsappNumber" to defaultSettings.whatsappNumber,
                "promoBannerText" to defaultSettings.promoBannerText,
                "promoBannerTextBn" to defaultSettings.promoBannerTextBn,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        batch.commit().await()
        true
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "products")
    }
}
