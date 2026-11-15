package com.example.data.repository

import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.CartItem
import com.example.data.model.DeliveryAddress
import com.example.data.model.Product
import com.example.data.model.UserNotification
import com.example.data.model.UserProfile
import com.example.data.model.WishlistItem
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

class UserShopRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing user data.")

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(fsError ?: error, OperationType.GET, "users/$userId")
                }
                throw error
            }

    suspend fun ensureUserProfileExists(
        displayName: String?,
        email: String?,
        avatarUrl: String?
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val existing = docRef.get().await()
        if (!existing.exists()) {
            val payload = mapOf(
                "userId" to uid,
                "fullName" to (displayName?.takeIf { it.isNotBlank() } ?: "Valued Shopper"),
                "email" to (email ?: ""),
                "phone" to "",
                "avatarUrl" to (avatarUrl ?: ""),
                "preferredLanguage" to "en",
                "notificationsEnabled" to true,
                "promoNotificationsEnabled" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}")
    }

    suspend fun updateProfile(
        fullName: String,
        phone: String,
        email: String,
        avatarUrl: String,
        preferredLanguage: String,
        notificationsEnabled: Boolean,
        promoNotificationsEnabled: Boolean
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        docRef.update(
            mapOf(
                "fullName" to fullName.trim().ifBlank { "Valued Shopper" },
                "phone" to phone.trim(),
                "email" to email.trim(),
                "avatarUrl" to avatarUrl.trim(),
                "preferredLanguage" to if (preferredLanguage == "bn") "bn" else "en",
                "notificationsEnabled" to notificationsEnabled,
                "promoNotificationsEnabled" to promoNotificationsEnabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}")
    }

    fun observeAddresses(userId: String): Flow<List<DeliveryAddress>> =
        db.collection("users").document(userId).collection("addresses")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    DeliveryAddress::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.isDefault }
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(
                        fsError ?: error,
                        OperationType.LIST,
                        "users/$userId/addresses"
                    )
                }
                throw error
            }

    suspend fun saveAddress(address: DeliveryAddress): Result<String> = runCatching {
        val uid = requireUserId()
        val col = db.collection("users").document(uid).collection("addresses")
        val docRef = if (address.id.isBlank()) col.document() else col.document(address.id)
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "label" to address.label.trim().ifBlank { "Home" },
            "fullName" to address.fullName.trim(),
            "phone" to address.phone.trim(),
            "district" to address.district.trim(),
            "upazila" to address.upazila.trim(),
            "area" to address.area.trim(),
            "fullAddress" to address.fullAddress.trim(),
            "isDefault" to address.isDefault,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (address.id.isBlank()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
            docRef.set(payload).await()
        } else {
            docRef.update(
                mapOf(
                    "label" to payload["label"],
                    "fullName" to payload["fullName"],
                    "phone" to payload["phone"],
                    "district" to payload["district"],
                    "upazila" to payload["upazila"],
                    "area" to payload["area"],
                    "fullAddress" to payload["fullAddress"],
                    "isDefault" to payload["isDefault"],
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        docRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/addresses")
    }

    suspend fun deleteAddress(addressId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        db.collection("users").document(uid).collection("addresses").document(addressId)
            .delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(
            e,
            OperationType.DELETE,
            "users/${auth.currentUser?.uid}/addresses/$addressId"
        )
    }

    fun observeCart(userId: String): Flow<List<CartItem>> =
        db.collection("users").document(userId).collection("cart")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CartItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(fsError ?: error, OperationType.LIST, "users/$userId/cart")
                }
                throw error
            }

    suspend fun addToCart(
        product: Product,
        size: String,
        color: String,
        quantity: Long
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val safeKey = "${product.id}_${size.replace(Regex("[^a-zA-Z0-9]"), "")}_${color.replace(Regex("[^a-zA-Z0-9]"), "")}"
            .take(120)
        val docRef = db.collection("users").document(uid).collection("cart").document(safeKey)
        val existingSnap = docRef.get().await()
        if (existingSnap.exists()) {
            val prevQty = existingSnap.getLong("quantity") ?: 0L
            val newQty = (prevQty + quantity).coerceIn(1L, 99L)
            docRef.update(
                mapOf(
                    "quantity" to newQty,
                    "unitPrice" to product.effectivePrice,
                    "regularPrice" to product.regularPrice,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "productId" to product.id,
                    "productName" to product.name,
                    "productNameBn" to product.nameBn,
                    "imageResName" to (product.imageResNames.firstOrNull() ?: ""),
                    "imageUrl" to (product.imageUrls.firstOrNull() ?: ""),
                    "unitPrice" to product.effectivePrice,
                    "regularPrice" to product.regularPrice,
                    "selectedSize" to size,
                    "selectedColor" to color,
                    "quantity" to quantity.coerceIn(1L, 99L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/cart")
    }

    suspend fun updateCartQuantity(cartItemId: String, quantity: Long): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("cart").document(cartItemId)
        if (quantity <= 0L) {
            docRef.delete().await()
        } else {
            docRef.update(
                mapOf(
                    "quantity" to quantity.coerceIn(1L, 99L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(
            e,
            OperationType.UPDATE,
            "users/${auth.currentUser?.uid}/cart/$cartItemId"
        )
    }

    suspend fun removeFromCart(cartItemId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        db.collection("users").document(uid).collection("cart").document(cartItemId)
            .delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(
            e,
            OperationType.DELETE,
            "users/${auth.currentUser?.uid}/cart/$cartItemId"
        )
    }

    suspend fun clearCart(): Result<Unit> = runCatching {
        val uid = requireUserId()
        val items = db.collection("users").document(uid).collection("cart").get().await()
        val batch = db.batch()
        items.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "users/${auth.currentUser?.uid}/cart")
    }

    fun observeWishlist(userId: String): Flow<List<WishlistItem>> =
        db.collection("users").document(userId).collection("wishlist")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    WishlistItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(
                        fsError ?: error,
                        OperationType.LIST,
                        "users/$userId/wishlist"
                    )
                }
                throw error
            }

    suspend fun toggleWishlist(product: Product): Result<Boolean> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("wishlist").document(product.id)
        val snap = docRef.get().await()
        if (snap.exists()) {
            docRef.delete().await()
            false
        } else {
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "productId" to product.id,
                    "productName" to product.name,
                    "productNameBn" to product.nameBn,
                    "imageResName" to (product.imageResNames.firstOrNull() ?: ""),
                    "imageUrl" to (product.imageUrls.firstOrNull() ?: ""),
                    "price" to product.effectivePrice,
                    "regularPrice" to product.regularPrice,
                    "inStock" to (product.stock > 0L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            true
        }
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(
            e,
            OperationType.WRITE,
            "users/${auth.currentUser?.uid}/wishlist/${product.id}"
        )
    }

    fun observeNotifications(userId: String): Flow<List<UserNotification>> =
        db.collection("users").document(userId).collection("notifications")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    UserNotification::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt }
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(
                        fsError ?: error,
                        OperationType.LIST,
                        "users/$userId/notifications"
                    )
                }
                throw error
            }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        db.collection("users").document(uid).collection("notifications")
            .document(notificationId)
            .update(
                mapOf(
                    "isRead" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(
            e,
            OperationType.UPDATE,
            "users/${auth.currentUser?.uid}/notifications/$notificationId"
        )
    }

    suspend fun markAllNotificationsRead(): Result<Unit> = runCatching {
        val uid = requireUserId()
        val snap = db.collection("users").document(uid).collection("notifications")
            .whereEqualTo("isRead", false).get().await()
        if (!snap.isEmpty) {
            val batch = db.batch()
            snap.documents.forEach { doc ->
                batch.update(
                    doc.reference,
                    mapOf("isRead" to true, "updatedAt" to FieldValue.serverTimestamp())
                )
            }
            batch.commit().await()
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(
            e,
            OperationType.UPDATE,
            "users/${auth.currentUser?.uid}/notifications"
        )
    }

    suspend fun sendNotificationToUser(
        targetUserId: String,
        title: String,
        titleBn: String,
        message: String,
        messageBn: String,
        type: String,
        orderId: String = ""
    ): Result<Unit> = runCatching {
        requireUserId()
        db.collection("users").document(targetUserId).collection("notifications")
            .document()
            .set(
                mapOf(
                    "userId" to targetUserId,
                    "title" to title,
                    "titleBn" to titleBn,
                    "message" to message,
                    "messageBn" to messageBn,
                    "type" to type,
                    "orderId" to orderId,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "users/$targetUserId/notifications")
    }

    // Admin customer list
    fun observeAllCustomers(): Flow<List<UserProfile>> =
        db.collection("users")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(fsError ?: error, OperationType.LIST, "users")
                }
                throw error
            }
}
