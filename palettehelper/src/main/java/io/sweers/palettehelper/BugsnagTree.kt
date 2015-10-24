package io.sweers.palettehelper

import android.util.Log
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Error
import timber.log.Timber
import java.util.ArrayDeque
import java.util.Deque

/**
 * A logging implementation which buffers the last 200 messages and notifies on error exceptions.
 *
 * Borrowed from here: https://github.com/JakeWharton/Telecine/blob/master/telecine/src/main/java/com/jakewharton/telecine/BugsnagTree.java
 */
final class BugsnagTree: Timber.Tree() {
    companion object {
        private val BUFFER_SIZE = 200

        private fun priorityToString(priority: Int): String {
            when (priority) {
                Log.ERROR -> return "E"
                Log.WARN -> return "W"
                Log.INFO -> return "I"
                Log.DEBUG -> return "D"
                else -> return java.lang.String.valueOf(priority)
            }
        }
    }

    // Adding one to the initial size accounts for the add before remove.
    private val buffer: Deque<String> = ArrayDeque(BUFFER_SIZE + 1)

    override fun log(priority: Int, tag: String?, message: String?, t: Throwable?) {
        var adjustedMessage = "${System.currentTimeMillis()} ${priorityToString(priority)} $message"
        synchronized (buffer) {
            buffer.addLast(adjustedMessage);
            if (buffer.size > BUFFER_SIZE) {
                buffer.removeFirst();
            }
        }
        if (t != null && priority == Log.ERROR) {
            Bugsnag.notify(t);
        }
    }

    public fun update(error: Error) {
        synchronized (buffer) {
            for ((i, message) in buffer.withIndex()) {
                error.addToTab("Log", java.lang.String.format("%03d", i + 1), message);
            }
        }
    }
}
