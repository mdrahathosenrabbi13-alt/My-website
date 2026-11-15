package com.example.data.repository

import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.PaymentRecord
import com.example.data.model.TrackingStep
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing orders.")

    fun observeCustomerOrders(userId: String): Flow<List<Order>> =
        db.collection("orders")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Order::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt }
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(fsError ?: error, OperationType.LIST, "orders")
                }
                throw error
            }

    fun observeCustomerPayments(userId: String): Flow<List<PaymentRecord>> =
        db.collection("payments")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PaymentRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt }
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(fsError ?: error, OperationType.LIST, "payments")
                }
                throw error
            }

    fun observeAllOrders(): Flow<List<Order>> =
        db.collection("orders")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Order::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt }
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(fsError ?: error, OperationType.LIST, "orders")
                }
                throw error
            }

    fun observeAllPayments(): Flow<List<PaymentRecord>> =
        db.collection("payments")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PaymentRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt }
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(fsError ?: error, OperationType.LIST, "payments")
                }
                throw error
            }

    suspend fun placeOrder(
        customerName: String,
        customerPhone: String,
        customerEmail: String,
        district: String,
        upazila: String,
        area: String,
        fullAddress: String,
        orderNotes: String,
        items: List<CartItem>,
        subtotal: Double,
        deliveryCharge: Double,
        couponCode: String,
        couponDiscount: Double,
        totalAmount: Double,
        paymentMethod: String,
        transactionId: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(items.isNotEmpty()) { "Cart cannot be empty" }

        val nowLabel = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date())
        val initialPaymentStatus = if (paymentMethod == "Cash on Delivery") {
            "Pending"
        } else {
            "Awaiting Verification"
        }

        val orderRef = db.collection("orders").document()
        val itemMaps = items.map { item ->
            mapOf(
                "productId" to item.productId,
                "productName" to item.productName,
                "imageResName" to item.imageResName,
                "unitPrice" to item.unitPrice,
                "quantity" to item.quantity,
                "selectedSize" to item.selectedSize,
                "selectedColor" to item.selectedColor
            )
        }
        val initialTracking = listOf(
            mapOf(
                "stage" to "Order Placed",
                "note" to if (paymentMethod == "Cash on Delivery") {
                    "Order placed with Cash on Delivery"
                } else {
                    "Order placed • $paymentMethod TrxID: $transactionId awaiting verification"
                },
                "timestampLabel" to nowLabel
            )
        )

        val estDays = if (district.equals("Gaibandha", ignoreCase = true)) {
            "1-2 Days (Local Express)"
        } else if (district.equals("Dhaka", ignoreCase = true)) {
            "2-3 Business Days"
        } else {
            "3-5 Business Days"
        }

        val orderPayload = mapOf(
            "userId" to uid,
            "customerName" to customerName.trim(),
            "customerPhone" to customerPhone.trim(),
            "customerEmail" to customerEmail.trim(),
            "district" to district.trim(),
            "upazila" to upazila.trim(),
            "area" to area.trim(),
            "fullAddress" to fullAddress.trim(),
            "orderNotes" to orderNotes.trim(),
            "items" to itemMaps,
            "subtotal" to subtotal,
            "deliveryCharge" to deliveryCharge,
            "couponCode" to couponCode.trim().uppercase(),
            "couponDiscount" to couponDiscount,
            "totalAmount" to totalAmount,
            "paymentMethod" to paymentMethod,
            "paymentStatus" to initialPaymentStatus,
            "transactionId" to transactionId.trim(),
            "status" to "Order Placed",
            "estimatedDelivery" to estDays,
            "trackingHistory" to initialTracking,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        orderRef.set(orderPayload).await()

        val paymentRef = db.collection("payments").document()
        paymentRef.set(
            mapOf(
                "userId" to uid,
                "orderId" to orderRef.id,
                "method" to paymentMethod,
                "amount" to totalAmount,
                "transactionId" to transactionId.trim(),
                "status" to initialPaymentStatus,
                "verificationNote" to if (paymentMethod == "Cash on Delivery") {
                    "Collect on delivery"
                } else {
                    "Submitted for backend/gateway verification"
                },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        // Create confirmation notification for user
        db.collection("users").document(uid).collection("notifications")
            .document()
            .set(
                mapOf(
                    "userId" to uid,
                    "title" to "Order #${orderRef.id.takeLast(6).uppercase()} Placed",
                    "titleBn" to "অর্ডার #${orderRef.id.takeLast(6).uppercase()} গৃহীত হয়েছে",
                    "message" to "Thank you! Your order of ৳${totalAmount.toLong()} via $paymentMethod has been received.",
                    "messageBn" to "ধন্যবাদ! ৳${totalAmount.toLong()} টাকার অর্ডারটি ($paymentMethod) গ্রহণ করা হয়েছে।",
                    "type" to "order",
                    "orderId" to orderRef.id,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

        orderRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "orders")
    }

    suspend fun cancelOrderByCustomer(order: Order): Result<Unit> = runCatching {
        requireUserId()
        val nowLabel = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date())
        val updatedHistory = order.trackingHistory.map {
            mapOf("stage" to it.stage, "note" to it.note, "timestampLabel" to it.timestampLabel)
        } + mapOf(
            "stage" to "Cancelled",
            "note" to "Cancelled by customer",
            "timestampLabel" to nowLabel
        )
        db.collection("orders").document(order.id).update(
            mapOf(
                "status" to "Cancelled",
                "trackingHistory" to updatedHistory,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "orders/${order.id}")
    }

    suspend fun adminUpdateOrderStatus(
        order: Order,
        newStatus: String,
        note: String,
        estimatedDelivery: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val nowLabel = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date())
        val updatedHistory = order.trackingHistory.map {
            mapOf("stage" to it.stage, "note" to it.note, "timestampLabel" to it.timestampLabel)
        } + mapOf(
            "stage" to newStatus,
            "note" to note.ifBlank { "Status updated to $newStatus" },
            "timestampLabel" to nowLabel
        )

        db.collection("orders").document(order.id).update(
            mapOf(
                "status" to newStatus,
                "estimatedDelivery" to estimatedDelivery,
                "trackingHistory" to updatedHistory,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        // Automatically notify the customer
        db.collection("users").document(order.userId).collection("notifications")
            .document()
            .set(
                mapOf(
                    "userId" to order.userId,
                    "title" to "Order #${order.id.takeLast(6).uppercase()}: $newStatus",
                    "titleBn" to "অর্ডার আপডেট: $newStatus",
                    "message" to note.ifBlank { "Your order is now $newStatus." },
                    "messageBn" to "আপনার অর্ডারটি এখন \"$newStatus\" ধাপে রয়েছে।",
                    "type" to "order",
                    "orderId" to order.id,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "orders/${order.id}")
    }

    suspend fun adminVerifyPayment(
        payment: PaymentRecord,
        newPaymentStatus: String,
        verificationNote: String
    ): Result<Unit> = runCatching {
        requireUserId()
        db.collection("payments").document(payment.id).update(
            mapOf(
                "status" to newPaymentStatus,
                "verificationNote" to verificationNote.ifBlank { "Updated by admin" },
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        db.collection("orders").document(payment.orderId).update(
            mapOf(
                "paymentStatus" to newPaymentStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        db.collection("users").document(payment.userId).collection("notifications")
            .document()
            .set(
                mapOf(
                    "userId" to payment.userId,
                    "title" to "Payment $newPaymentStatus",
                    "titleBn" to "পেমেন্ট $newPaymentStatus",
                    "message" to "Payment for Order #${payment.orderId.takeLast(6).uppercase()} (${payment.method}) is $newPaymentStatus.",
                    "messageBn" to "অর্ডার #${payment.orderId.takeLast(6).uppercase()}-এর পেমেন্ট ($newPaymentStatus) আপডেট হয়েছে।",
                    "type" to "payment",
                    "orderId" to payment.orderId,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "payments/${payment.id}")
    }
}
