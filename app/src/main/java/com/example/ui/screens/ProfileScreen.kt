package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.BangladeshLocations
import com.example.data.model.DeliveryAddress
import com.example.data.model.PaymentRecord
import com.example.data.model.UserProfile
import com.example.localization.AppStrings
import com.example.localization.formatBdt
import com.example.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    strings: AppStrings,
    userProfile: UserProfile?,
    addresses: List<DeliveryAddress>,
    payments: List<PaymentRecord>,
    orderCount: Int,
    themeMode: ThemeMode,
    isAdmin: Boolean,
    onSaveProfile: (name: String, phone: String, email: String, avatarUrl: String, lang: String) -> Unit,
    onSaveAddress: (DeliveryAddress) -> Unit,
    onDeleteAddress: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onOpenOrders: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAdminDashboard: () -> Unit,
    onSignOut: () -> Unit
) {
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAddAddressDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Profile Header Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!userProfile?.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = userProfile?.avatarUrl,
                            contentDescription = userProfile?.fullName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userProfile?.fullName ?: "Shopper",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = userProfile?.email ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!userProfile?.phone.isNullOrBlank()) {
                            Text(
                                text = userProfile?.phone ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier.testTag("edit_profile_button")
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit Profile")
                    }
                }
            }
        }

        // Quick Actions (Orders, Notifications, Admin)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    onClick = onOpenOrders,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_orders_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Rounded.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("${strings.orders} ($orderCount)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Track & Invoices", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = onOpenNotifications,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_notifications_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Rounded.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(strings.notifications, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("Alerts & Offers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        }

        if (isAdmin) {
            item {
                Button(
                    onClick = onOpenAdminDashboard,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("open_admin_dashboard_btn")
                ) {
                    Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.adminPanel, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Language & Theme Settings
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Appearance & Language", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = strings.langCode == "en",
                            onClick = {
                                onSaveProfile(
                                    userProfile?.fullName ?: "Shopper",
                                    userProfile?.phone ?: "",
                                    userProfile?.email ?: "",
                                    userProfile?.avatarUrl ?: "",
                                    "en"
                                )
                            },
                            label = { Text("English") }
                        )
                        FilterChip(
                            selected = strings.langCode == "bn",
                            onClick = {
                                onSaveProfile(
                                    userProfile?.fullName ?: "Shopper",
                                    userProfile?.phone ?: "",
                                    userProfile?.email ?: "",
                                    userProfile?.avatarUrl ?: "",
                                    "bn"
                                )
                            },
                            label = { Text("বাংলা") }
                        )
                    }
                }
            }
        }

        // Delivery Addresses
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Delivery Addresses (${addresses.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showAddAddressDialog = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                    if (addresses.isEmpty()) {
                        Text(
                            text = "No saved addresses yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        addresses.forEach { addr ->
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${addr.label} • ${addr.fullName}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("${addr.fullAddress}, ${addr.upazila}, ${addr.district}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(addr.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onDeleteAddress(addr.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete address", tint = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Payment History
        if (payments.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Payment History", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        payments.take(6).forEach { pay ->
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("${pay.method} • ${pay.status}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    if (pay.transactionId.isNotBlank()) {
                                        Text("TrxID: ${pay.transactionId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text(formatBdt(pay.amount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        // Logout Button
        item {
            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.logout, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showEditProfileDialog) {
        var nameInput by remember { mutableStateOf(userProfile?.fullName ?: "") }
        var phoneInput by remember { mutableStateOf(userProfile?.phone ?: "") }
        var emailInput by remember { mutableStateOf(userProfile?.email ?: "") }
        var avatarInput by remember { mutableStateOf(userProfile?.avatarUrl ?: "") }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Full Name") }, singleLine = true)
                    OutlinedTextField(value = phoneInput, onValueChange = { phoneInput = it }, label = { Text("Phone Number") }, singleLine = true)
                    OutlinedTextField(value = emailInput, onValueChange = { emailInput = it }, label = { Text("Email") }, singleLine = true)
                    OutlinedTextField(value = avatarInput, onValueChange = { avatarInput = it }, label = { Text("Profile Picture URL") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSaveProfile(nameInput, phoneInput, emailInput, avatarInput, strings.langCode)
                    showEditProfileDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddAddressDialog) {
        var label by remember { mutableStateOf("Home") }
        var fullName by remember { mutableStateOf(userProfile?.fullName ?: "") }
        var phone by remember { mutableStateOf(userProfile?.phone ?: "") }
        var district by remember { mutableStateOf("Gaibandha") }
        var upazila by remember { mutableStateOf("Gaibandha Sadar") }
        var area by remember { mutableStateOf("") }
        var fullAddr by remember { mutableStateOf("") }
        var districtOpen by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddAddressDialog = false },
            title = { Text("New Delivery Address") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label (Home/Office)") }, singleLine = true)
                    OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Recipient Name") }, singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, singleLine = true)
                    ExposedDropdownMenuBox(expanded = districtOpen, onExpandedChange = { districtOpen = it }) {
                        OutlinedTextField(
                            value = district,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("District") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(districtOpen) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = districtOpen, onDismissRequest = { districtOpen = false }) {
                            BangladeshLocations.districts.forEach { d ->
                                DropdownMenuItem(
                                    text = { Text(d) },
                                    onClick = {
                                        district = d
                                        upazila = BangladeshLocations.districtToUpazilas[d]?.firstOrNull() ?: "Sadar"
                                        districtOpen = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(value = upazila, onValueChange = { upazila = it }, label = { Text("Upazila") }, singleLine = true)
                    OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("Area") }, singleLine = true)
                    OutlinedTextField(value = fullAddr, onValueChange = { fullAddr = it }, label = { Text("Full Address") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (fullName.isNotBlank() && phone.isNotBlank() && fullAddr.isNotBlank()) {
                        onSaveAddress(
                            DeliveryAddress(
                                label = label,
                                fullName = fullName,
                                phone = phone,
                                district = district,
                                upazila = upazila,
                                area = area,
                                fullAddress = fullAddr,
                                isDefault = addresses.isEmpty()
                            )
                        )
                        showAddAddressDialog = false
                    }
                }) {
                    Text("Save Address")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAddressDialog = false }) { Text("Cancel") }
            }
        )
    }
}
