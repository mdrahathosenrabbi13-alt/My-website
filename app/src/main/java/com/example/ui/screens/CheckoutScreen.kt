package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BangladeshLocations
import com.example.data.model.CartItem
import com.example.data.model.DeliveryAddress
import com.example.data.model.StoreSettings
import com.example.data.model.UserProfile
import com.example.localization.AppStrings
import com.example.localization.formatBdt
import com.example.ui.theme.BkashPink
import com.example.ui.theme.NagadOrange
import com.example.ui.theme.RocketPurple
import com.example.ui.theme.UpayBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    strings: AppStrings,
    userProfile: UserProfile?,
    savedAddresses: List<DeliveryAddress>,
    cartItems: List<CartItem>,
    storeSettings: StoreSettings,
    couponDiscount: Double,
    calculateDelivery: (district: String, subtotal: Double) -> Double,
    onBack: () -> Unit,
    onConfirmOrder: (
        fullName: String,
        phone: String,
        email: String,
        district: String,
        upazila: String,
        area: String,
        fullAddress: String,
        orderNotes: String,
        paymentMethod: String,
        transactionId: String
    ) -> Unit
) {
    val defaultAddr = savedAddresses.firstOrNull { it.isDefault } ?: savedAddresses.firstOrNull()

    var fullName by remember(userProfile, defaultAddr) {
        mutableStateOf(defaultAddr?.fullName ?: userProfile?.fullName ?: "")
    }
    var phone by remember(userProfile, defaultAddr) {
        mutableStateOf(defaultAddr?.phone ?: userProfile?.phone ?: "")
    }
    var email by remember(userProfile) {
        mutableStateOf(userProfile?.email ?: "")
    }
    var district by remember(defaultAddr) {
        mutableStateOf(defaultAddr?.district ?: "Gaibandha")
    }
    var upazila by remember(defaultAddr) {
        mutableStateOf(defaultAddr?.upazila ?: "Gaibandha Sadar")
    }
    var area by remember(defaultAddr) {
        mutableStateOf(defaultAddr?.area ?: "")
    }
    var fullAddress by remember(defaultAddr) {
        mutableStateOf(defaultAddr?.fullAddress ?: "")
    }
    var orderNotes by remember { mutableStateOf("") }

    val availableMethods = storeSettings.enabledPaymentMethods.ifEmpty {
        listOf("bKash", "Nagad", "Rocket", "Upay", "Visa", "Mastercard", "Cash on Delivery")
    }
    var selectedPaymentMethod by remember { mutableStateOf(availableMethods.first()) }
    var transactionId by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    var districtMenuOpen by remember { mutableStateOf(false) }
    var upazilaMenuOpen by remember { mutableStateOf(false) }
    val upazilaOptions = BangladeshLocations.districtToUpazilas[district] ?: listOf("Sadar")

    val subtotal = cartItems.sumOf { it.unitPrice * it.quantity }
    val deliveryCharge = calculateDelivery(district, subtotal)
    val totalAmount = (subtotal - couponDiscount + deliveryCharge).coerceAtLeast(0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (strings.langCode == "bn") "চেকআউট" else "Checkout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (savedAddresses.isNotEmpty()) {
                item {
                    Text(
                        text = if (strings.langCode == "bn") "সংরক্ষিত ঠিকানা" else "Saved Addresses",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(savedAddresses, key = { it.id }) { addr ->
                            FilterChip(
                                selected = fullAddress == addr.fullAddress && district == addr.district,
                                onClick = {
                                    fullName = addr.fullName
                                    phone = addr.phone
                                    district = addr.district
                                    upazila = addr.upazila
                                    area = addr.area
                                    fullAddress = addr.fullAddress
                                },
                                label = { Text("${addr.label}: ${addr.district}") }
                            )
                        }
                    }
                }
            }

            // Delivery Information Form
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (strings.langCode == "bn") "ডেলিভারি তথ্য" else "Delivery Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text(if (strings.langCode == "bn") "পূর্ণ নাম" else "Full Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("checkout_name_input")
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text(if (strings.langCode == "bn") "মোবাইল নম্বর (০১XXXXXXXXX)" else "Phone Number (01XXXXXXXXX)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("checkout_phone_input")
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ExposedDropdownMenuBox(
                                expanded = districtMenuOpen,
                                onExpandedChange = { districtMenuOpen = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = district,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("District") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(districtMenuOpen) },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = districtMenuOpen,
                                    onDismissRequest = { districtMenuOpen = false }
                                ) {
                                    BangladeshLocations.districts.forEach { d ->
                                        DropdownMenuItem(
                                            text = { Text(d) },
                                            onClick = {
                                                district = d
                                                upazila = BangladeshLocations.districtToUpazilas[d]?.firstOrNull() ?: "Sadar"
                                                districtMenuOpen = false
                                            }
                                        )
                                    }
                                }
                            }

                            ExposedDropdownMenuBox(
                                expanded = upazilaMenuOpen,
                                onExpandedChange = { upazilaMenuOpen = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = upazila,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Upazila") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(upazilaMenuOpen) },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = upazilaMenuOpen,
                                    onDismissRequest = { upazilaMenuOpen = false }
                                ) {
                                    upazilaOptions.forEach { u ->
                                        DropdownMenuItem(
                                            text = { Text(u) },
                                            onClick = {
                                                upazila = u
                                                upazilaMenuOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = area,
                            onValueChange = { area = it },
                            label = { Text("Area / Ward / Bazar") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = fullAddress,
                            onValueChange = { fullAddress = it },
                            label = { Text("Full Delivery Address (House, Road)") },
                            maxLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("checkout_address_input")
                        )

                        OutlinedTextField(
                            value = orderNotes,
                            onValueChange = { orderNotes = it },
                            label = { Text("Order Notes (Optional)") },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Bangladesh Payment Method Selection
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = strings.paymentMethod,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        availableMethods.forEach { method ->
                            val isSelected = selectedPaymentMethod == method
                            val badgeColor = when (method) {
                                "bKash" -> BkashPink
                                "Nagad" -> NagadOrange
                                "Rocket" -> RocketPurple
                                "Upay" -> UpayBlue
                                else -> MaterialTheme.colorScheme.primary
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) badgeColor else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .background(
                                        if (isSelected) badgeColor.copy(alpha = 0.08f)
                                        else Color.Transparent
                                    )
                                    .clickable { selectedPaymentMethod = method }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (method) {
                                        "Cash on Delivery" -> Icons.Rounded.LocalShipping
                                        "Visa", "Mastercard" -> Icons.Rounded.CreditCard
                                        else -> Icons.Rounded.AccountBalanceWallet
                                    },
                                    contentDescription = null,
                                    tint = badgeColor
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = method,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = when (method) {
                                            "Cash on Delivery" -> "Pay in cash upon receiving parcel"
                                            "Visa", "Mastercard" -> "SSLCOMMERZ / aamarPay Gateway Reference"
                                            else -> "Mobile Banking • Verified via Backend"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (selectedPaymentMethod != "Cash on Delivery") {
                            val merchantInfo = when (selectedPaymentMethod) {
                                "bKash" -> "Send Money / Payment to bKash Merchant: ${storeSettings.bkashMerchantNumber}"
                                "Nagad" -> "Send Money / Payment to Nagad Merchant: ${storeSettings.nagadMerchantNumber}"
                                "Rocket" -> "Send Money to Rocket Merchant: ${storeSettings.rocketMerchantNumber}"
                                "Upay" -> "Send Money to Upay Merchant: ${storeSettings.upayMerchantNumber}"
                                else -> "Enter your SSLCOMMERZ / aamarPay Card Transaction Reference"
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Security,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$merchantInfo. Status remains 'Awaiting Verification' until verified on the backend.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = transactionId,
                                onValueChange = { transactionId = it },
                                label = { Text("$selectedPaymentMethod Transaction ID / Gateway Ref") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("checkout_trx_input")
                            )
                        }
                    }
                }
            }

            // Order Summary & Confirm
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (strings.langCode == "bn") "অর্ডার সামারি (${cartItems.sumOf { it.quantity }} আইটেম)"
                            else "Order Summary (${cartItems.sumOf { it.quantity }} items)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(strings.subtotal, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatBdt(subtotal), fontWeight = FontWeight.SemiBold)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${strings.deliveryCharge} ($district)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (deliveryCharge == 0.0) "FREE" else formatBdt(deliveryCharge), fontWeight = FontWeight.SemiBold)
                        }
                        if (couponDiscount > 0.0) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(strings.couponDiscount, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("-${formatBdt(couponDiscount)}", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(strings.total, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = formatBdt(totalAmount),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (formError != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                when {
                                    fullName.trim().length < 2 -> formError = "Please enter your full name"
                                    phone.trim().length < 8 -> formError = "Please enter a valid Bangladeshi phone number"
                                    fullAddress.trim().length < 5 -> formError = "Please enter your full delivery address"
                                    selectedPaymentMethod != "Cash on Delivery" && transactionId.trim().length < 4 ->
                                        formError = "Please enter your $selectedPaymentMethod Transaction ID for verification"
                                    else -> {
                                        formError = null
                                        onConfirmOrder(
                                            fullName,
                                            phone,
                                            email,
                                            district,
                                            upazila,
                                            area,
                                            fullAddress,
                                            orderNotes,
                                            selectedPaymentMethod,
                                            transactionId
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("confirm_order_btn")
                        ) {
                            Text(strings.confirmOrder, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
