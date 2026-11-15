package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ShopRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun userProfileAndCart_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val userRepo = UserShopRepository(firestore, auth)

        val profileRes = withTimeout(TIMEOUT_MS) {
            userRepo.ensureUserProfileExists("Alice Rahman", ALICE_EMAIL, null)
        }
        assertTrue(profileRes.isSuccess)

        val prod = Product(
            id = "prod_${UUID.randomUUID().toString().take(8)}",
            name = "Jamdani Saree",
            nameBn = "জামদানি শাড়ি",
            categoryId = "cat_fashion",
            brand = "Dhakaiya",
            regularPrice = 5000.0,
            discountPrice = 4200.0,
            stock = 10L
        )
        val cartRes = withTimeout(TIMEOUT_MS) {
            userRepo.addToCart(prod, "Standard", "Emerald", 2L)
        }
        assertTrue(cartRes.isSuccess)

        val cartItems = withTimeout(FLOW_TIMEOUT_MS) {
            userRepo.observeCart(uid).first { it.isNotEmpty() }
        }
        assertEquals(1, cartItems.size)
        assertEquals(2L, cartItems.first().quantity)
    }

    @Test
    fun orders_authenticatedCustomer_canPlaceAndObserveOwnOrders() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val orderRepo = OrderRepository(firestore, auth)

        val cartItem = CartItem(
            id = "ci_1",
            userId = uid,
            productId = "prod_1",
            productName = "Wireless Headphones",
            unitPrice = 2890.0,
            regularPrice = 3500.0,
            selectedSize = "Standard",
            selectedColor = "Black",
            quantity = 1L
        )

        val orderRes = withTimeout(TIMEOUT_MS) {
            orderRepo.placeOrder(
                customerName = "Alice Rahman",
                customerPhone = "01787962913",
                customerEmail = ALICE_EMAIL,
                district = "Gaibandha",
                upazila = "Gaibandha Sadar",
                area = "Station Road",
                fullAddress = "House 12, Station Road",
                orderNotes = "Call on arrival",
                items = listOf(cartItem),
                subtotal = 2890.0,
                deliveryCharge = 120.0,
                couponCode = "",
                couponDiscount = 0.0,
                totalAmount = 3010.0,
                paymentMethod = "bKash",
                transactionId = "BKX987654"
            )
        }
        assertTrue(orderRes.isSuccess)
        val orderId = orderRes.getOrThrow()

        val orders = withTimeout(FLOW_TIMEOUT_MS) {
            orderRepo.observeCustomerOrders(uid).first { list -> list.any { it.id == orderId } }
        }
        assertTrue(orders.any { it.id == orderId && it.paymentStatus == "Awaiting Verification" })
    }

    @Test
    fun orders_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        signInTestUser(BOB_EMAIL)
        val bobOrderRepo = OrderRepository(firestore, auth)

        var caught: FirebaseFirestoreException? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobOrderRepo.observeCustomerOrders(aliceUid).first()
            }
        } catch (e: Exception) {
            caught = e.unwrapFirestoreException()
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caught?.code)
    }

    @Test
    fun products_unauthenticatedAccess_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val catalogRepo = CatalogRepository(firestore, auth)

        var caught: FirebaseFirestoreException? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                catalogRepo.observeProducts().first()
            }
        } catch (e: Exception) {
            caught = e.unwrapFirestoreException()
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caught?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_shop@test.com"
        const val BOB_EMAIL = "bob_shop@test.com"
        const val TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3500L
    }
}
