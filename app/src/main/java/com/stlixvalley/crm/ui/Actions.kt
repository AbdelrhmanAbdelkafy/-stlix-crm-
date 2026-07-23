package com.stlixvalley.crm.ui

import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Free, phone-native record actions shared by the list and detail screens:
 * WhatsApp (wa.me deep-link), email (mailto), and add-to-calendar. No paid APIs.
 */
object Actions {

    /** First field that looks like a real phone number, or null. */
    fun firstPhone(rec: JSONObject): String? =
        listOf("mobile", "phone", "homephone", "otherphone")
            .map { rec.optString(it) }
            .firstOrNull { s -> s.count { it.isDigit() } >= 8 }

    /** First field that looks like an email address, or null. */
    fun firstEmail(rec: JSONObject): String? =
        listOf("email", "secondaryemail", "email1", "email2")
            .map { rec.optString(it) }
            .firstOrNull { it.contains("@") && it.contains(".") }

    /** Egypt-friendly: keep digits, drop a 00 prefix, turn a leading 0 into 20. */
    fun normalizeNumber(raw: String): String {
        var d = raw.filter { it.isDigit() }
        if (d.startsWith("00")) d = d.drop(2)
        if (d.startsWith("0")) d = "20" + d.drop(1)
        return d
    }

    fun whatsApp(rawNumber: String, greeting: String): Intent {
        val url = "https://wa.me/${normalizeNumber(rawNumber)}?text=" + Uri.encode(greeting)
        return Intent(Intent.ACTION_VIEW, Uri.parse(url))
    }

    fun email(address: String): Intent =
        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address"))

    fun calendar(title: String, description: String, dateStart: String): Intent {
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, title)
            .putExtra(CalendarContract.Events.DESCRIPTION, description)
        parseMillis(dateStart)?.let { start ->
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + 3_600_000L)
        }
        return intent
    }

    private fun parseMillis(date: String): Long? =
        runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)?.time }.getOrNull()
}
