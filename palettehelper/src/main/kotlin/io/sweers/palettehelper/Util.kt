package io.sweers.palettehelper

import android.content.Context
import android.widget.Toast
import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONObject

/**
 * Converts a given color to a #xxxxxx string.
 */
public fun rgbToHex(color: Int): String = "#${Integer.toHexString(color)}"

/**
 * Copies a given text to the clipboard
 */
public fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText("Copied Text", text)
    clipboard.setPrimaryClip(clip)
}

public fun copyAndNotify(context: Context, hex: String) {
    copyToClipboard(context, hex)
    Toast.makeText(context, "Copied ${hex} to clipboard", Toast.LENGTH_SHORT).show();
}