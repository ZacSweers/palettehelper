package io.sweers.palettehelper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.net.URLDecoder
import java.io.UnsupportedEncodingException
import java.util.LinkedHashMap
import timber.log.Timber

/**
 * Created by hsweers on 2/19/15.
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
     * Utility function that splits query params into a hashmap
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