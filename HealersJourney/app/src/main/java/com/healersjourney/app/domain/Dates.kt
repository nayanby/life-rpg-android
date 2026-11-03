package com.healersjourney.app.domain

import java.util.Calendar

object Dates {

    @Volatile var resetHour: Int = 5

    fun dateKeyOf(cal: Calendar): String {
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(y, m, d)
    }

    fun todayKey(): String = dateKeyOf(Calendar.getInstance())

    fun keyToCal(key: String): Calendar? {
        val parts = key.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size != 3) return null
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(parts[0], parts[1] - 1, parts[2])
        return cal
    }

    /** Before the configured reset hour, activities still belong to the previous game day. */
    fun gameDayKey(now: Calendar = Calendar.getInstance()): String {
        val cal = now.clone() as Calendar
        if (cal.get(Calendar.HOUR_OF_DAY) < resetHour) cal.add(Calendar.DAY_OF_MONTH, -1)
        return dateKeyOf(cal)
    }

    fun studyDateKey(now: Calendar = Calendar.getInstance()): String = gameDayKey(now)

    /** Milliseconds until the configured rollover. Name retained for existing callers. */
    fun millisUntilFiveAm(nowMillis: Long = System.currentTimeMillis()): Long {
        val target = Calendar.getInstance()
        target.timeInMillis = nowMillis
        target.set(Calendar.HOUR_OF_DAY, resetHour)
        target.set(Calendar.MINUTE, 0)
        target.set(Calendar.SECOND, 0)
        target.set(Calendar.MILLISECOND, 0)
        if (target.timeInMillis <= nowMillis) target.add(Calendar.DAY_OF_MONTH, 1)
        return target.timeInMillis - nowMillis
    }

    /** "6h 12m" style label, or "45m" when under an hour. */
    fun countdownText(millis: Long): String {
        val totalMinutes = (millis.coerceAtLeast(0) / 60_000L).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    fun previousNightKey(key: String): String {
        val cal = keyToCal(key) ?: return key
        cal.add(Calendar.DAY_OF_MONTH, -1)
        return dateKeyOf(cal)
    }

    fun previousNightKeyOfNow(): String = previousNightKey(todayKey())

    fun addDays(key: String, n: Int): String {
        val cal = keyToCal(key) ?: return key
        cal.add(Calendar.DAY_OF_MONTH, n)
        return dateKeyOf(cal)
    }

    fun weekKeyOf(key: String): String {
        val cal = keyToCal(key) ?: return key
        val day = cal.get(Calendar.DAY_OF_WEEK)
        val mondayOffset = if (day == Calendar.SUNDAY) -6 else Calendar.MONDAY - day
        cal.add(Calendar.DAY_OF_MONTH, mondayOffset)
        return dateKeyOf(cal)
    }

    fun weekKeyOfNow(): String = weekKeyOf(todayKey())

    fun monthKeyOf(key: String): String = key.take(7)

    fun monthKeyOfNow(): String = todayKey().take(7)

    /** The 7 day-keys of the current week, Monday first. */
    fun currentWeekKeys(): List<String> {
        val monday = weekKeyOfNow()
        return (0 until 7).map { addDays(monday, it) }
    }

    fun daysBetween(laterKey: String, earlierKey: String): Int {
        val later = keyToCal(laterKey) ?: return 999
        val earlier = keyToCal(earlierKey) ?: return 999
        val millis = later.timeInMillis - earlier.timeInMillis
        return maxOf(0, (millis / 86_400_000L).toInt())
    }

    fun yearOfNow(): Int = Calendar.getInstance().get(Calendar.YEAR)

    fun monthOfNowZeroBased(): Int = Calendar.getInstance().get(Calendar.MONTH)

    fun daysInMonth(year: Int, monthZeroBased: Int): Int {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(year, monthZeroBased, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun mondayFirstOffset(year: Int, monthZeroBased: Int): Int {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(year, monthZeroBased, 1)
        return (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    }

    val MONTH_NAMES = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val MONTH_SHORT = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    fun monthName(monthNumber: Int): String =
        MONTH_NAMES.getOrElse(monthNumber - 1) { "" }

    /** "12th September" style label used by the to-do day heading. */
    fun todoDisplayDate(key: String): String {
        val cal = keyToCal(key) ?: return key
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val suffix = when {
            day % 10 == 1 && day % 100 != 11 -> "st"
            day % 10 == 2 && day % 100 != 12 -> "nd"
            day % 10 == 3 && day % 100 != 13 -> "rd"
            else -> "th"
        }
        return "$day$suffix ${MONTH_NAMES[cal.get(Calendar.MONTH)]}"
    }

    /** "12 Sep 25" style label used for milestone completion dates. */
    fun formatMilestoneDate(dateString: String): String {
        if (dateString.isEmpty()) return "Not completed yet"
        val parts = dateString.split("-")
        if (parts.size != 3) return dateString
        val year = parts[0].takeLast(2)
        val month = MONTH_SHORT.getOrElse((parts[1].toIntOrNull() ?: 1) - 1) { "" }
        return "${parts[2].toIntOrNull() ?: 0} $month $year"
    }

    /** "12 Sep 2025" style label used for personal-care dates. */
    fun prettyCareDate(key: String): String {
        if (key.isEmpty()) return "Not recorded"
        val cal = keyToCal(key) ?: return key
        val day = "%02d".format(cal.get(Calendar.DAY_OF_MONTH))
        return "$day ${MONTH_SHORT[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"
    }

    /** "12 Sep 2025, 03:45 pm" style timestamp matching the web app's en-IN format. */
    fun nowText(): String {
        val cal = Calendar.getInstance()
        val day = "%02d".format(cal.get(Calendar.DAY_OF_MONTH))
        val month = MONTH_SHORT[cal.get(Calendar.MONTH)]
        val year = cal.get(Calendar.YEAR)
        var hour = cal.get(Calendar.HOUR_OF_DAY)
        val ampm = if (hour < 12) "am" else "pm"
        hour = if (hour % 12 == 0) 12 else hour % 12
        val minute = "%02d".format(cal.get(Calendar.MINUTE))
        return "$day $month $year, %02d:$minute $ampm".format(hour)
    }

    /** The date-only prefix of [nowText], e.g. "10 Sep 2025". */
    fun todayLabel(): String = nowText().substringBefore(",")
}
