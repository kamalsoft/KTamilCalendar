package com.kamalsoft.tamilcalendar.tv.domain.content

import com.kamalsoft.tamilcalendar.tv.domain.model.DayTag
import com.kamalsoft.tamilcalendar.tv.domain.model.Deity
import com.kamalsoft.tamilcalendar.tv.domain.model.TamilDayData

/** A short devotional verse. All texts are classical works in the public domain; read by text-to-speech. */
data class Sloka(
    val id: String,
    val titleTa: String,
    val titleEn: String,
    val deity: Deity?,
    val textTa: String,
    /** Spoken when the device has no Tamil voice. */
    val transliteration: String,
    val meaningEn: String,
    val source: String,
)

object Slokas {
    val SUPRABHATAM = Sloka(
        "suprabhatam", "வேங்கடேச சுப்ரபாதம்", "Venkatesa Suprabhatam", Deity.PERUMAL,
        "கௌஸல்யா ஸுப்ரஜா ராம பூர்வா ஸந்த்யா ப்ரவர்த்ததே ।\nஉத்திஷ்ட நரசார்தூல கர்த்தவ்யம் தைவமாஹ்நிகம் ॥",
        "Kausalya supraja Rama, purva sandhya pravartate. Uttishtha narashardula, kartavyam daivam ahnikam.",
        "O Rama, dear son of Kausalya, dawn is breaking in the east. Arise, tiger among men; the day's sacred duties await.",
        // Verse 1 is Valmiki's (Ramayana, Bala Kanda); the rest of the Suprabhatam is by Prativadi Bhayankaram Annan.
        "Sri Venkatesa Suprabhatam, verse 1 — from Valmiki Ramayana, Bala Kanda",
    )
    val THIRUPPUGAZH = Sloka(
        "thiruppugazh", "திருப்புகழ் — முத்தைத்தரு", "Thiruppugazh — Muththaith tharu", Deity.MURUGAN,
        "முத்தைத்தரு பத்தித் திருநகை\nஅத்திக்கிறை சத்திச் சரவண\nமுத்திக்கொரு வித்துக் குருபர எனவோதும்",
        "Muththaith tharu paththith thirunagai, aththikkirai saththich charavana, muththikkoru viththuk gurupara ena odhum.",
        "O Saravana, Lord of Deivanai whose smile is like a row of pearls, bearer of the Shakti spear, seed of liberation, supreme Guru — so the scriptures sing of you.",
        "Thiruppugazh, opening lines (Arunagirinathar, 15th c.)",
    )
    val SIVAPURANAM = Sloka(
        "sivapuranam", "சிவபுராணம்", "Sivapuranam", Deity.SHIVA,
        "நமச்சிவாய வாழ்க நாதன் தாள் வாழ்க\nஇமைப்பொழுதும் என் நெஞ்சில் நீங்காதான் தாள் வாழ்க",
        "Namachchivaaya vaazhga, naadhan thaal vaazhga. Imaippozhudhum en nenjil neengaadhaan thaal vaazhga.",
        "Hail Namasivaya! Hail the feet of the Lord, who never leaves my heart even for the blink of an eye.",
        "Thiruvasagam, Sivapuranam lines 1–2 (Manikkavasagar, 9th c.)",
    )
    val VINAYAGAR = Sloka(
        "suklambaradharam", "விநாயகர் துதி", "Vinayagar Dhyanam", Deity.VINAYAGAR,
        "ஶுக்லாம்பரதரம் விஷ்ணும் ஶஶிவர்ணம் சதுர்புஜம் ।\nப்ரஸந்ந வதநம் த்யாயேத் ஸர்வ விக்நோப ஶாந்தயே ॥",
        "Shuklambaradharam Vishnum, shashivarnam chaturbhujam. Prasanna vadanam dhyayet, sarva vighnopa shantaye.",
        "Meditate on the all-pervading one, clad in white, moon-hued and four-armed, with a serene face, so that every obstacle is removed.",
        "Traditional dhyana sloka",
    )
    val NARAYANI = Sloka(
        "narayani", "நாராயணி ஸ்துதி", "Narayani Stuti", Deity.AMMAN,
        "ஸர்வ மங்கள மாங்கல்யே ஶிவே ஸர்வார்த்த ஸாதிகே ।\nஶரண்யே த்ர்யம்பகே கௌரி நாராயணி நமோஸ்து தே ॥",
        "Sarva mangala mangalye, shive sarvartha sadhike. Sharanye tryambake Gauri, Narayani namostu te.",
        "Auspiciousness of all that is auspicious, gracious one who fulfils every aim, refuge of all, three-eyed Gauri — salutations to you, Narayani.",
        "Devi Mahatmyam, Narayani Stuti",
    )
    val GAYATRI = Sloka(
        "gayatri", "காயத்ரி மந்திரம்", "Gayatri Mantra", null,
        "ஓம் பூர் புவ: ஸுவ: ।\nதத் ஸவிதுர் வரேண்யம் பர்கோ தேவஸ்ய தீமஹி ।\nதியோ யோ ந: ப்ரசோதயாத் ॥",
        "Om bhur bhuvah suvah. Tat savitur varenyam, bhargo devasya dhimahi. Dhiyo yo nah prachodayat.",
        "We meditate on the adorable radiance of the divine Sun; may it illumine our minds.",
        "Rig Veda 3.62.10",
    )
    val KURAL = Sloka(
        "kural1", "திருக்குறள் — கடவுள் வாழ்த்து", "Thirukkural — Kural 1", null,
        "அகர முதல எழுத்தெல்லாம் ஆதி\nபகவன் முதற்றே உலகு",
        "Agara mudhala ezhuththellaam, aadhi bagavan mudhatre ulagu.",
        "As the letter A is the first of all letters, so the Primal God is the first of the world.",
        "Thirukkural, Kural 1 (Thiruvalluvar)",
    )

    val ALL = listOf(SUPRABHATAM, THIRUPPUGAZH, SIVAPURANAM, VINAYAGAR, NARAYANI, GAYATRI, KURAL)

    /**
     * The day's sloka: the day's observance decides the deity (Sashti → Murugan, Pradosham → Shiva, …);
     * otherwise the weekday tradition (Mon Shiva, Tue Murugan, Wed Vinayagar, Thu Kural, Fri Amman,
     * Sat Perumal Suprabhatam, Sun Gayatri).
     */
    fun forDay(day: TamilDayData): Sloka {
        val byTag = when {
            DayTag.SASHTI in day.tags || DayTag.KRITHIGAI in day.tags -> THIRUPPUGAZH
            DayTag.PRADOSHAM in day.tags || DayTag.MASA_SIVARATRI in day.tags -> SIVAPURANAM
            DayTag.SANKATAHARA in day.tags -> VINAYAGAR
            DayTag.EKADASI in day.tags || DayTag.THIRUVONAM in day.tags || DayTag.PURATTASI_SANI in day.tags -> SUPRABHATAM
            DayTag.AADI_VELLI in day.tags || DayTag.POURNAMI in day.tags -> NARAYANI
            else -> null
        }
        return byTag ?: when (day.weekdayIndex) {
            1 -> SIVAPURANAM
            2 -> THIRUPPUGAZH
            3 -> VINAYAGAR
            4 -> KURAL
            5 -> NARAYANI
            6 -> SUPRABHATAM
            else -> GAYATRI
        }
    }
}
