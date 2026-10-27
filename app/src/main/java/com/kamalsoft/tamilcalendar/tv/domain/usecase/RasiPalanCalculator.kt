package com.kamalsoft.tamilcalendar.tv.domain.usecase

import com.kamalsoft.tamilcalendar.tv.domain.model.EventSource
import com.kamalsoft.tamilcalendar.tv.domain.model.RasiOutlook
import com.kamalsoft.tamilcalendar.tv.domain.model.RasiPalan

/**
 * Offline Rasi Palan based on Chandra Gochara (the Moon's transit counted from each janma rasi).
 * Classical rule: houses 1, 3, 6, 7, 10, 11 favourable; 8 is Chandrashtamam.
 * Editorial predictions synced from the API replace these messages when available.
 */
object RasiPalanCalculator {

    private val houseMessages = listOf(
        RasiOutlook.GOOD to ("மனதில் உற்சாகம்; புதிய முயற்சிகளுக்கு ஏற்ற நாள்." to "Cheerful mood; good day to begin new efforts."),
        RasiOutlook.NEUTRAL to ("செலவுகளில் கவனம் தேவை; பேச்சில் நிதானம்." to "Watch expenses; be measured in speech."),
        RasiOutlook.GOOD to ("தைரியம் கூடும்; எடுத்த காரியம் வெற்றி பெறும்." to "Courage rises; tasks taken up succeed."),
        RasiOutlook.CAUTION to ("குடும்பத்தில் சிறு குழப்பம்; பயணங்களில் கவனம்." to "Minor family friction; take care when travelling."),
        RasiOutlook.CAUTION to ("திட்டமிட்ட செயல்களில் தாமதம்; பொறுமை அவசியம்." to "Plans may be delayed; patience needed."),
        RasiOutlook.GOOD to ("எதிர்ப்புகள் விலகும்; ஆரோக்கியம் மேம்படும்." to "Opposition fades; health improves."),
        RasiOutlook.GOOD to ("நண்பர்கள், வாழ்க்கைத் துணையின் ஆதரவு கிடைக்கும்." to "Support from friends and spouse."),
        RasiOutlook.CHANDRASHTAMAM to ("சந்திராஷ்டமம் — முக்கிய முடிவுகளைத் தவிர்க்கவும்." to "Chandrashtamam — postpone important decisions."),
        RasiOutlook.NEUTRAL to ("வீண் அலைச்சல் ஏற்படலாம்; இறை வழிபாடு நன்மை தரும்." to "Possible wasted running around; prayer helps."),
        RasiOutlook.GOOD to ("தொழிலில் முன்னேற்றம்; பாராட்டு கிடைக்கும்." to "Progress at work; recognition comes."),
        RasiOutlook.GOOD to ("லாபம் உண்டு; மகிழ்ச்சியான செய்தி வரும்." to "Gains likely; happy news arrives."),
        RasiOutlook.CAUTION to ("செலவு அதிகரிக்கும்; போதுமான ஓய்வு தேவை." to "Expenses rise; get enough rest."),
    )

    fun forMoonRasi(moonRasi: Int): List<RasiPalan> = (0 until 12).map { rasi ->
        val house = Math.floorMod(moonRasi - rasi, 12) + 1
        val (outlook, text) = houseMessages[house - 1]
        RasiPalan(rasi, house, outlook, text.first, text.second, EventSource.COMPUTED)
    }
}
