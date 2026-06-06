package com.drpsphca.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.huawei.hms.ads.AdParam
import com.huawei.hms.ads.BannerAdSize
import com.huawei.hms.ads.HwAds
import com.huawei.hms.ads.banner.BannerView
import android.content.Context

fun initAds(context: Context) {
    HwAds.init(context)
}

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            BannerView(context).apply {
                adId = BuildConfig.ADS_UNIT_ID
                bannerAdSize = BannerAdSize.BANNER_SIZE_320_50
                loadAd(AdParam.Builder().build())
            }
        }
    )
}
