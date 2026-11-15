package com.example.localization

import java.text.NumberFormat
import java.util.Locale

data class AppStrings(
    val langCode: String,
    val appName: String,
    val tagline: String,
    val locationBanner: String,
    val shopNow: String,
    val searchPlaceholder: String,
    val categories: String,
    val featuredProducts: String,
    val newArrivals: String,
    val bestSellers: String,
    val flashSale: String,
    val recommended: String,
    val specialOffers: String,
    val recentlyViewed: String,
    val seeAll: String,
    val addToCart: String,
    val buyNow: String,
    val addedToCart: String,
    val outOfStock: String,
    val inStock: String,
    val home: String,
    val shop: String,
    val cart: String,
    val wishlist: String,
    val profile: String,
    val orders: String,
    val notifications: String,
    val adminPanel: String,
    val signInWithGoogle: String,
    val onboardingTitle1: String,
    val onboardingDesc1: String,
    val onboardingTitle2: String,
    val onboardingDesc2: String,
    val onboardingTitle3: String,
    val onboardingDesc3: String,
    val skip: String,
    val next: String,
    val getStarted: String,
    val subtotal: String,
    val deliveryCharge: String,
    val couponDiscount: String,
    val total: String,
    val applyCoupon: String,
    val couponPlaceholder: String,
    val proceedToCheckout: String,
    val continueShopping: String,
    val confirmOrder: String,
    val paymentMethod: String,
    val orderPlacedTitle: String,
    val trackOrder: String,
    val whatsappSupport: String,
    val logout: String
)

object Strings {
    val english = AppStrings(
        langCode = "en",
        appName = "Best Shoping BD",
        tagline = "Shop Smart, Shop Easy!",
        locationBanner = "Gaibandha, Bangladesh • 01787962913",
        shopNow = "Shop Now",
        searchPlaceholder = "Search products, brands...",
        categories = "Categories",
        featuredProducts = "Featured Products",
        newArrivals = "New Arrivals",
        bestSellers = "Best Sellers",
        flashSale = "Flash Sale",
        recommended = "Recommended For You",
        specialOffers = "Special Offers",
        recentlyViewed = "Recently Viewed",
        seeAll = "See All",
        addToCart = "Add to Cart",
        buyNow = "Buy Now",
        addedToCart = "Added to cart",
        outOfStock = "Out of Stock",
        inStock = "In Stock",
        home = "Home",
        shop = "Shop",
        cart = "Cart",
        wishlist = "Wishlist",
        profile = "Profile",
        orders = "Orders",
        notifications = "Notifications",
        adminPanel = "Admin Dashboard",
        signInWithGoogle = "Continue with Google",
        onboardingTitle1 = "Discover Amazing Products",
        onboardingDesc1 = "Explore curated Bangladeshi fashion, electronics, skincare, and daily essentials at unbeatable BDT prices.",
        onboardingTitle2 = "Shop Easily and Securely",
        onboardingDesc2 = "Pay safely with bKash, Nagad, Rocket, Upay, Visa, Mastercard, or Cash on Delivery across Bangladesh.",
        onboardingTitle3 = "Get Fast Home Delivery",
        onboardingDesc3 = "From Gaibandha to all 64 districts with real-time order tracking straight to your doorstep.",
        skip = "Skip",
        next = "Next",
        getStarted = "Get Started",
        subtotal = "Subtotal",
        deliveryCharge = "Delivery Charge",
        couponDiscount = "Coupon Discount",
        total = "Total Payable",
        applyCoupon = "Apply",
        couponPlaceholder = "Coupon code (e.g. EID15)",
        proceedToCheckout = "Proceed to Checkout",
        continueShopping = "Continue Shopping",
        confirmOrder = "Confirm Order",
        paymentMethod = "Payment Method",
        orderPlacedTitle = "Order Placed!",
        trackOrder = "Track Order",
        whatsappSupport = "WhatsApp Support",
        logout = "Sign Out"
    )

    val bangla = AppStrings(
        langCode = "bn",
        appName = "বেস্ট শপিং বিডি",
        tagline = "স্মার্ট কেনাকাটা, সহজ জীবন!",
        locationBanner = "গাইবান্ধা, বাংলাদেশ • ০১৭৮৭৯৬২৯১৩",
        shopNow = "এখনই কিনুন",
        searchPlaceholder = "পণ্য বা ব্র্যান্ড খুঁজুন...",
        categories = "ক্যাটাগরি",
        featuredProducts = "বিশেষ পণ্য",
        newArrivals = "নতুন কালেকশন",
        bestSellers = "সর্বাধিক বিক্রিত",
        flashSale = "ফ্ল্যাশ সেল",
        recommended = "আপনার জন্য বাছাইকৃত",
        specialOffers = "বিশেষ অফার",
        recentlyViewed = "সম্প্রতি দেখা",
        seeAll = "সব দেখুন",
        addToCart = "কার্টে যোগ করুন",
        buyNow = "এখনই কিনুন",
        addedToCart = "কার্টে যোগ হয়েছে",
        outOfStock = "স্টক শেষ",
        inStock = "স্টকে আছে",
        home = "হোম",
        shop = "শপ",
        cart = "কার্ট",
        wishlist = "পছন্দ",
        profile = "প্রোফাইল",
        orders = "অর্ডার",
        notifications = "নোটিফিকেশন",
        adminPanel = "অ্যাডমিন ড্যাশবোর্ড",
        signInWithGoogle = "গুগল দিয়ে সাইন ইন করুন",
        onboardingTitle1 = "সেরা মানের পণ্য খুঁজুন",
        onboardingDesc1 = "বাংলাদেশি ফ্যাশন, ইলেকট্রনিক্স, স্কিনকেয়ার ও নিত্যপ্রয়োজনীয় পণ্য সাশ্রয়ী মূল্যে কিনুন।",
        onboardingTitle2 = "সহজ ও নিরাপদ পেমেন্ট",
        onboardingDesc2 = "বিকাশ, নগদ, রকেট, উপায়, কার্ড অথবা ক্যাশ অন ডেলিভারিতে নিশ্চিন্তে পেমেন্ট করুন।",
        onboardingTitle3 = "দ্রুত হোম ডেলিভারি",
        onboardingDesc3 = "গাইবান্ধা থেকে সারাদেশের ৬৪ জেলায় রিয়েল-টাইম অর্ডার ট্র্যাকিংসহ দ্রুত ডেলিভারি।",
        skip = "এড়িয়ে যান",
        next = "পরবর্তী",
        getStarted = "শুরু করুন",
        subtotal = "সাবটোটাল",
        deliveryCharge = "ডেলিভারি চার্জ",
        couponDiscount = "কুপন ছাড়",
        total = "সর্বমোট",
        applyCoupon = "প্রয়োগ করুন",
        couponPlaceholder = "কুপন কোড (যেমন EID15)",
        proceedToCheckout = "চেকআউট করুন",
        continueShopping = "কেনাকাটা চালিয়ে যান",
        confirmOrder = "অর্ডার নিশ্চিত করুন",
        paymentMethod = "পেমেন্ট মাধ্যম",
        orderPlacedTitle = "অর্ডার সম্পন্ন হয়েছে!",
        trackOrder = "অর্ডার ট্র্যাক করুন",
        whatsappSupport = "হোয়াটসঅ্যাপ সাপোর্ট",
        logout = "সাইন আউট"
    )

    fun forLang(langCode: String): AppStrings = if (langCode == "bn") bangla else english
}

fun formatBdt(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.Builder().setLanguage("en").setRegion("IN").build())
    formatter.maximumFractionDigits = 0
    return "৳${formatter.format(amount.toLong())}"
}

fun orderStatusLabel(status: String, lang: String): String {
    if (lang != "bn") return status
    return when (status) {
        "Order Placed" -> "অর্ডার গৃহীত"
        "Order Confirmed" -> "অর্ডার নিশ্চিত"
        "Processing" -> "প্রস্তুত হচ্ছে"
        "Shipped" -> "পাঠানো হয়েছে"
        "Out for Delivery" -> "ডেলিভারির পথে"
        "Delivered" -> "ডেলিভারি সম্পন্ন"
        "Cancelled" -> "বাতিল"
        else -> status
    }
}
