/*
 * Copyright 2017 Ralph Bergmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
