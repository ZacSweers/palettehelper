package io.sweers.palettehelper

import android.app.Application
import timber.log.Timber
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Error
import com.bugsnag.android.BeforeNotify
import com.google.android.gms.analytics.GoogleAnalytics
import kotlin.properties.Delegates
import com.google.android.gms.analytics.Tracker
import com.mixpanel.android.mpmetrics.MixpanelAPI

class PaletteHelperApplication: Application() {

    class object {
        var mixPanel: MixpanelAPI by Delegates.notNull()
    }

    override fun onCreate() {
        super.onCreate()

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
    }

    public fun setUpAnalytics() {
        mixPanel = MixpanelAPI.getInstance(this, BuildConfig.ANALYTICS_KEY)
    }
}