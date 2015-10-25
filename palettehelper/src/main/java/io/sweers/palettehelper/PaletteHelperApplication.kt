package io.sweers.palettehelper

import android.app.Application
import com.bugsnag.android.BeforeNotify
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Error
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration
import com.squareup.leakcanary.LeakCanary
import com.squareup.okhttp.OkHttpClient
import timber.log.Timber
import kotlin.properties.Delegates

class PaletteHelperApplication: Application() {

    companion object {
        var mixPanel: MixpanelAPI by Delegates.notNull()
    }

    override fun onCreate() {
        super.onCreate()

        LeakCanary.install(this);

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Bugsnag.init(this, BuildConfig.BUGSNAG_KEY)
            Bugsnag.setReleaseStage(BuildConfig.BUILD_TYPE)
            Bugsnag.setProjectPackages("io.sweers.palettehelper")

            val tree = BugsnagTree()
            Bugsnag.getClient().beforeNotify(object : BeforeNotify {
                override fun run(error: Error): Boolean {
                    tree.update(error)
                    return true
                }
            })

            Timber.plant(tree)
        }

        setUpAnalytics()
        ImageLoader.getInstance().init(
                ImageLoaderConfiguration.Builder(this)
                        .imageDownloader(OkHttpImageDownloader(this, OkHttpClient()))
                        .build()
        );
    }

    public fun setUpAnalytics() {
        mixPanel = MixpanelAPI.getInstance(this, BuildConfig.ANALYTICS_KEY)
    }
}
