package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Order
import com.example.data.model.OrderStages
import com.example.localization.AppStrings
import com.example.localization.formatBdt
import com.example.localization.orderStatusLabel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersAndTrackingScreen(
    strings: AppStrings,
    orders: List<Order>,
    initialSelectedOrderId: String?,
    onBack: () -> Unit,
    onCancelOrder: (Order) -> Unit
) {
    var orderIdSearch by remember { mutableStateOf("") }
    var selectedOrderId by remember(initialSelectedOrderId) { mutableStateOf(initialSelectedOrderId) }

    val selectedOrder = orders.firstOrNull { it.id == selectedOrderId }
    val filteredOrders = remember(orders, orderIdSearch) {
        val q = orderIdSearch.trim()
        if (q.isBlank()) orders
        else orders.filter {
            it.id.contains(q, ignoreCase = true) ||
                it.status.contains(q, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedOrder != null) "Order #${selectedOrder.id.takeLast(6).uppercase()}"
                        else strings.orders
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedOrder != null) selectedOrderId = null else onBack()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (selectedOrder != null) {
            OrderDetailTrackingView(
                order = selectedOrder,
                strings = strings,
                onCancelOrder = { onCancelOrder(selectedOrder) },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                OutlinedTextField(
                    value = orderIdSearch,
                    onValueChange = { orderIdSearch = it },
                    placeholder = { Text("Track by Order ID...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("order_tracking_search_input")
                )

                if (filteredOrders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.LocalShipping,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (strings.langCode == "bn") "কোনো অর্ডার পাওয়া যায়নি" else "No orders found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredOrders, key = { it.id }) { order ->
                            val dateStr = order.createdAt?.toDate()?.let {
                                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(it)
                            } ?: "Just now"

                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedOrderId = order.id }
                                    .testTag("order_card_${order.id}")
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Order #${order.id.takeLast(6).uppercase()}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = orderStatusLabel(order.status, strings.langCode),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$dateStr • ${order.items.sumOf { it.quantity }} items",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${order.paymentMethod} (${order.paymentStatus})",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = formatBdt(order.totalAmount),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderDetailTrackingView(
    order: Order,
    strings: AppStrings,
    onCancelOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentStageIndex = OrderStages.ALL.indexOf(order.status)
    val canCancel = order.status == "Order Placed" || order.status == "Order Confirmed"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Real-Time 6-Stage Tracking Timeline
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (strings.langCode == "bn") "লাইভ অর্ডার ট্র্যাকিং" else "Live Order Tracking",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (order.estimatedDelivery.isNotBlank()) {
                            Text(
                                text = "ETA: ${order.estimatedDelivery}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    if (order.status == "Cancelled") {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = orderStatusLabel("Cancelled", strings.langCode),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        OrderStages.ALL.forEachIndexed { idx, stage ->
                            val completed = idx <= currentStageIndex
                            val historyMatch = order.trackingHistory.lastOrNull { it.stage == stage }
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.padding(vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (completed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = stage,
                                    tint = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${idx + 1}. ${orderStatusLabel(stage, strings.langCode)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (completed) FontWeight.Bold else FontWeight.Normal,
                                        color = if (completed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (historyMatch != null) {
                                        Text(
                                            text = "${historyMatch.note} (${historyMatch.timestampLabel})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Delivery & Payment Info
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
                        text = "Delivery & Payment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("${order.customerName} • ${order.customerPhone}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${order.fullAddress}, ${order.area}, ${order.upazila}, ${order.district}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    Text("Payment: ${order.paymentMethod} • Status: ${order.paymentStatus}", style = MaterialTheme.typography.bodyMedium)
                    if (order.transactionId.isNotBlank()) {
                        Text("Transaction ID: ${order.transactionId}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Order Items & Invoice Summary
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Items",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.quantity}x ${item.productName} (${item.selectedSize}/${item.selectedColor})",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatBdt(item.unitPrice * item.quantity),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", fontWeight = FontWeight.Bold)
                        Text(formatBdt(order.totalAmount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Invoice & Cancel Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val invoiceText = buildString {
                            appendLine("=== BEST SHOPING BD - INVOICE ===")
                            appendLine("Shop Smart, Shop Easy! | Gaibandha, Bangladesh | 01787962913")
                            appendLine("Order ID: ${order.id}")
                            appendLine("Customer: ${order.customerName} (${order.customerPhone})")
                            appendLine("Address: ${order.fullAddress}, ${order.upazila}, ${order.district}")
                            appendLine("Payment: ${order.paymentMethod} [${order.paymentStatus}]")
                            if (order.transactionId.isNotBlank()) appendLine("TrxID: ${order.transactionId}")
                            appendLine("--- Items ---")
                            order.items.forEach {
                                appendLine("${it.quantity}x ${it.productName} = ${formatBdt(it.unitPrice * it.quantity)}")
                            }
                            appendLine("Subtotal: ${formatBdt(order.subtotal)}")
                            appendLine("Delivery: ${formatBdt(order.deliveryCharge)}")
                            if (order.couponDiscount > 0) appendLine("Discount: -${formatBdt(order.couponDiscount)}")
                            appendLine("TOTAL: ${formatBdt(order.totalAmount)}")
                        }
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Best Shoping BD Invoice #${order.id.takeLast(6).uppercase()}")
                            putExtra(Intent.EXTRA_TEXT, invoiceText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Download / Share Invoice"))
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Invoice")
                }

                if (canCancel) {
                    Button(
                        onClick = onCancelOrder,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("cancel_order_btn")
                    ) {
                        Text("Cancel Order", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
