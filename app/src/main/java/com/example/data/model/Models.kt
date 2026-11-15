package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val avatarUrl: String = "",
    val preferredLanguage: String = "en",
    val notificationsEnabled: Boolean = true,
    val promoNotificationsEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DeliveryAddress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "Home",
    val fullName: String = "",
    val phone: String = "",
    val district: String = "Gaibandha",
    val upazila: String = "Gaibandha Sadar",
    val area: String = "",
    val fullAddress: String = "",
    val isDefault: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Category(
    @DocumentId val id: String = "",
    val name: String = "",
    val nameBn: String = "",
    val iconName: String = "category",
    val displayOrder: Long = 0L,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val name: String = "",
    val nameBn: String = "",
    val description: String = "",
    val descriptionBn: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val brand: String = "",
    val regularPrice: Double = 0.0,
    val discountPrice: Double = 0.0,
    val stock: Long = 0L,
    val imageResNames: List<String> = emptyList(),
    val imageUrls: List<String> = emptyList(),
    val sizes: List<String> = emptyList(),
    val colors: List<String> = emptyList(),
    val isFeatured: Boolean = false,
    val isNewArrival: Boolean = false,
    val isBestSeller: Boolean = false,
    val isFlashSale: Boolean = false,
    val rating: Double = 4.5,
    val reviewCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val effectivePrice: Double
        get() = if (discountPrice > 0.0 && discountPrice < regularPrice) discountPrice else regularPrice

    val discountPercent: Int
        get() = if (regularPrice > 0.0 && discountPrice > 0.0 && discountPrice < regularPrice) {
            (((regularPrice - discountPrice) / regularPrice) * 100).toInt()
        } else 0
}

@IgnoreExtraProperties
data class ProductReview(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val reviewerName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CartItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val productName: String = "",
    val productNameBn: String = "",
    val imageResName: String = "",
    val imageUrl: String = "",
    val unitPrice: Double = 0.0,
    val regularPrice: Double = 0.0,
    val selectedSize: String = "",
    val selectedColor: String = "",
    val quantity: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WishlistItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val productName: String = "",
    val productNameBn: String = "",
    val imageResName: String = "",
    val imageUrl: String = "",
    val price: Double = 0.0,
    val regularPrice: Double = 0.0,
    val inStock: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class OrderItem(
    val productId: String = "",
    val productName: String = "",
    val imageResName: String = "",
    val unitPrice: Double = 0.0,
    val quantity: Long = 1L,
    val selectedSize: String = "",
    val selectedColor: String = ""
)

@IgnoreExtraProperties
data class TrackingStep(
    val stage: String = "",
    val note: String = "",
    val timestampLabel: String = ""
)

@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val district: String = "",
    val upazila: String = "",
    val area: String = "",
    val fullAddress: String = "",
    val orderNotes: String = "",
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    val deliveryCharge: Double = 0.0,
    val couponCode: String = "",
    val couponDiscount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentMethod: String = "Cash on Delivery",
    val paymentStatus: String = "Pending",
    val transactionId: String = "",
    val status: String = "Order Placed",
    val estimatedDelivery: String = "2-4 Business Days",
    val trackingHistory: List<TrackingStep> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PaymentRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderId: String = "",
    val method: String = "",
    val amount: Double = 0.0,
    val transactionId: String = "",
    val status: String = "Pending",
    val verificationNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Coupon(
    @DocumentId val id: String = "",
    val code: String = "",
    val discountPercent: Long = 0L,
    val maxDiscountBdt: Double = 0.0,
    val minOrderBdt: Double = 0.0,
    val isActive: Boolean = true,
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val titleBn: String = "",
    val message: String = "",
    val messageBn: String = "",
    val type: String = "order",
    val orderId: String = "",
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PromoBanner(
    val title: String = "",
    val titleBn: String = "",
    val subtitle: String = "",
    val subtitleBn: String = "",
    val badgeText: String = "",
    val imageResName: String = "img_hero_fashion"
)

@IgnoreExtraProperties
data class StoreSettings(
    @DocumentId val id: String = "store_config",
    val appName: String = "Best Shoping BD",
    val tagline: String = "Shop Smart, Shop Easy!",
    val contactNumber: String = "01787962913",
    val companyAddress: String = "Gaibandha, Bangladesh",
    val dhakaDeliveryCharge: Double = 70.0,
    val outsideDhakaDeliveryCharge: Double = 120.0,
    val freeDeliveryThreshold: Double = 5000.0,
    val bkashMerchantNumber: String = "01787962913",
    val nagadMerchantNumber: String = "01787962913",
    val rocketMerchantNumber: String = "01787962913",
    val upayMerchantNumber: String = "01787962913",
    val enabledPaymentMethods: List<String> = listOf(
        "bKash", "Nagad", "Rocket", "Upay", "Visa", "Mastercard", "Cash on Delivery"
    ),
    val banners: List<PromoBanner> = listOf(
        PromoBanner(
            title = "Festive Heritage Collection",
            titleBn = "উৎসবের ঐতিহ্যবাহী কালেকশন",
            subtitle = "Up to 35% off Jamdani & Leather",
            subtitleBn = "জামদানি ও চামড়ার পণ্যে ৩৫% পর্যন্ত ছাড়",
            badgeText = "EID SPECIAL",
            imageResName = "img_hero_fashion"
        ),
        PromoBanner(
            title = "Flagship Smart Tech Deals",
            titleBn = "স্মার্ট টেক ফ্ল্যাশ ডিল",
            subtitle = "Official warranty • Nationwide delivery",
            subtitleBn = "অফিশিয়াল ওয়ারেন্টি • সারাদেশে ডেলিভারি",
            badgeText = "FLASH SALE",
            imageResName = "img_hero_tech"
        )
    ),
    val facebookUrl: String = "https://facebook.com/bestshopingbd",
    val whatsappNumber: String = "01787962913",
    val promoBannerText: String = "Use code EID15 for 15% off orders above ৳1,500",
    val promoBannerTextBn: String = "৳১,৫০০+ অর্ডারে ১৫% ছাড়ে EID15 কোড ব্যবহার করুন",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object OrderStages {
    val ALL = listOf(
        "Order Placed",
        "Order Confirmed",
        "Processing",
        "Shipped",
        "Out for Delivery",
        "Delivered"
    )
}

object BangladeshLocations {
    val districtToUpazilas: Map<String, List<String>> = mapOf(
        "Gaibandha" to listOf(
            "Gaibandha Sadar", "Gobindaganj", "Palashbari", "Sundarganj",
            "Sadullapur", "Saghata", "Fulchhari"
        ),
        "Dhaka" to listOf(
            "Dhanmondi", "Gulshan", "Mirpur", "Uttara", "Mohammadpur", "Banani", "Motijheel", "Savar"
        ),
        "Rangpur" to listOf("Rangpur Sadar", "Pirganj", "Mithapukur", "Badarganj"),
        "Bogura" to listOf("Bogura Sadar", "Sherpur", "Shibganj", "Kahalu"),
        "Chattogram" to listOf("Kotwali", "Panchlaish", "Halishahar", "Patiya", "Sitakunda"),
        "Sylhet" to listOf("Sylhet Sadar", "Beanibazar", "Golapganj", "Moulvibazar"),
        "Rajshahi" to listOf("Boalia", "Motihar", "Paba", "Godagari"),
        "Khulna" to listOf("Khulna Sadar", "Sonadanga", "Khalishpur", "Dumuria"),
        "Barishal" to listOf("Barishal Sadar", "Bakerganj", "Babuganj"),
        "Mymensingh" to listOf("Mymensingh Sadar", "Trishal", "Bhaluka")
    )

    val districts: List<String> = districtToUpazilas.keys.toList()
}
