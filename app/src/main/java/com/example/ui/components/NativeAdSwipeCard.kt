package com.example.ui.components

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.ui.theme.CoM8ThemeTokens
import com.example.ui.theme.InterFontFamily
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

const val COM8_SWIPE_NATIVE_AD_UNIT_ID = "ca-app-pub-3013444933880597/7405830145"

@Stable
class SwipeDeckNativeAdController(
    private val loadNext: () -> Unit
) {
    var currentAd by mutableStateOf<NativeAd?>(null)
        internal set

    var isLoading by mutableStateOf(false)
        internal set

    var loadRequestTrigger by mutableIntStateOf(0)
        private set

    fun consumeAndPreloadNext() {
        currentAd?.destroy()
        currentAd = null
        loadRequestTrigger += 1
        loadNext()
    }
}

@Composable
fun rememberSwipeDeckNativeAdController(
    adUnitId: String = COM8_SWIPE_NATIVE_AD_UNIT_ID
): SwipeDeckNativeAdController {
    val context = LocalContext.current
    var disposed by remember { mutableStateOf(false) }
    lateinit var controller: SwipeDeckNativeAdController

    val triggerLoad = remember(context, adUnitId) {
        {
            if (!disposed && !controller.isLoading && controller.currentAd == null) {
                controller.isLoading = true
                runCatching {
                    val loader = AdLoader.Builder(context.applicationContext, adUnitId)
                        .forNativeAd { ad ->
                            if (disposed) {
                                ad.destroy()
                            } else {
                                controller.currentAd?.destroy()
                                controller.currentAd = ad
                                controller.isLoading = false
                            }
                        }
                        .withAdListener(object : AdListener() {
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                controller.isLoading = false
                            }
                        })
                        .withNativeAdOptions(
                            NativeAdOptions.Builder()
                                .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                                .build()
                        )
                        .build()
                    loader.loadAd(AdRequest.Builder().build())
                }.onFailure {
                    controller.isLoading = false
                }
            }
        }
    }

    controller = remember(triggerLoad) {
        SwipeDeckNativeAdController(loadNext = triggerLoad)
    }

    DisposableEffect(controller) {
        disposed = false
        triggerLoad()
        onDispose {
            disposed = true
            controller.currentAd?.destroy()
            controller.currentAd = null
        }
    }

    return controller
}

@Composable
fun NativeAdDeckCard(
    nativeAd: NativeAd,
    mediaHeight: Dp,
    onDismissAd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = CoM8ThemeTokens.colors
    val headlineColor = tokens.onSurface.toArgb()
    val mutedColor = tokens.onSurfaceMuted.toArgb()
    val mediaBgColor = (if (tokens.isDark) tokens.pastelMint else Color(0xFFCFE3DE)).toArgb()
    val targetMediaPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        mediaHeight.coerceIn(220.dp, 320.dp).roundToPx()
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .testTag("native_ad_swipe_card"),
        shape = RoundedCornerShape(24.dp),
        color = tokens.surface,
        border = BorderStroke(1.dp, tokens.border),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    val inflater = LayoutInflater.from(ctx)
                    val adView = inflater.inflate(
                        R.layout.layout_native_ad_swipe_card,
                        null,
                        false
                    ) as NativeAdView
                    bindNativeAdToView(
                        adView = adView,
                        nativeAd = nativeAd,
                        mediaHeightPx = targetMediaPx,
                        mediaBgColor = mediaBgColor,
                        headlineColor = headlineColor,
                        mutedColor = mutedColor
                    )
                    adView
                },
                update = { adView ->
                    val mediaContainer = adView.findViewById<View>(R.id.ad_media_container)
                    if (mediaContainer != null && mediaContainer.layoutParams?.height != targetMediaPx) {
                        mediaContainer.layoutParams = mediaContainer.layoutParams?.apply {
                            height = targetMediaPx
                        }
                    }
                    adView.findViewById<TextView>(R.id.ad_headline)?.setTextColor(headlineColor)
                    adView.findViewById<TextView>(R.id.ad_advertiser)?.setTextColor(mutedColor)
                    adView.findViewById<TextView>(R.id.ad_body)?.setTextColor(mutedColor)
                    mediaContainer?.setBackgroundColor(mediaBgColor)

                    if (adView.tag !== nativeAd) {
                        bindNativeAdToView(
                            adView = adView,
                            nativeAd = nativeAd,
                            mediaHeightPx = targetMediaPx,
                            mediaBgColor = mediaBgColor,
                            headlineColor = headlineColor,
                            mutedColor = mutedColor
                        )
                    }
                }
            )

            HorizontalDivider(thickness = 1.dp, color = tokens.border)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tokens.surface)
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onDismissAd)
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .testTag("skip_native_ad_button"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sponsored card",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = tokens.onSurfaceMuted
                )
                Text(
                    text = "Skip ad →",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = tokens.primary
                )
            }
        }
    }
}

private fun bindNativeAdToView(
    adView: NativeAdView,
    nativeAd: NativeAd,
    mediaHeightPx: Int,
    mediaBgColor: Int,
    headlineColor: Int,
    mutedColor: Int
) {
    val mediaContainer = adView.findViewById<View>(R.id.ad_media_container)
    if (mediaContainer != null) {
        mediaContainer.setBackgroundColor(mediaBgColor)
        mediaContainer.layoutParams?.let { params ->
            params.height = mediaHeightPx
            mediaContainer.layoutParams = params
        }
    }

    val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
    val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
    val advertiserView = adView.findViewById<TextView>(R.id.ad_advertiser)
    val bodyView = adView.findViewById<TextView>(R.id.ad_body)
    val iconView = adView.findViewById<ImageView>(R.id.ad_app_icon)
    val ctaView = adView.findViewById<Button>(R.id.ad_call_to_action)

    adView.mediaView = mediaView
    adView.headlineView = headlineView
    adView.advertiserView = advertiserView
    adView.bodyView = bodyView
    adView.iconView = iconView
    adView.callToActionView = ctaView

    mediaView?.mediaContent = nativeAd.mediaContent

    headlineView?.text = nativeAd.headline
    headlineView?.setTextColor(headlineColor)

    val advertiserText = nativeAd.advertiser ?: nativeAd.store ?: "Sponsored"
    advertiserView?.text = advertiserText
    advertiserView?.setTextColor(mutedColor)

    if (nativeAd.body.isNullOrBlank()) {
        bodyView?.visibility = View.GONE
    } else {
        bodyView?.visibility = View.VISIBLE
        bodyView?.text = nativeAd.body
        bodyView?.setTextColor(mutedColor)
    }

    val iconDrawable = nativeAd.icon?.drawable
    if (iconDrawable != null) {
        iconView?.setImageDrawable(iconDrawable)
        iconView?.visibility = View.VISIBLE
    } else {
        iconView?.visibility = View.GONE
    }

    if (nativeAd.callToAction.isNullOrBlank()) {
        ctaView?.visibility = View.GONE
    } else {
        ctaView?.visibility = View.VISIBLE
        ctaView?.text = nativeAd.callToAction
    }

    adView.tag = nativeAd
    adView.setNativeAd(nativeAd)
}
