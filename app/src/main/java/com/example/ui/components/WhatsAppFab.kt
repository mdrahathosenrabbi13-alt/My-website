package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.WhatsAppGreen

@Composable
fun WhatsAppSupportFab(
    phoneNumber: String,
    onUnavailable: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    FloatingActionButton(
        onClick = {
            val digits = phoneNumber.filter { it.isDigit() }
            val intl = if (digits.startsWith("0")) "88$digits" else digits
            val waUri = Uri.parse("https://wa.me/$intl?text=${Uri.encode("Hello Best Shoping BD, I need assistance with my order.")}")
            val intent = Intent(Intent.ACTION_VIEW, waUri)
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                onUnavailable("WhatsApp Support: $phoneNumber (Gaibandha, BD)")
            }
        },
        shape = CircleShape,
        containerColor = WhatsAppGreen,
        contentColor = Color.White,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
        modifier = modifier
            .size(56.dp)
            .testTag("whatsapp_support_fab")
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.Chat,
            contentDescription = "WhatsApp Support",
            modifier = Modifier.size(26.dp)
        )
    }
}
