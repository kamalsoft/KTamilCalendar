package com.kamalsoft.tamilcalendar.tv.domain.model

enum class UnitsPref(val ta: String, val en: String) { AUTO("தானியங்கி", "Auto"), IMPERIAL("°F · mph", "°F · mph"), METRIC("°C · km/h", "°C · km/h") }

enum class ConventionPref(val ta: String, val en: String) {
    AUTO("தானியங்கி", "Auto (6 AM in India, sunrise elsewhere)"),
    TRADITIONAL_6AM("காலை 6 மணி", "Fixed 6 AM – 6 PM"),
    SUNRISE_BASED("சூரிய உதயம்", "Local sunrise – sunset"),
}

/** User-toggleable features. Defaults = everything on, values chosen automatically. */
data class AppSettings(
    /** Destination names hidden from the tab bar (Today, Location, Theme, Settings, About can't be hidden). */
    val hiddenSections: Set<String> = emptySet(),
    val weather: Boolean = true,
    /** Faiths whose observances are hidden. Hindu observances are the calendar itself and always shown. */
    val hiddenFaiths: Set<Religion> = emptySet(),
    val usHolidays: Boolean = true,
    val schoolCalendars: Boolean = true,
    val tamilNaduHolidays: Boolean = true,
    val animations: Boolean = true,
    val weatherAnimations: Boolean = true,
    val clock24h: Boolean = false,
    val tamilOnly: Boolean = false,
    val units: UnitsPref = UnitsPref.AUTO,
    val convention: ConventionPref = ConventionPref.AUTO,
    /** Full-screen ambient clock after [ambientMinutes] without input. */
    val ambient: Boolean = true,
    val ambientMinutes: Int = 2,
    /** Reminder 10 minutes before Rahu Kalam starts. */
    val rahuReminder: Boolean = false,
    /** Reminder when each Nalla Neram window begins. */
    val nallaReminder: Boolean = false,
    /** Synthesised tanpura/bell background under the daily sloka. */
    val slokaMusic: Boolean = true,
    /** Also read the sloka aloud (text-to-speech). Off: music only. */
    val slokaVoice: Boolean = false,
    val music: MusicStyle = MusicStyle(),
)

val AMBIENT_MINUTE_CHOICES = listOf(1, 2, 5, 10)

/** Whether [this] event should be shown under [s]. Local days off stay visible even if their faith is hidden. */
fun CalendarEvent.isVisible(s: AppSettings): Boolean {
    if (religion != null && religion != Religion.HINDU && religion in s.hiddenFaiths && !isLocalHoliday) return false
    return when (region) {
        HolidayRegion.US_FEDERAL, HolidayRegion.ILLINOIS -> s.usHolidays
        HolidayRegion.SCHOOL_D203, HolidayRegion.SCHOOL_D204 -> s.schoolCalendars
        // Tamil Nadu *government* holidays only; Hindu festivals marked as TN holidays stay as festivals.
        HolidayRegion.TAMIL_NADU -> s.tamilNaduHolidays || kind != EventKind.GOVT_HOLIDAY
        null -> true
    }
}

/** The day with events filtered by [s], and holiday/school tags recomputed from what remains. */
fun TamilDayData.applying(s: AppSettings): TamilDayData {
    val kept = events.filter { it.isVisible(s) }
    if (kept.size == events.size) return this
    val tags = tags.toMutableSet()
    if (kept.none { it.isLocalHoliday }) tags -= DayTag.GOVT_HOLIDAY
    if (kept.none { it.kind == EventKind.SCHOOL && it.isHoliday }) tags -= DayTag.SCHOOL_HOLIDAY
    return copy(events = kept, tags = tags)
}
