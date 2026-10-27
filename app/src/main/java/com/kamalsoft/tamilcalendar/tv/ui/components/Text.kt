package com.kamalsoft.tamilcalendar.tv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.tv.material3.LocalTextStyle
import com.kamalsoft.tamilcalendar.tv.ui.theme.DisplayPrefs

private fun Char.isTamil() = this in '஀'..'௿'

/**
 * "Tamil only" mode: in bilingual labels ("திதி · Tithi"), drops the " · " parts that are English
 * (contain Latin letters and no Tamil). Text with no Tamil at all (e.g. the weather page) is untouched.
 */
fun localize(text: String): String {
    if (!DisplayPrefs.tamilOnly || '·' !in text || text.none { it.isTamil() }) return text
    return text.split(" · ")
        .filter { part -> part.any { it.isTamil() } || part.none { it in 'A'..'Z' || it in 'a'..'z' } }
        .joinToString(" · ")
}

/** App-wide Text: tv-material Text plus the Tamil-only setting. Same parameters as used across the UI. */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current,
) = androidx.tv.material3.Text(
    text = localize(text), modifier = modifier, color = color, fontSize = fontSize, fontWeight = fontWeight,
    textAlign = textAlign, lineHeight = lineHeight, overflow = overflow, softWrap = softWrap, maxLines = maxLines, style = style,
)
