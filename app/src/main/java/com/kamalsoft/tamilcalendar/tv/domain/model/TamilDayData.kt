package com.kamalsoft.tamilcalendar.tv.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Everything the TV shows for one civil (midnight-to-midnight, in the configured location's time zone) day. */
data class TamilDayData(
    val date: LocalDate,
    val tamilDate: TamilDate,
    val sunrise: LocalTime,
    val sunset: LocalTime,
    val panchangam: Panchangam,
    val timings: DayTimings,
    val chandrashtamam: Chandrashtamam,
    val tags: Set<DayTag>,
    val events: List<CalendarEvent>,
) {
    val weekdayIndex: Int get() = date.dayOfWeek.value % 7
    val weekdayTa: String get() = TamilNames.weekdays[weekdayIndex].first
    val weekdayEn: String get() = TamilNames.weekdays[weekdayIndex].second
    val isGovernmentHoliday: Boolean get() = DayTag.GOVT_HOLIDAY in tags

    /** Faiths (other than the day's Hindu observances) with an observance on this day. */
    val otherReligions: Set<Religion> get() = events.mapNotNull { it.religion }.filter { it != Religion.HINDU }.toSet()

    /**
     * The one item a month-grid cell has room for: a local holiday, else a Hindu festival, else another
     * faith's observance, else a deity observance (Sashti, Pradosham, …). School days don't qualify.
     */
    val headline: Pair<String, String>?
        get() {
            val e = events.filter { it.kind != EventKind.SCHOOL && it.kind != EventKind.MUHURTHAM }
                .minByOrNull {
                    when {
                        it.isLocalHoliday -> 0
                        it.religion == Religion.HINDU -> 1
                        it.religion != null -> 2
                        else -> 3
                    }
                }
            if (e != null) return e.titleTa to e.titleEn
            val tag = tags.filter { it.deity != null && it != DayTag.POURNAMI }.minByOrNull { it.ordinal }
                ?: tags.firstOrNull { it == DayTag.AMAVASAI || it == DayTag.POURNAMI }
            return tag?.let { it.ta to it.en }
        }
}

data class TamilDate(
    /** 0 = Chithirai … 11 = Panguni. */
    val monthIndex: Int,
    val day: Int,
    /** 0..59 position in the 60-year cycle. */
    val yearCycleIndex: Int,
    val thiruvalluvarAandu: Int,
) {
    val monthTa: String get() = TamilNames.months[monthIndex].first
    val monthEn: String get() = TamilNames.months[monthIndex].second
    val yearTa: String get() = TamilNames.years[yearCycleIndex].first
    val yearEn: String get() = TamilNames.years[yearCycleIndex].second

    /** e.g. "ஐப்பசி 9" */
    val displayTa: String get() = "$monthTa $day"
    /** e.g. "பராபவ வருடம்" */
    val yearDisplayTa: String get() = "$yearTa வருடம்"
}

/** A panchangam limb (tithi / nakshatram / yogam) as it stands at sunrise, plus when it ends. */
data class PanchangaElement(
    val index: Int,
    val nameTa: String,
    val nameEn: String,
    /** Null when the element runs past the next sunrise window we searched. */
    val endsAt: LocalDateTime?,
)

enum class Paksha(val ta: String, val en: String) {
    SHUKLA("வளர்பிறை", "Shukla Paksha"),
    KRISHNA("தேய்பிறை", "Krishna Paksha"),
}

data class Panchangam(
    /** index 0..29: 0–14 Shukla (14 = Pournami), 15–29 Krishna (29 = Amavasai). */
    val tithi: PanchangaElement,
    val nakshatram: PanchangaElement,
    val yogam: PanchangaElement,
    val paksha: Paksha,
    /** Moon's sidereal sign at sunrise (Chandra rasi). */
    val moonRasi: Int,
    /** When the Moon leaves [moonRasi]. */
    val moonRasiEndsAt: LocalDateTime?,
    val sunRasi: Int,
) {
    val moonRasiTa: String get() = TamilNames.rasis[moonRasi].first
    val moonRasiEn: String get() = TamilNames.rasis[moonRasi].second

    companion object {
        fun tithiNames(index: Int): Pair<String, String> = when (index) {
            14 -> "பௌர்ணமி" to "Pournami"
            29 -> "அமாவாசை" to "Amavasai"
            else -> TamilNames.tithis[index % 15]
        }
    }
}

enum class TimingType(val ta: String, val en: String) {
    NALLA_NERAM("நல்ல நேரம்", "Nalla Neram"),
    GOWRI_NALLA_NERAM("கௌரி நல்ல நேரம்", "Gowri Nalla Neram"),
    RAHU_KALAM("ராகு காலம்", "Rahu Kalam"),
    YAMAGANDAM("எமகண்டம்", "Yamagandam"),
    KULIGAI("குளிகை", "Kuligai"),
    ;

    val quality: TimingQuality
        get() = when (this) {
            NALLA_NERAM, GOWRI_NALLA_NERAM -> TimingQuality.AUSPICIOUS
            RAHU_KALAM, YAMAGANDAM -> TimingQuality.INAUSPICIOUS
            KULIGAI -> TimingQuality.MIXED
        }
}

enum class TimingQuality { AUSPICIOUS, INAUSPICIOUS, MIXED }

/** The eight daytime Gowri Panchangam periods. */
enum class Gowri(val ta: String, val en: String, val isGood: Boolean) {
    UTHI("உத்தி", "Uthi", true),
    AMIRDHAM("அமிர்தம்", "Amirdham", true),
    ROGAM("ரோகம்", "Rogam", false),
    LAABAM("லாபம்", "Laabam", true),
    DHANAM("தனம்", "Dhanam", true),
    SUGAM("சுகம்", "Sugam", true),
    SORAM("சோரம்", "Soram", false),
    VISHAM("விஷம்", "Visham", false),
}

data class TimingWindow(
    val type: TimingType,
    val start: LocalTime,
    val end: LocalTime,
    /** Set for Gowri windows, null otherwise. */
    val gowri: Gowri? = null,
) {
    val quality: TimingQuality get() = type.quality
}

data class DayTimings(
    val nallaNeram: List<TimingWindow>,
    val gowriNallaNeram: List<TimingWindow>,
    val rahuKalam: TimingWindow,
    val yamagandam: TimingWindow,
    val kuligai: TimingWindow,
) {
    fun all(): List<TimingWindow> = nallaNeram + gowriNallaNeram + listOf(rahuKalam, yamagandam, kuligai)
}

/** The Moon transits the 8th sign from [rasi]: people of that janma rasi should avoid big decisions. */
data class Chandrashtamam(
    val rasi: Int,
    val nakshatras: List<Int>,
    val until: LocalDateTime?,
) {
    val rasiTa: String get() = TamilNames.rasis[rasi].first
    val nakshatrasTa: String get() = nakshatras.joinToString(", ") { TamilNames.nakshatras[it].first }
}

/** Deities the monthly Tamil observances are dedicated to. */
enum class Deity(val ta: String, val en: String) {
    MURUGAN("முருகன்", "Murugan"),
    SHIVA("சிவன்", "Shiva"),
    PERUMAL("பெருமாள்", "Perumal"),
    VINAYAGAR("விநாயகர்", "Vinayagar"),
    AMMAN("அம்மன்", "Amman"),
}

enum class Religion(val ta: String, val en: String) {
    HINDU("இந்து", "Hindu"),
    CHRISTIAN("கிறிஸ்தவம்", "Christian"),
    ISLAM("இஸ்லாம்", "Islamic"),
    JAIN("சமணம்", "Jain"),
    SIKH("சீக்கியம்", "Sikh"),
    BUDDHIST("பௌத்தம்", "Buddhist"),
    JEWISH("யூதம்", "Jewish"),
}

enum class DayTag(val ta: String, val en: String, val deity: Deity? = null) {
    AMAVASAI("அமாவாசை", "Amavasai"),
    POURNAMI("பௌர்ணமி", "Pournami", Deity.AMMAN),
    PRADOSHAM("பிரதோஷம்", "Pradosham", Deity.SHIVA),
    MASA_SIVARATRI("மாத சிவராத்திரி", "Masa Sivaratri", Deity.SHIVA),
    EKADASI("ஏகாதசி", "Ekadasi", Deity.PERUMAL),
    THIRUVONAM("திருவோணம்", "Thiruvonam", Deity.PERUMAL),
    PURATTASI_SANI("புரட்டாசி சனி", "Purattasi Saturday", Deity.PERUMAL),
    SASHTI("சஷ்டி", "Sashti", Deity.MURUGAN),
    KRITHIGAI("கிருத்திகை", "Krithigai", Deity.MURUGAN),
    SANKATAHARA("சங்கடஹர சதுர்த்தி", "Sankatahara Chathurthi", Deity.VINAYAGAR),
    AADI_VELLI("ஆடி வெள்ளி", "Aadi Friday", Deity.AMMAN),
    SUBHA_MUHURTHAM("சுப முகூர்த்தம்", "Subha Muhurtham"),
    /** A day off in one of the configured local regions (e.g. US federal / Illinois). */
    GOVT_HOLIDAY("அரசு விடுமுறை", "Govt. Holiday"),
    SCHOOL_HOLIDAY("பள்ளி விடுமுறை", "No School"),
}

/** RELIGIOUS = observances of other faiths (Christian, Islamic, Jain, …); Hindu ones are FESTIVAL / VIRATHAM. */
enum class EventKind { FESTIVAL, VIRATHAM, GOVT_HOLIDAY, SCHOOL, MUHURTHAM, RELIGIOUS }

enum class EventSource {
    COMPUTED,
    /** Shipped in the APK (assets), e.g. school district calendars. */
    BUNDLED,
    REMOTE,
}

/** Whose holiday an event is. Which regions count as "local" days off is configured in AppContainer. */
enum class HolidayRegion(val ta: String, val en: String) {
    TAMIL_NADU("தமிழ்நாடு", "Tamil Nadu"),
    US_FEDERAL("அமெரிக்க அரசு", "US Federal"),
    ILLINOIS("இல்லினாய்", "Illinois"),
    SCHOOL_D203("பள்ளி 203", "Naperville 203"),
    SCHOOL_D204("பள்ளி 204", "Indian Prairie 204"),
}

data class CalendarEvent(
    val date: LocalDate,
    val kind: EventKind,
    val titleTa: String,
    val titleEn: String,
    /** A day off for [region] (no work / no school). Informational entries (e.g. "first day of school") are false. */
    val isHoliday: Boolean,
    val source: EventSource,
    val detail: String? = null,
    val region: HolidayRegion? = null,
    /** Set only on list rows that collapse a multi-day run (e.g. Winter Break); [date] is the first day. */
    val endDate: LocalDate? = null,
    /** A government day off where the user lives (set by the data layer from the region config). */
    val isLocalHoliday: Boolean = false,
    val religion: Religion? = null,
    val deity: Deity? = null,
)

enum class RasiOutlook(val ta: String, val en: String) {
    GOOD("நன்மை", "Good"),
    NEUTRAL("சமம்", "Neutral"),
    CAUTION("கவனம்", "Caution"),
    CHANDRASHTAMAM("சந்திராஷ்டமம்", "Chandrashtamam"),
}

data class RasiPalan(
    val rasi: Int,
    /** House the Moon occupies counted from this rasi (1..12). */
    val moonHouse: Int,
    val outlook: RasiOutlook,
    val messageTa: String,
    val messageEn: String,
    val source: EventSource,
) {
    val rasiTa: String get() = TamilNames.rasis[rasi].first
    val rasiEn: String get() = TamilNames.rasis[rasi].second
}

data class GeoLocation(
    /** Tamil name when known, otherwise the place name as returned by the geocoder. */
    val nameTa: String,
    /** Short English name, e.g. "Naperville". */
    val nameEn: String,
    val latitude: Double,
    /** East positive. */
    val longitude: Double,
    val zoneId: String,
    /** ISO 3166-1 alpha-2, e.g. "US", "IN". Drives holidays, timing convention and units. */
    val countryCode: String? = null,
    /** State / province, e.g. "Illinois", "Tamil Nadu". */
    val admin1: String? = null,
    val countryName: String? = null,
) {
    val coordinates: String
        get() = "%.2f°%s, %.2f°%s".format(
            kotlin.math.abs(latitude), if (latitude >= 0) "N" else "S",
            kotlin.math.abs(longitude), if (longitude >= 0) "E" else "W",
        )

    /** e.g. "Naperville, Illinois, United States" */
    val displayName: String get() = listOfNotNull(nameEn, admin1, countryName).distinct().joinToString(", ")

    /** e.g. "நேப்பர்வில் · Naperville (41.75°N, 88.15°W)" */
    val label: String
        get() = (if (nameTa != nameEn) "$nameTa · $nameEn" else nameEn) + " ($coordinates)"

    companion object {
        val CHENNAI = GeoLocation("சென்னை", "Chennai", 13.0827, 80.2707, "Asia/Kolkata", "IN", "Tamil Nadu", "India")
        val NAPERVILLE = GeoLocation("நேப்பர்வில்", "Naperville", 41.7508, -88.1535, "America/Chicago", "US", "Illinois", "United States")
    }
}

/** Location-dependent behaviour, in one place so a new location just works. */
object LocationPolicy {
    private fun GeoLocation.isUs() = countryCode == "US"
    private fun GeoLocation.isTamilNadu() = countryCode == "IN" && admin1 == "Tamil Nadu"

    /** Holidays that are real days off here, so they turn a day red. */
    fun localHolidayRegions(loc: GeoLocation): Set<HolidayRegion> = buildSet {
        if (loc.isUs()) add(HolidayRegion.US_FEDERAL)
        if (loc.isUs() && loc.admin1 == "Illinois") add(HolidayRegion.ILLINOIS)
        if (loc.isTamilNadu()) add(HolidayRegion.TAMIL_NADU)
    }

    /** Bundled school calendars only exist for Naperville (District 203 and Indian Prairie 204). */
    fun schoolDistricts(loc: GeoLocation): Set<HolidayRegion> =
        if (loc.isUs() && loc.admin1 == "Illinois" && loc.nameEn.equals("Naperville", ignoreCase = true)) {
            setOf(HolidayRegion.SCHOOL_D203, HolidayRegion.SCHOOL_D204)
        } else {
            emptySet()
        }

    /** Indian printed calendars use a fixed 6 AM–6 PM day; elsewhere the real sunrise–sunset span is used. */
    fun timingConvention(loc: GeoLocation): TimingConvention =
        if (loc.countryCode == "IN") TimingConvention.TRADITIONAL_6AM else TimingConvention.SUNRISE_BASED

    /** °F / mph in the US (and a few others), °C / km/h elsewhere. */
    fun usesImperialUnits(loc: GeoLocation): Boolean = loc.countryCode in setOf("US", "LR", "MM")
}

/**
 * Tamil printed calendars anchor Rahu/Yama/Kuligai/Gowri to a fixed 6:00–18:00 day.
 * Astrologically stricter sources divide the actual sunrise–sunset span instead.
 */
enum class TimingConvention { TRADITIONAL_6AM, SUNRISE_BASED }
