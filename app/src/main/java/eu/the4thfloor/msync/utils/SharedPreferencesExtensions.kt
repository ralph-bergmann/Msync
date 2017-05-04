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


import android.content.SharedPreferences

/**
 * usage:
 * {@code defaultSharedPreferences.apply("ACCESS_TOKEN" to "accessToken", "NAME" to "name")}
 */
fun SharedPreferences.apply(vararg pairs: Pair<String, Any?>) {
    edit().apply {
        pairs.forEach {
            when (it.second) {
                null       -> remove(it.first)
                is Boolean -> putBoolean(it.first, it.second as Boolean)
                is Float   -> putFloat(it.first, it.second as Float)
                is Int     -> putInt(it.first, it.second as Int)
                is Long    -> putLong(it.first, it.second as Long)
                is String  -> putString(it.first, it.second as String)
            }
        }
    }.apply()
}
