package com.example.core

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class JalaliDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
)

object JalaliCalendar {

    private val jalaliMonthsFa = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    private val jalaliMonthsEn = listOf(
        "Farvardin", "Ordibehesht", "Khordad",
        "Tir", "Mordad", "Shahrivar",
        "Mehr", "Aban", "Azar",
        "Dey", "Bahman", "Esfand"
    )

    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun toPersianDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun localizeDigits(input: String, usePersianDigits: Boolean): String =
        if (usePersianDigits) toPersianDigits(input) else input

    fun gregorianToJalali(gYear: Int, gMonth: Int, gDay: Int, hour: Int = 0, minute: Int = 0): JalaliDate {
        val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        var gy2 = if (gMonth > 2) gYear + 1 else gYear
        var days = 355666 + (365 * gYear) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gDay + gDaysInMonth[gMonth - 1]
        var jy = -1595 + 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + (days / 31)
            jd = 1 + (days % 31)
        } else {
            jm = 7 + ((days - 186) / 30)
            jd = 1 + ((days - 186) % 30)
        }
        return JalaliDate(jy, jm, jd, hour, minute)
    }

    fun fromTimestamp(millis: Long): JalaliDate {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        return gregorianToJalali(
            gYear = cal.get(Calendar.YEAR),
            gMonth = cal.get(Calendar.MONTH) + 1,
            gDay = cal.get(Calendar.DAY_OF_MONTH),
            hour = cal.get(Calendar.HOUR_OF_DAY),
            minute = cal.get(Calendar.MINUTE),
        )
    }

    fun formatDateTime(
        millis: Long,
        useJalali: Boolean,
        isPersian: Boolean,
        usePersianDigits: Boolean,
        includeTime: Boolean = true,
    ): String {
        val raw = if (useJalali) {
            val j = fromTimestamp(millis)
            val monthName = if (isPersian) jalaliMonthsFa[j.month - 1] else jalaliMonthsEn[j.month - 1]
            val timePart = if (includeTime) {
                String.format(Locale.US, " • %02d:%02d", j.hour, j.minute)
            } else {
                ""
            }
            "${j.day} $monthName ${j.year}$timePart"
        } else {
            val pattern = if (includeTime) "yyyy/MM/dd • HH:mm" else "yyyy/MM/dd"
            SimpleDateFormat(pattern, Locale.US).format(Date(millis))
        }
        return localizeDigits(raw, usePersianDigits && isPersian)
    }
}
