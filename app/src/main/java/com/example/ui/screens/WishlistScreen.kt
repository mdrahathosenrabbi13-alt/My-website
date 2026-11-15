package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.data.model.WishlistItem
import com.example.localization.AppStrings
import com.example.localization.formatBdt
import com.example.ui.components.ProductImage

@Composable
fun WishlistScreen(
    strings: AppStrings,
    wishlistItems: List<WishlistItem>,
    products: List<Product>,
    onOpenProduct: (Product) -> Unit,
    onMoveToCart: (WishlistItem) -> Unit,
    onRemoveFromWishlist: (Product) -> Unit,
    onBrowseShop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${strings.wishlist} (${wishlistItems.size})",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }

        if (wishlistItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (strings.langCode == "bn") "আপনার উইশলিস্ট খালি" else "No saved items yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = onBrowseShop, shape = RoundedCornerShape(14.dp)) {
                        Text(strings.shopNow)
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(wishlistItems, key = { it.productId }) { item ->
                    val matchingProduct = products.firstOrNull { it.id == item.productId }
                    val name = if (strings.langCode == "bn" && item.productNameBn.isNotBlank()) item.productNameBn else item.productName
                    val available = matchingProduct?.let { it.stock > 0L } ?: item.inStock

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = matchingProduct != null) {
                                matchingProduct?.let(onOpenProduct)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProductImage(
                                imageResName = item.imageResName,
                                imageUrl = item.imageUrl,
                                contentDescription = name,
                                modifier = Modifier
                                    .size(80.dp)
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
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = formatBdt(matchingProduct?.effectivePrice ?: item.price),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (item.regularPrice > item.price) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = formatBdt(item.regularPrice),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textDecoration = TextDecoration.LineThrough
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (available) strings.inStock else strings.outOfStock,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (available) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                IconButton(
                                    onClick = {
                                        onRemoveFromWishlist(
                                            matchingProduct ?: Product(id = item.productId, name = item.productName)
                                        )
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Remove from wishlist",
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                IconButton(
                                    onClick = { onMoveToCart(item) },
                                    enabled = available,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AddShoppingCart,
                                        contentDescription = "Move to cart",
                                        tint = MaterialTheme.colorScheme.primary
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
