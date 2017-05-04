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

import android.accounts.Account
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.content.ContextCompat
import eu.the4thfloor.msync.BuildConfig
import org.jetbrains.anko.accountManager
import eu.the4thfloor.msync.sync.lollipop.createSyncJobs as createSyncJobsLollipop
import eu.the4thfloor.msync.sync.prelollipop.createSyncJobs as createSyncJobsPreLollipop


inline fun doFromSdk(version: Int, f: () -> Unit, other: () -> Unit) {
    if (Build.VERSION.SDK_INT >= version)
        f()
    else
        other()
}


fun Context.checkSelfPermission(vararg permissions: String): Int =
    if (permissions.any { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_DENIED })
        PackageManager.PERMISSION_DENIED
    else
        PackageManager.PERMISSION_GRANTED


fun Context.hasAccount(): Boolean =
    getAccount() != null

fun Context.getAccount(): Account? =
    accountManager.getAccountsByType(BuildConfig.ACCOUNT_TYPE).firstOrNull()

fun Context.getRefreshToken(): String =
    accountManager.getPassword(getAccount())

fun Context.createAccount(accountName: String, refreshToken: String): Account {

    var account: Account? = getAccount()

    if (account != null) {
        return account
    }

    account = Account(accountName, BuildConfig.ACCOUNT_TYPE)
    accountManager.addAccountExplicitly(account, null, null)
    accountManager.setPassword(account, refreshToken)

    return account
}


fun Context.createSyncJobs(init: Boolean) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        createSyncJobsLollipop(init)
    } else {
        createSyncJobsPreLollipop(init)
    }
