package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.R

fun resolveDrawableId(name: String): Int? = when (name) {
    "img_prod_saree" -> R.drawable.img_prod_saree
    "img_prod_headphones" -> R.drawable.img_prod_headphones
    "img_prod_handbag" -> R.drawable.img_prod_handbag
    "img_prod_watch" -> R.drawable.img_prod_watch
    "img_prod_sneakers" -> R.drawable.img_prod_sneakers
    "img_prod_skincare" -> R.drawable.img_prod_skincare
    "img_prod_smartphone" -> R.drawable.img_prod_smartphone
    "img_prod_earbuds" -> R.drawable.img_prod_earbuds
    "img_hero_fashion" -> R.drawable.img_hero_fashion
    "img_hero_tech" -> R.drawable.img_hero_tech
    "img_lifestyle_pack" -> R.drawable.img_lifestyle_pack
    "img_onboarding_delivery" -> R.drawable.img_onboarding_delivery
    "img_app_icon_fg" -> R.drawable.img_app_icon_fg
    else -> null
}

val BUNDLED_PRODUCT_IMAGES = listOf(
    "img_prod_saree" to "Jamdani Saree",
    "img_prod_headphones" to "ANC Headphones",
    "img_prod_handbag" to "Leather Handbag",
    "img_prod_watch" to "Smartwatch",
    "img_prod_sneakers" to "Court Sneakers",
    "img_prod_skincare" to "Glow Serum",
    "img_prod_smartphone" to "5G Smartphone",
    "img_prod_earbuds" to "TWS Earbuds"
)

@Composable
fun ProductImage(
    imageResName: String,
    imageUrl: String = "",
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val resId = resolveDrawableId(imageResName)
    when {
        imageUrl.startsWith("http", ignoreCase = true) -> {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
                error = resId?.let { painterResource(it) }
            )
        }
        resId != null -> {
            Image(
                painter = painterResource(id = resId),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        else -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ShoppingBag,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                )
            }
        }
    }
}
