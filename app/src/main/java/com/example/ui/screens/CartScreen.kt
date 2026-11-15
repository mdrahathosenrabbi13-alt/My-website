package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ShoppingCartCheckout
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CartItem
import com.example.data.model.Coupon
import com.example.localization.AppStrings
import com.example.localization.formatBdt
import com.example.ui.components.ProductImage

@Composable
fun CartScreen(
    strings: AppStrings,
    cartItems: List<CartItem>,
    availableCoupons: List<Coupon>,
    appliedCoupon: Coupon?,
    couponDiscount: Double,
    estimatedDeliveryCharge: Double,
    onUpdateQuantity: (String, Long) -> Unit,
    onRemoveItem: (String) -> Unit,
    onApplyCoupon: (String, Double) -> Unit,
    onClearCoupon: () -> Unit,
    onContinueShopping: () -> Unit,
    onProceedToCheckout: () -> Unit
) {
    val subtotal = cartItems.sumOf { it.unitPrice * it.quantity }
    val total = (subtotal - couponDiscount + if (cartItems.isEmpty()) 0.0 else estimatedDeliveryCharge)
        .coerceAtLeast(0.0)
    var couponInput by remember(appliedCoupon) { mutableStateOf(appliedCoupon?.code ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.cart} (${cartItems.sumOf { it.quantity }})",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onContinueShopping) {
                    Text(strings.continueShopping)
                }
            }
        }

        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.ShoppingCartCheckout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (strings.langCode == "bn") "আপনার কার্ট খালি" else "Your shopping cart is empty",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onContinueShopping,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(strings.continueShopping)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cartItems, key = { it.id }) { item ->
                    val name = if (strings.langCode == "bn" && item.productNameBn.isNotBlank()) item.productNameBn else item.productName
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProductImage(
                                imageResName = item.imageResName,
                                imageUrl = item.imageUrl,
                                contentDescription = name,
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2
                                )
                                Text(
                                    text = "${item.selectedSize} • ${item.selectedColor}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = formatBdt(item.unitPrice * item.quantity),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainer
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { onUpdateQuantity(item.id, item.quantity - 1) },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(Icons.Rounded.Remove, contentDescription = "Decrease")
                                            }
                                            Text(
                                                text = "${item.quantity}",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            IconButton(
                                                onClick = { onUpdateQuantity(item.id, item.quantity + 1) },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(Icons.Rounded.Add, contentDescription = "Increase")
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = { onRemoveItem(item.id) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Remove item",
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Coupon Box
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = couponInput,
                                    onValueChange = { couponInput = it },
                                    placeholder = { Text(strings.couponPlaceholder) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("cart_coupon_input")
                                )
                                Button(
                                    onClick = { onApplyCoupon(couponInput, subtotal) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(52.dp)
                                        .testTag("cart_apply_coupon_btn")
                                ) {
                                    Text(strings.applyCoupon)
                                }
                            }
                            val activeCoupons = availableCoupons.filter { it.isActive }
                            if (activeCoupons.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(activeCoupons, key = { it.id }) { c ->
                                        AssistChip(
                                            onClick = {
                                                couponInput = c.code
                                                onApplyCoupon(c.code, subtotal)
                                            },
                                            label = { Text("${c.code} (${c.discountPercent}% OFF)") }
                                        )
                                    }
                                }
                            }
                            if (appliedCoupon != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Applied: ${appliedCoupon.code} (-${formatBdt(couponDiscount)})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    TextButton(onClick = onClearCoupon) {
                                        Text("Remove")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Summary Footer
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 92.dp)
                ) {
                    SummaryLine(strings.subtotal, formatBdt(subtotal))
                    SummaryLine(strings.deliveryCharge, if (estimatedDeliveryCharge == 0.0) "FREE" else formatBdt(estimatedDeliveryCharge))
                    if (couponDiscount > 0.0) {
                        SummaryLine(strings.couponDiscount, "-${formatBdt(couponDiscount)}")
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = strings.total,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatBdt(total),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onProceedToCheckout,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("proceed_to_checkout_btn")
                    ) {
                        Text(strings.proceedToCheckout, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
