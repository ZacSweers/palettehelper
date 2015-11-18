package io.sweers.palettehelper

import android.app.ActivityManager
import android.content.Context
import android.support.v4.app.ActivityManagerCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.module.GlideModule

/**
 * Configure Glide to set desired image quality.
 */
class GlideConfiguration : GlideModule {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // Prefer higher quality images unless we're on a low RAM device
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        builder.setDecodeFormat(
                if (ActivityManagerCompat.isLowRamDevice(activityManager))
                    DecodeFormat.PREFER_RGB_565
                else
                    DecodeFormat.PREFER_ARGB_8888
        )
    }

    override fun registerComponents(context: Context, glide: Glide) {

    }
}
