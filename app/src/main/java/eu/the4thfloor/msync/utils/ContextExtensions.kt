package eu.the4thfloor.msync.utils

import android.Manifest
import android.accounts.Account
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars.ACCOUNT_NAME
import android.provider.CalendarContract.Calendars.ACCOUNT_TYPE
import android.provider.CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
import android.provider.CalendarContract.Calendars.CONTENT_URI
import android.provider.CalendarContract.Calendars._ID
import android.support.annotation.RequiresPermission
import android.support.v4.content.ContextCompat
import android.text.format.Time
import eu.the4thfloor.msync.BuildConfig
import eu.the4thfloor.msync.R
import org.jetbrains.anko.accountManager
import org.jetbrains.anko.defaultSharedPreferences

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

fun Context.getAccount(): Account? {

    val accounts = accountManager.getAccountsByType(BuildConfig.ACCOUNT_TYPE)

    if (!accounts.isEmpty()) {
        return accounts[0]
    } else {
        return null
    }
}

fun Context.createAccount(accountName: String, refreshToken: String): Account {

    var account: Account? = getAccount()

    if (account != null) {
        return account
    }

    account = Account(accountName, BuildConfig.ACCOUNT_TYPE)
    accountManager.addAccountExplicitly(account, null, null)
    accountManager.setAuthToken(account, BuildConfig.AUTH_TOKEN_TYPE, refreshToken)

    return account
}


@RequiresPermission(allOf = arrayOf(Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.WRITE_CALENDAR))
fun Context.getCalendarID(account: Account): Long? {

    val calendarCursor =
        contentResolver.query(CONTENT_URI,
                              arrayOf(_ID),
                              "$ACCOUNT_NAME = ? AND $ACCOUNT_TYPE = ?",
                              arrayOf(account.name, account.type),
                              null)

    if (calendarCursor.count <= 0) {
        calendarCursor.close()
        return null
    } else {
        calendarCursor.moveToFirst()
        val id = calendarCursor.getLong(calendarCursor.getColumnIndex(_ID))
        calendarCursor.close()
        return id
    }
}

@RequiresPermission(allOf = arrayOf(Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.WRITE_CALENDAR))
fun Context.createCalendar(account: Account): Long {

    val calendarName = defaultSharedPreferences.getString(getString(R.string.pref_key_calendar_name),
                                                          getString(R.string.pref_default_calendar_name))
    val calendarColor = defaultSharedPreferences.getInt(getString(R.string.pref_key_calendar_color),
                                                        resources.getColor(R.color.red))

    val values = ContentValues()
    values.put(CalendarContract.Calendars.NAME, calendarName)
    values.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, calendarName)
    values.put(CalendarContract.Calendars.CALENDAR_COLOR, calendarColor)
    values.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
    values.put(CalendarContract.Calendars.OWNER_ACCOUNT, account.name)
    values.put(CalendarContract.Calendars.ACCOUNT_NAME, account.name)
    values.put(CalendarContract.Calendars.ACCOUNT_TYPE, account.type)
    values.put(CalendarContract.Calendars.SYNC_EVENTS, 1)
    values.put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, Time.getCurrentTimezone())

    return ContentUris.parseId(contentResolver.insert(contentUri(CalendarContract.Calendars.CONTENT_URI, account), values))
}

private fun contentUri(uri: Uri, account: Account): Uri {
    return uri.buildUpon()
        .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, account.name)
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, account.type).build()
}
