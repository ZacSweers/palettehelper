package io.sweers.palettehelper

import timber.log.Timber
import java.util.Deque
import java.util.ArrayDeque
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Error
import android.util.Log

/**
 * A logging implementation which buffers the last 200 messages and notifies on error exceptions.
 *
 * Borrowed from here: https://github.com/JakeWharton/Telecine/blob/master/telecine/src/main/java/com/jakewharton/telecine/BugsnagTree.java
 */
final class BugsnagTree: Timber.HollowTree() {

    class object {
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

    override public fun d(message: String, vararg args: Any) {
        logMessage(Log.DEBUG, message, args)
    }
    override public fun d(t: Throwable, message: String, vararg args: Any) {
        logMessage(Log.DEBUG, message, args)
    }
    override public fun i(message: String, vararg args: Any) {
        logMessage(Log.INFO, message, args)
    }
    override public fun i(t: Throwable, message: String, vararg args: Any) {
        logMessage(Log.INFO, message, args)
    }
    override public fun w(message: String, vararg args: Any) {
        logMessage(Log.WARN, message, args)
    }
    override public fun w(t: Throwable, message: String, vararg args: Any) {
        logMessage(Log.WARN, message, args)
    }
    override public fun e(message: String, vararg args: Any) {
        logMessage(Log.ERROR, message, args)
    }
    override public fun e(t: Throwable, message: String, vararg args: Any) {
        logMessage(Log.ERROR, message, args)
        Bugsnag.notify(t)
    }

    private fun logMessage(priority: Int, message: String, vararg args: Any) {
        var messageCopy = message
        if (args.size() > 0) {
            messageCopy = java.lang.String.format(messageCopy, args)
        }
        messageCopy = "${System.currentTimeMillis()} ${priorityToString(priority)} ${messageCopy}"
        synchronized (buffer) {
            buffer.addLast(messageCopy)
            if (buffer.size() > BUFFER_SIZE) {
                buffer.removeFirst()
            }
        }
    }

    public fun update(error: Error) {
        synchronized (buffer) {
            var i = 1
            for (message: String in buffer) {
                error.addToTab("Log", java.lang.String.format("%03d", i++), message)
            }
        }
    }
}