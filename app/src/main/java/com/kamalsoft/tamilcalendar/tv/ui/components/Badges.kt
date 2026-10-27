package com.kamalsoft.tamilcalendar.tv.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.kamalsoft.tamilcalendar.tv.R
import com.kamalsoft.tamilcalendar.tv.domain.model.DayTag
import com.kamalsoft.tamilcalendar.tv.domain.model.Religion
import com.kamalsoft.tamilcalendar.tv.ui.theme.TvColors

@DrawableRes
fun DayTag.badgeRes(): Int = when (this) {
    DayTag.AMAVASAI -> R.drawable.badge_amavasai
    DayTag.POURNAMI -> R.drawable.badge_pournami
    DayTag.PRADOSHAM -> R.drawable.badge_pradosham
    DayTag.EKADASI -> R.drawable.badge_ekadasi
    DayTag.KRITHIGAI -> R.drawable.badge_krithigai
    DayTag.SUBHA_MUHURTHAM -> R.drawable.badge_muhurtham
    DayTag.GOVT_HOLIDAY -> R.drawable.badge_holiday
    DayTag.SCHOOL_HOLIDAY -> R.drawable.badge_school
    DayTag.MASA_SIVARATRI -> R.drawable.badge_sivaratri
    DayTag.THIRUVONAM -> R.drawable.badge_thiruvonam
    DayTag.PURATTASI_SANI -> R.drawable.badge_purattasi
    DayTag.SASHTI -> R.drawable.badge_sashti
    DayTag.SANKATAHARA -> R.drawable.badge_sankatahara
    DayTag.AADI_VELLI -> R.drawable.badge_aadi_velli
}

/** Icon for a faith's observances; null for Hindu (Hindu days use the deity/observance badges). */
@DrawableRes
fun Religion.iconRes(): Int? = when (this) {
    Religion.HINDU -> null
    Religion.CHRISTIAN -> R.drawable.rel_christian
    Religion.ISLAM -> R.drawable.rel_islam
    Religion.JAIN -> R.drawable.rel_jain
    Religion.SIKH -> R.drawable.rel_sikh
    Religion.BUDDHIST -> R.drawable.rel_buddhist
    Religion.JEWISH -> R.drawable.rel_jewish
}

/** Icon + Tamil label pill, used on the Daily hero card. */
@Composable
fun TagChip(tag: DayTag) {
    Row(
        modifier = Modifier
            .background(TvColors.MidnightDeep.copy(alpha = 0.7f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Image(painterResource(tag.badgeRes()), contentDescription = null, modifier = Modifier.size(18.dp))
        Text("${tag.ta} · ${tag.en}", style = MaterialTheme.typography.labelMedium, color = TvColors.Text)
    }
}

/**
 * Icon-only badges for dense surfaces (month grid): the day's observances first (deity badges),
 * then one icon per other faith with an observance that day.
 */
@Composable
fun TagIcons(tags: Set<DayTag>, religions: Set<Religion> = emptySet(), size: Dp = 12.dp, max: Int = 4) {
    val icons = tags.filter { it != DayTag.GOVT_HOLIDAY }.sortedBy { it.ordinal }.map { it.badgeRes() to it.en } +
        religions.sortedBy { it.ordinal }.mapNotNull { r -> r.iconRes()?.let { it to r.en } }
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        icons.take(max).forEach { (res, label) ->
            Image(painterResource(res), contentDescription = label, modifier = Modifier.size(size))
        }
    }
}

@Composable
fun Pill(text: String, color: Color, textColor: Color = TvColors.Text) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = textColor,
        modifier = Modifier
            .background(color, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}
