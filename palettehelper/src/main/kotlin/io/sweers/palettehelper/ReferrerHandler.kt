package io.sweers.palettehelper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.LinkedHashMap

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

        val params = splitQuery(referralInfo)

        if (params != null) {
            Timber.d("Params are: ${params}")
            PaletteHelperApplication.mixPanel.trackMisc(ANALYTICS_KEY_PLAY_REFERRER, params.get(KEY_CAMPAIGN))
            PaletteHelperApplication.mixPanel.trackMisc(ANALYTICS_KEY_PLAY_REFERRER, params.get(KEY_SOURCE))
            PaletteHelperApplication.mixPanel.trackMisc(ANALYTICS_KEY_PLAY_REFERRER, params.get(KEY_MEDIUM))

            if (params.containsKey(KEY_TERM)) {
                PaletteHelperApplication.mixPanel.trackMisc(ANALYTICS_KEY_PLAY_REFERRER, params.get(KEY_TERM))
            }

            if (params.containsKey(KEY_CONTENT)) {
                PaletteHelperApplication.mixPanel.trackMisc(ANALYTICS_KEY_PLAY_REFERRER, params.get(KEY_CONTENT))
            }
        }
    }

    /**
     * Utility function that tries to split query params into a Map
     *
     * @param query The URL query with unsplit params
     * @return A Map of the query param keys mapped to their values
     */
    private fun splitQuery(query: String): LinkedHashMap<String, String>? {
        val queryPairs = LinkedHashMap<String, String>()
        val pairs = query.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf('=')
            try {
                queryPairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"))
            } catch (e: UnsupportedEncodingException) {
                return null
            }

        }
        return queryPairs
    }

}