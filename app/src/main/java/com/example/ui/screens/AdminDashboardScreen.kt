package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Category
import com.example.data.model.Coupon
import com.example.data.model.Order
import com.example.data.model.OrderStages
import com.example.data.model.PaymentRecord
import com.example.data.model.Product
import com.example.data.model.StoreSettings
import com.example.data.model.UserProfile
import com.example.localization.formatBdt
import com.example.ui.components.BUNDLED_PRODUCT_IMAGES

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    products: List<Product>,
    categories: List<Category>,
    orders: List<Order>,
    payments: List<PaymentRecord>,
    customers: List<UserProfile>,
    coupons: List<Coupon>,
    storeSettings: StoreSettings,
    onBack: () -> Unit,
    onSeedDefaultCatalog: () -> Unit,
    onSaveProduct: (Product) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onSaveCategory: (Category) -> Unit,
    onUpdateOrderStatus: (Order, String, String, String) -> Unit,
    onVerifyPayment: (PaymentRecord, String, String) -> Unit,
    onSaveCoupon: (Coupon) -> Unit,
    onSaveSettings: (StoreSettings) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Products", "Orders", "Payments", "Settings")

    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showCouponDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_btn")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            when (selectedTab) {
                0 -> AdminOverviewTab(
                    products = products,
                    orders = orders,
                    customers = customers,
                    onSeedCatalog = onSeedDefaultCatalog
                )
                1 -> AdminProductsTab(
                    products = products,
                    categories = categories,
                    onAddProduct = { editingProduct = Product() },
                    onEditProduct = { editingProduct = it },
                    onDeleteProduct = onDeleteProduct,
                    onAddCategory = { showCategoryDialog = true }
                )
                2 -> AdminOrdersTab(
                    orders = orders,
                    onUpdateOrderStatus = onUpdateOrderStatus
                )
                3 -> AdminPaymentsAndCouponsTab(
                    payments = payments,
                    coupons = coupons,
                    onVerifyPayment = onVerifyPayment,
                    onAddCoupon = { showCouponDialog = true },
                    onToggleCoupon = { c -> onSaveCoupon(c.copy(isActive = !c.isActive)) }
                )
                4 -> AdminCustomersAndSettingsTab(
                    customers = customers,
                    storeSettings = storeSettings,
                    onSaveSettings = onSaveSettings
                )
            }
        }
    }

    editingProduct?.let { prod ->
        ProductEditorDialog(
            initial = prod,
            categories = categories,
            onDismiss = { editingProduct = null },
            onSave = { updated ->
                onSaveProduct(updated)
                editingProduct = null
            }
        )
    }

    if (showCategoryDialog) {
        var catName by remember { mutableStateOf("") }
        var catNameBn by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("New Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = catName, onValueChange = { catName = it }, label = { Text("Name (EN)") }, singleLine = true)
                    OutlinedTextField(value = catNameBn, onValueChange = { catNameBn = it }, label = { Text("Name (BN)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (catName.isNotBlank()) {
                        onSaveCategory(
                            Category(
                                name = catName,
                                nameBn = catNameBn.ifBlank { catName },
                                displayOrder = (categories.size + 1).toLong(),
                                isActive = true
                            )
                        )
                        showCategoryDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showCategoryDialog = false }) { Text("Cancel") } }
        )
    }

    if (showCouponDialog) {
        var code by remember { mutableStateOf("") }
        var percent by remember { mutableStateOf("15") }
        var maxDisc by remember { mutableStateOf("500") }
        var minOrder by remember { mutableStateOf("1000") }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCouponDialog = false },
            title = { Text("Create Coupon") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code (e.g. WINTER20)") }, singleLine = true)
                    OutlinedTextField(value = percent, onValueChange = { percent = it }, label = { Text("Discount %") }, singleLine = true)
                    OutlinedTextField(value = maxDisc, onValueChange = { maxDisc = it }, label = { Text("Max Discount (BDT)") }, singleLine = true)
                    OutlinedTextField(value = minOrder, onValueChange = { minOrder = it }, label = { Text("Min Order (BDT)") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val pct = percent.toLongOrNull()?.coerceIn(1L, 90L) ?: 10L
                    if (code.trim().length >= 2) {
                        onSaveCoupon(
                            Coupon(
                                code = code.trim().uppercase(),
                                discountPercent = pct,
                                maxDiscountBdt = maxDisc.toDoubleOrNull() ?: 500.0,
                                minOrderBdt = minOrder.toDoubleOrNull() ?: 0.0,
                                isActive = true,
                                description = desc
                            )
                        )
                        showCouponDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showCouponDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AdminOverviewTab(
    products: List<Product>,
    orders: List<Order>,
    customers: List<UserProfile>,
    onSeedCatalog: () -> Unit
) {
    val totalRevenue = orders.filter { it.status != "Cancelled" }.sumOf { it.totalAmount }
    val pending = orders.count { it.status in listOf("Order Placed", "Order Confirmed", "Processing", "Shipped", "Out for Delivery") }
    val completed = orders.count { it.status == "Delivered" }
    val cancelled = orders.count { it.status == "Cancelled" }

    val primaryColor = MaterialTheme.colorScheme.primary
    val chartValues = remember(orders) {
        val base = listOf(12000f, 18500f, 15000f, 24000f, 31000f, (totalRevenue.toFloat().coerceAtLeast(8000f)))
        val max = base.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        base.map { it / max }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Total Revenue", formatBdt(totalRevenue), Modifier.weight(1f))
                StatCard("Total Orders", "${orders.size}", Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Products", "${products.size}", Modifier.weight(1f))
                StatCard("Customers", "${customers.size}", Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Pending", "$pending", Modifier.weight(1f))
                StatCard("Delivered", "$completed", Modifier.weight(1f))
                StatCard("Cancelled", "$cancelled", Modifier.weight(1f))
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Monthly Sales Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        val barWidth = size.width / (chartValues.size * 2f)
                        chartValues.forEachIndexed { idx, ratio ->
                            val barH = size.height * ratio
                            val x = idx * (barWidth * 2f) + barWidth * 0.5f
                            drawRoundRect(
                                color = primaryColor,
                                topLeft = Offset(x, size.height - barH),
                                size = Size(barWidth, barH),
                                cornerRadius = CornerRadius(12f, 12f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf("Jun", "Jul", "Aug", "Sep", "Oct", "Now").forEach { m ->
                            Text(m, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item {
            OutlinedButton(
                onClick = onSeedCatalog,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Seed Default Best Shoping BD Catalog")
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun AdminProductsTab(
    products: List<Product>,
    categories: List<Category>,
    onAddProduct: () -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onAddCategory: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onAddProduct, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Product")
                }
                OutlinedButton(onClick = onAddCategory, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Text("Add Category (${categories.size})")
                }
            }
        }
        items(products, key = { it.id }) { prod ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(prod.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "${prod.categoryName} • ${formatBdt(prod.effectivePrice)} • Stock: ${prod.stock}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onEditProduct(prod) }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { onDeleteProduct(prod.id) }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminOrdersTab(
    orders: List<Order>,
    onUpdateOrderStatus: (Order, String, String, String) -> Unit
) {
    var filterStatus by remember { mutableStateOf<String?>(null) }
    val filtered = if (filterStatus == null) orders else orders.filter { it.status == filterStatus }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(selected = filterStatus == null, onClick = { filterStatus = null }, label = { Text("All (${orders.size})") })
                }
                items(OrderStages.ALL + "Cancelled") { st ->
                    FilterChip(
                        selected = filterStatus == st,
                        onClick = { filterStatus = if (filterStatus == st) null else st },
                        label = { Text(st) }
                    )
                }
            }
        }
        items(filtered, key = { it.id }) { order ->
            val currentIdx = OrderStages.ALL.indexOf(order.status)
            val nextStage = if (currentIdx in 0 until OrderStages.ALL.lastIndex) OrderStages.ALL[currentIdx + 1] else null

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("#${order.id.takeLast(6).uppercase()} • ${order.customerName}", fontWeight = FontWeight.Bold)
                        Text(formatBdt(order.totalAmount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("${order.customerPhone} • ${order.district} • ${order.paymentMethod} (${order.paymentStatus})", style = MaterialTheme.typography.bodySmall)
                    Text("Current Stage: ${order.status}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (nextStage != null) {
                            Button(
                                onClick = {
                                    onUpdateOrderStatus(order, nextStage, "Moved to $nextStage by Admin", order.estimatedDelivery)
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Advance to $nextStage")
                            }
                        }
                        if (order.status != "Cancelled" && order.status != "Delivered") {
                            OutlinedButton(
                                onClick = {
                                    onUpdateOrderStatus(order, "Cancelled", "Cancelled by Admin", order.estimatedDelivery)
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminPaymentsAndCouponsTab(
    payments: List<PaymentRecord>,
    coupons: List<Coupon>,
    onVerifyPayment: (PaymentRecord, String, String) -> Unit,
    onAddCoupon: () -> Unit,
    onToggleCoupon: (Coupon) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Coupons (${coupons.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(onClick = onAddCoupon, shape = RoundedCornerShape(10.dp)) { Text("New Coupon") }
            }
        }
        items(coupons, key = { it.id }) { c ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${c.code} • ${c.discountPercent}% OFF", fontWeight = FontWeight.Bold)
                        Text(c.description, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = c.isActive, onCheckedChange = { onToggleCoupon(c) })
                }
            }
        }
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            Text("Payment Verification (${payments.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(payments, key = { it.id }) { pay ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${pay.method} • ${formatBdt(pay.amount)}", fontWeight = FontWeight.Bold)
                        Text(pay.status, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    if (pay.transactionId.isNotBlank()) {
                        Text("TrxID: ${pay.transactionId}", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (pay.status != "Paid") {
                            Button(onClick = { onVerifyPayment(pay, "Paid", "Verified by Admin") }) {
                                Text("Verify Paid")
                            }
                        }
                        if (pay.status == "Paid") {
                            OutlinedButton(onClick = { onVerifyPayment(pay, "Refunded", "Refunded by Admin") }) {
                                Text("Refund")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminCustomersAndSettingsTab(
    customers: List<UserProfile>,
    storeSettings: StoreSettings,
    onSaveSettings: (StoreSettings) -> Unit
) {
    var appName by remember(storeSettings) { mutableStateOf(storeSettings.appName) }
    var tagline by remember(storeSettings) { mutableStateOf(storeSettings.tagline) }
    var contactNumber by remember(storeSettings) { mutableStateOf(storeSettings.contactNumber) }
    var address by remember(storeSettings) { mutableStateOf(storeSettings.companyAddress) }
    var dhakaCharge by remember(storeSettings) { mutableStateOf(storeSettings.dhakaDeliveryCharge.toInt().toString()) }
    var outsideCharge by remember(storeSettings) { mutableStateOf(storeSettings.outsideDhakaDeliveryCharge.toInt().toString()) }
    var bkashNum by remember(storeSettings) { mutableStateOf(storeSettings.bkashMerchantNumber) }
    var promoText by remember(storeSettings) { mutableStateOf(storeSettings.promoBannerText) }
    var enabledMethods by remember(storeSettings) { mutableStateOf(storeSettings.enabledPaymentMethods) }

    val allMethods = listOf("bKash", "Nagad", "Rocket", "Upay", "Visa", "Mastercard", "Cash on Delivery")

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Store & Payment Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = appName, onValueChange = { appName = it }, label = { Text("App Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tagline, onValueChange = { tagline = it }, label = { Text("Tagline") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = contactNumber, onValueChange = { contactNumber = it }, label = { Text("Contact / WhatsApp") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Company Address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = dhakaCharge, onValueChange = { dhakaCharge = it }, label = { Text("Dhaka Delivery (৳)") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = outsideCharge, onValueChange = { outsideCharge = it }, label = { Text("Outside Dhaka (৳)") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = bkashNum, onValueChange = { bkashNum = it }, label = { Text("bKash Merchant Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = promoText, onValueChange = { promoText = it }, label = { Text("Promo Banner Text") }, modifier = Modifier.fillMaxWidth())

                    Text("Enabled Payment Methods", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    allMethods.forEach { method ->
                        val checked = enabledMethods.contains(method)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    enabledMethods = if (isChecked) (enabledMethods + method).distinct()
                                    else (enabledMethods - method).ifEmpty { listOf("Cash on Delivery") }
                                }
                            )
                            Text(method)
                        }
                    }

                    Button(
                        onClick = {
                            onSaveSettings(
                                storeSettings.copy(
                                    appName = appName,
                                    tagline = tagline,
                                    contactNumber = contactNumber,
                                    whatsappNumber = contactNumber,
                                    companyAddress = address,
                                    dhakaDeliveryCharge = dhakaCharge.toDoubleOrNull() ?: 70.0,
                                    outsideDhakaDeliveryCharge = outsideCharge.toDoubleOrNull() ?: 120.0,
                                    bkashMerchantNumber = bkashNum,
                                    nagadMerchantNumber = bkashNum,
                                    rocketMerchantNumber = bkashNum,
                                    upayMerchantNumber = bkashNum,
                                    promoBannerText = promoText,
                                    enabledPaymentMethods = enabledMethods
                                )
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Store Settings")
                    }
                }
            }
        }

        item {
            Text("Registered Customers (${customers.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(customers, key = { it.userId }) { c ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(c.fullName, fontWeight = FontWeight.Bold)
                    Text("${c.email} • ${c.phone.ifBlank { "No phone" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductEditorDialog(
    initial: Product,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var nameBn by remember { mutableStateOf(initial.nameBn) }
    var brand by remember { mutableStateOf(initial.brand.ifBlank { "Best Shoping BD" }) }
    var regPrice by remember { mutableStateOf(if (initial.regularPrice > 0) initial.regularPrice.toInt().toString() else "") }
    var discPrice by remember { mutableStateOf(if (initial.discountPrice > 0) initial.discountPrice.toInt().toString() else "") }
    var stock by remember { mutableStateOf(if (initial.stock > 0) initial.stock.toString() else "20") }
    var desc by remember { mutableStateOf(initial.description) }
    var selectedCatId by remember { mutableStateOf(initial.categoryId.ifBlank { categories.firstOrNull()?.id ?: "cat_fashion" }) }
    var selectedImg by remember { mutableStateOf(initial.imageResNames.firstOrNull() ?: "img_prod_saree") }
    var customUrl by remember { mutableStateOf(initial.imageUrls.firstOrNull() ?: "") }
    var isFeatured by remember { mutableStateOf(initial.isFeatured) }
    var isFlashSale by remember { mutableStateOf(initial.isFlashSale) }
    var isBestSeller by remember { mutableStateOf(initial.isBestSeller) }

    var catMenuOpen by remember { mutableStateOf(false) }
    var imgMenuOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isBlank()) "Add Product" else "Edit Product") },
        text = {
            LazyColumn(
                modifier = Modifier.height(420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name (EN)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(value = nameBn, onValueChange = { nameBn = it }, label = { Text("Product Name (BN)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                item {
                    ExposedDropdownMenuBox(expanded = catMenuOpen, onExpandedChange = { catMenuOpen = it }) {
                        val catLabel = categories.firstOrNull { it.id == selectedCatId }?.name ?: selectedCatId
                        OutlinedTextField(
                            value = catLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catMenuOpen) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = catMenuOpen, onDismissRequest = { catMenuOpen = false }) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        selectedCatId = cat.id
                                        catMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = regPrice, onValueChange = { regPrice = it }, label = { Text("Regular ৳") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = discPrice, onValueChange = { discPrice = it }, label = { Text("Discount ৳") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                }
                item {
                    OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Available Stock") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                item {
                    ExposedDropdownMenuBox(expanded = imgMenuOpen, onExpandedChange = { imgMenuOpen = it }) {
                        OutlinedTextField(
                            value = BUNDLED_PRODUCT_IMAGES.firstOrNull { it.first == selectedImg }?.second ?: selectedImg,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Bundled Studio Photo") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(imgMenuOpen) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = imgMenuOpen, onDismissRequest = { imgMenuOpen = false }) {
                            BUNDLED_PRODUCT_IMAGES.forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        selectedImg = key
                                        imgMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(value = customUrl, onValueChange = { customUrl = it }, label = { Text("Custom Image URL (Optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isFeatured, onCheckedChange = { isFeatured = it })
                        Text("Featured")
                        Spacer(modifier = Modifier.width(8.dp))
                        Checkbox(checked = isFlashSale, onCheckedChange = { isFlashSale = it })
                        Text("Flash Sale")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isBestSeller, onCheckedChange = { isBestSeller = it })
                        Text("Best Seller")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val rPrice = regPrice.toDoubleOrNull() ?: 0.0
                val dPrice = discPrice.toDoubleOrNull() ?: rPrice
                val catName = categories.firstOrNull { it.id == selectedCatId }?.name ?: "General"
                if (name.isNotBlank() && rPrice > 0.0) {
                    onSave(
                        initial.copy(
                            name = name,
                            nameBn = nameBn.ifBlank { name },
                            brand = brand,
                            categoryId = selectedCatId,
                            categoryName = catName,
                            regularPrice = rPrice,
                            discountPrice = dPrice,
                            stock = stock.toLongOrNull() ?: 10L,
                            description = desc.ifBlank { "Quality product from Best Shoping BD." },
                            imageResNames = listOf(selectedImg),
                            imageUrls = if (customUrl.isNotBlank()) listOf(customUrl.trim()) else emptyList(),
                            sizes = initial.sizes.ifEmpty { listOf("Standard") },
                            colors = initial.colors.ifEmpty { listOf("Default") },
                            isFeatured = isFeatured,
                            isFlashSale = isFlashSale,
                            isBestSeller = isBestSeller,
                            isNewArrival = initial.id.isBlank() || initial.isNewArrival
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
