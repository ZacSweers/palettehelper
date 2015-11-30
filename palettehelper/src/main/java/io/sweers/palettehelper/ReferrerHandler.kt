package io.sweers.palettehelper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import io.sweers.palettehelper.util.ANALYTICS_KEY_PLAY_REFERRER
import io.sweers.palettehelper.util.trackMisc
import timber.log.Timber

/**
 * This class handles any referrer information passed back to the app after installation and
 * passes it up to analytics.
 *
 * [More info](https://developers.google.com/analytics/devguides/collection/android/v4/campaigns)
 */
public class ReferrerHandler: BroadcastReceiver() {

    companion object {
        val VENDING_REFERRED_INFO = "referrer"

        // Required params
        private val KEY_CAMPAIGN = "utm_campaign"
        private val KEY_SOURCE = "utm_source"
        private val KEY_MEDIUM = "utm_medium"

        // Optional params
        private val KEY_TERM = "utm_term"
        private val KEY_CONTENT = "utm_content"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val referralInfo = intent.getStringExtra(VENDING_REFERRED_INFO)

        if (referralInfo == null) {
            Timber.e(Exception(), "Referral info is null")
        }

        val queryUri = Uri.parse(referralInfo)

        if (queryUri != null) {
            Timber.d("Params are: ${queryUri.toString()}")
            var params = queryUri.getQueryParameterNames()
            logParamIfValid(params, queryUri, KEY_CAMPAIGN)
            logParamIfValid(params, queryUri, KEY_SOURCE)
            logParamIfValid(params, queryUri, KEY_MEDIUM)
            logParamIfValid(params, queryUri, KEY_TERM)
            logParamIfValid(params, queryUri, KEY_CONTENT)
        }
    }

    fun logParamIfValid(params: Set<String>, queryUri: Uri, key: String) {
        if (params.contains(key)) {
            PaletteHelperApplication.mixPanel.trackMisc(ANALYTICS_KEY_PLAY_REFERRER, queryUri.getQueryParameter(key))
        }
    }
}
