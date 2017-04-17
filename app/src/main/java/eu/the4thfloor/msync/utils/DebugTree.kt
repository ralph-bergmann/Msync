package eu.the4thfloor.msync.utils

import android.os.Looper
import timber.log.Timber
import java.util.regex.Pattern

class DebugTree(private val tag: String) : Timber.DebugTree() {

    override fun log(priority: Int, ignored: String?, message: String, throwable: Throwable?) {
        super.log(priority,
                  tag,
                  StringBuilder()
                      .append(createPrefix())
                      .append("  ")
                      .append(message)
                      .toString(),
                  throwable)
    }

    companion object {

        private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")

        private fun createPrefix(): String {

            val stackTraces = Throwable().stackTrace
            if (stackTraces.size < 8) {
                throw IllegalStateException("Synthetic stacktrace didn't have enough elements: are you using proguard?")
            }

            val stackTrace = stackTraces[7]
            var className = stackTrace.className
            val m = ANONYMOUS_CLASS.matcher(className)
            if (m.find()) {
                className = m.replaceAll("")
            }
            className = className.substring(className.lastIndexOf('.') + 1)

            return StringBuilder().apply {
                append(className)
                append('.')
                append(stackTrace.methodName)
                append("(")
                append(stackTrace.fileName)
                append(':')
                append(stackTrace.lineNumber)
                append(')')

                if (Looper.myLooper() != Looper.getMainLooper()) {
                    append(" [Thread: ")
                    append(Thread.currentThread().name)
                    append(']')
                }

            }.toString()
        }
    }
}
