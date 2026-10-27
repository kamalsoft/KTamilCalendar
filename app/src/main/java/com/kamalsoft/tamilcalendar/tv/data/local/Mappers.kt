package com.kamalsoft.tamilcalendar.tv.data.local

import com.kamalsoft.tamilcalendar.tv.domain.model.CalendarEvent
import com.kamalsoft.tamilcalendar.tv.domain.model.Chandrashtamam
import com.kamalsoft.tamilcalendar.tv.domain.model.DayTag
import com.kamalsoft.tamilcalendar.tv.domain.model.DayTimings
import com.kamalsoft.tamilcalendar.tv.domain.model.EventKind
import com.kamalsoft.tamilcalendar.tv.domain.model.EventSource
import com.kamalsoft.tamilcalendar.tv.domain.model.Gowri
import com.kamalsoft.tamilcalendar.tv.domain.model.Deity
import com.kamalsoft.tamilcalendar.tv.domain.model.HolidayRegion
import com.kamalsoft.tamilcalendar.tv.domain.model.Religion
import com.kamalsoft.tamilcalendar.tv.domain.model.Paksha
import com.kamalsoft.tamilcalendar.tv.domain.model.PanchangaElement
import com.kamalsoft.tamilcalendar.tv.domain.model.Panchangam
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilDate
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilDayData
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilNames
import com.kamalsoft.tamilcalendar.tv.domain.model.TimingType
import com.kamalsoft.tamilcalendar.tv.domain.model.TimingWindow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * @param localHolidayRegions regions whose holidays mark a day red (GOVT_HOLIDAY); other regions'
 *   holidays (e.g. Tamil Nadu when living in the US) are still listed, but only as information.
 */
class CalendarMapper(
    private val zone: ZoneId,
    private val dataVersion: Int,
    private val localHolidayRegions: Set<HolidayRegion>,
) {

    private fun LocalDateTime?.toEpoch(): Long? = this?.atZone(zone)?.toEpochSecond()
    private fun Long?.toLocal(): LocalDateTime? = this?.let { LocalDateTime.ofInstant(Instant.ofEpochSecond(it), zone) }
    private fun Int.toTime(): LocalTime = LocalTime.ofSecondOfDay(toLong())

    fun toEntity(d: TamilDayData): DayEntity = DayEntity(
        epochDay = d.date.toEpochDay(),
        engineVersion = dataVersion,
        tamilMonth = d.tamilDate.monthIndex,
        tamilDay = d.tamilDate.day,
        yearCycleIndex = d.tamilDate.yearCycleIndex,
        thiruvalluvarAandu = d.tamilDate.thiruvalluvarAandu,
        sunriseSec = d.sunrise.toSecondOfDay(),
        sunsetSec = d.sunset.toSecondOfDay(),
        tithi = d.panchangam.tithi.index,
        tithiEnd = d.panchangam.tithi.endsAt.toEpoch(),
        nakshatram = d.panchangam.nakshatram.index,
        nakshatramEnd = d.panchangam.nakshatram.endsAt.toEpoch(),
        yogam = d.panchangam.yogam.index,
        yogamEnd = d.panchangam.yogam.endsAt.toEpoch(),
        moonRasi = d.panchangam.moonRasi,
        moonRasiEnd = d.panchangam.moonRasiEndsAt.toEpoch(),
        sunRasi = d.panchangam.sunRasi,
        chandrashtamaRasi = d.chandrashtamam.rasi,
        chandrashtamaStars = d.chandrashtamam.nakshatras.joinToString(","),
        tags = (d.tags - DayTag.GOVT_HOLIDAY - DayTag.SCHOOL_HOLIDAY).joinToString(",") { it.name },
    )

    fun toTimingEntities(d: TamilDayData): List<TimingEntity> = d.timings.all().map {
        TimingEntity(
            epochDay = d.date.toEpochDay(), type = it.type.name,
            startSec = it.start.toSecondOfDay(), endSec = it.end.toSecondOfDay(), gowri = it.gowri?.name,
        )
    }

    fun toEventEntity(e: CalendarEvent): EventEntity = EventEntity(
        epochDay = e.date.toEpochDay(), kind = e.kind.name, titleTa = e.titleTa, titleEn = e.titleEn,
        isHoliday = e.isHoliday, source = e.source.name, detail = e.detail, region = e.region?.name,
        religion = e.religion?.name, deity = e.deity?.name,
    )

    fun toDomain(e: EventEntity): CalendarEvent = CalendarEvent(
        date = LocalDate.ofEpochDay(e.epochDay),
        kind = EventKind.valueOf(e.kind),
        titleTa = e.titleTa, titleEn = e.titleEn, isHoliday = e.isHoliday,
        source = EventSource.valueOf(e.source), detail = e.detail,
        region = e.region?.let { r -> HolidayRegion.entries.firstOrNull { it.name == r } },
        religion = e.religion?.let { r -> Religion.entries.firstOrNull { it.name == r } },
        deity = e.deity?.let { r -> Deity.entries.firstOrNull { it.name == r } },
    ).let { it.copy(isLocalHoliday = isLocalDayOff(it)) }

    fun toDomain(w: DayWithDetails): TamilDayData {
        val e = w.day
        val windows = w.timings.map {
            TimingWindow(TimingType.valueOf(it.type), it.startSec.toTime(), it.endSec.toTime(), it.gowri?.let(Gowri::valueOf))
        }.sortedBy { it.start }
        fun single(t: TimingType) = windows.first { it.type == t }

        val events = w.events.map(::toDomain)
        val tags = e.tags.split(',').filter { it.isNotBlank() }.mapTo(mutableSetOf()) { DayTag.valueOf(it) }
        if (events.any { it.isLocalHoliday }) tags += DayTag.GOVT_HOLIDAY
        if (events.any { it.kind == EventKind.SCHOOL && it.isHoliday }) tags += DayTag.SCHOOL_HOLIDAY

        val tithiNames = Panchangam.tithiNames(e.tithi)
        return TamilDayData(
            date = LocalDate.ofEpochDay(e.epochDay),
            tamilDate = TamilDate(e.tamilMonth, e.tamilDay, e.yearCycleIndex, e.thiruvalluvarAandu),
            sunrise = e.sunriseSec.toTime(),
            sunset = e.sunsetSec.toTime(),
            panchangam = Panchangam(
                tithi = PanchangaElement(e.tithi, tithiNames.first, tithiNames.second, e.tithiEnd.toLocal()),
                nakshatram = TamilNames.nakshatras[e.nakshatram].let {
                    PanchangaElement(e.nakshatram, it.first, it.second, e.nakshatramEnd.toLocal())
                },
                yogam = TamilNames.yogas[e.yogam].let { PanchangaElement(e.yogam, it.first, it.second, e.yogamEnd.toLocal()) },
                paksha = if (e.tithi < 15) Paksha.SHUKLA else Paksha.KRISHNA,
                moonRasi = e.moonRasi,
                moonRasiEndsAt = e.moonRasiEnd.toLocal(),
                sunRasi = e.sunRasi,
            ),
            timings = DayTimings(
                nallaNeram = windows.filter { it.type == TimingType.NALLA_NERAM },
                gowriNallaNeram = windows.filter { it.type == TimingType.GOWRI_NALLA_NERAM },
                rahuKalam = single(TimingType.RAHU_KALAM),
                yamagandam = single(TimingType.YAMAGANDAM),
                kuligai = single(TimingType.KULIGAI),
            ),
            chandrashtamam = Chandrashtamam(
                rasi = e.chandrashtamaRasi,
                nakshatras = e.chandrashtamaStars.split(',').filter { it.isNotBlank() }.map { it.toInt() },
                until = e.moonRasiEnd.toLocal(),
            ),
            tags = tags,
            events = events.sortedWith(compareByDescending<CalendarEvent> { it.isLocalHoliday }.thenBy { it.kind == EventKind.SCHOOL }),
        )
    }

    /** A government holiday in a region the user lives in. Remote (synced) holidays count as local. */
    fun isLocalDayOff(e: CalendarEvent): Boolean =
        e.isHoliday && e.kind != EventKind.SCHOOL && (e.source == EventSource.REMOTE || e.region in localHolidayRegions)
}
