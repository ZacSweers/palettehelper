package io.sweers.palettehelper

import android.content.Context
import android.widget.Toast

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
    clipboard.primaryClip = clip
}

/**
 * Copies given text to the clipboard and notifies via Toast
 */
public fun copyAndNotify(context: Context, hex: String) {
    copyToClipboard(context, hex)
    Toast.makeText(context, "Copied $hex to clipboard", Toast.LENGTH_SHORT).show();
}

/**
 * Gets the current text on the clipboard, if any
 */
public fun getClipData(context: Context) : CharSequence {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    var clip = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)
    if (clip == null) {
        clip = ""
    }
    return clip
}
