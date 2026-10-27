package com.kamalsoft.tamilcalendar.tv.domain.content

import com.kamalsoft.tamilcalendar.tv.domain.model.RasiOutlook

/**
 * Classical gochara (transit) interpretations by house counted from the janma rasi (Moon sign).
 * Favourable houses: Sun 3/6/10/11 · Jupiter 2/5/7/9/11 · Saturn 3/6/11 · Rahu 3/6/11.
 */
object Gochara {
    data class Line(val ta: String, val en: String)

    val SUN = listOf(
        Line("உடல் சோர்வு; கோபத்தைத் தவிர்க்கவும்.", "Fatigue; avoid anger."),
        Line("செலவும் கண் தொந்தரவும்; பேச்சில் கவனம்.", "Expenses and eye strain; mind your words."),
        Line("வெற்றியும் தைரியமும்; புதிய முயற்சிகள் பலிக்கும்.", "Success and courage; new efforts pay off."),
        Line("வீட்டில் சிறு மனஸ்தாபம்; பயணத்தில் கவனம்.", "Minor friction at home; take care when travelling."),
        Line("மனக்குழப்பம்; பிள்ளைகள் நலனில் கவனம்.", "A restless mind; look after the children's needs."),
        Line("எதிரிகள் அடங்குவர்; ஆரோக்கியம் சிறக்கும்.", "Opponents subside; good health."),
        Line("துணையுடன் கருத்து வேறுபாடு; பொறுமை தேவை.", "Differences with spouse or partners; be patient."),
        Line("உடல்நலத்தில் கவனம்; அவசர முடிவுகள் வேண்டாம்.", "Watch your health; avoid hasty decisions."),
        Line("தந்தை வழி கவலை; ஆன்மீகப் பயணம் நல்லது.", "Concerns about your father; a pilgrimage helps."),
        Line("தொழிலில் உயர்வு, அங்கீகாரம்.", "Career rise and recognition."),
        Line("வருமானம் உயரும்; நல்ல செய்தி வரும்.", "Income rises; good news arrives."),
        Line("செலவு அதிகம்; ஓய்வும் வழிபாடும் நன்மை.", "Higher spending; rest and prayer help."),
    )
    val JUPITER = listOf(
        Line("இடமாற்றம், மன அழுத்தம்; ஆலோசனை கேட்டு நடக்கவும்.", "Changes and stress; act on good advice."),
        Line("குடும்ப மகிழ்ச்சி, பண வரவு.", "Family happiness and money."),
        Line("முயற்சிகளில் தடை; பொறுமை வேண்டும்.", "Obstacles in efforts; patience needed."),
        Line("வீடு, வாகனம் சார்ந்த கவலை.", "Concerns about home or vehicle."),
        Line("பிள்ளைப் பேறு, கல்வி, புத்தி வளர்ச்சி.", "Children, learning and wisdom flourish."),
        Line("கடன், போட்டி; செலவில் கட்டுப்பாடு.", "Debts and rivalry; control spending."),
        Line("திருமணம், கூட்டாண்மை சுபம்.", "Auspicious for marriage and partnerships."),
        Line("எதிர்பாராத தடைகள்; ஆரோக்கியத்தில் கவனம்.", "Unexpected hurdles; watch your health."),
        Line("பாக்கியம், ஆன்மீக முன்னேற்றம்.", "Good fortune and spiritual progress."),
        Line("பணியில் மாற்றம்; கவனமாக இருக்கவும்.", "Changes at work; stay careful."),
        Line("லாபம்; விருப்பங்கள் நிறைவேறும்.", "Gains; wishes fulfilled."),
        Line("சுபச் செலவுகள், தொலைதூரப் பயணம்.", "Spending on good causes; long journeys."),
    )
    val SATURN = listOf(
        Line("ஏழரைச் சனி (ஜென்மச் சனி): உழைப்பு அதிகம், பொறுமை தேவை.", "Sade Sati (Janma Shani): hard work; patience needed."),
        Line("ஏழரைச் சனி (பாதச் சனி): குடும்பச் செலவுகளில் கவனம்.", "Sade Sati (final phase): watch family expenses."),
        Line("நல்ல காலம்: முயற்சிகள் வெற்றி பெறும்.", "A good period: efforts succeed."),
        Line("அர்த்தாஷ்டமச் சனி: வீடு, தாய் நலனில் கவனம்.", "Ardhashtama Shani: care for home and mother."),
        Line("பிள்ளைகள், முதலீடுகளில் நிதானம்.", "Go slowly with children's matters and investments."),
        Line("நல்ல காலம்: கடன், நோய் நீங்கும்.", "A good period: debts and illness recede."),
        Line("கூட்டாண்மையில் சிக்கல்; பொறுமை.", "Strain in partnerships; patience."),
        Line("அஷ்டமச் சனி: உடல்நலம், பயணத்தில் கவனம்.", "Ashtama Shani: care with health and travel."),
        Line("தந்தை, குருவழி கவலை; வழிபாடு நன்று.", "Concerns for father or teachers; prayer helps."),
        Line("பணிச்சுமை அதிகம்; நேர்மை பலன் தரும்.", "Heavy workload; integrity pays."),
        Line("நல்ல காலம்: லாபமும் உயர்வும்.", "A good period: gains and advancement."),
        Line("ஏழரைச் சனி ஆரம்பம்: செலவுகளில் கவனம்.", "Sade Sati begins: control spending."),
    )
    val RAHU_GOOD = Line("ராகு சாதகம்: துணிச்சலான முயற்சிகள் பலிக்கும்.", "Rahu favourable: bold moves pay off.")
    val RAHU_BAD = Line("ராகு: குழப்பங்களைத் தவிர்த்துத் தெளிவாக முடிவெடுக்கவும்.", "Rahu: avoid confusion; decide with clarity.")

    private val SUN_GOOD = setOf(3, 6, 10, 11)
    private val JUP_GOOD = setOf(2, 5, 7, 9, 11)
    private val SAT_GOOD = setOf(3, 6, 11)
    private val RAHU_GOOD_HOUSES = setOf(3, 6, 11)

    fun sunGood(house: Int) = house in SUN_GOOD
    fun jupiterGood(house: Int) = house in JUP_GOOD
    fun saturnGood(house: Int) = house in SAT_GOOD
    fun rahuGood(house: Int) = house in RAHU_GOOD_HOUSES

    /** Overall outlook from how many of the considered transits are favourable. */
    fun outlook(good: Int, of: Int): RasiOutlook = when {
        good * 2 > of -> RasiOutlook.GOOD
        good * 2 == of -> RasiOutlook.NEUTRAL
        else -> RasiOutlook.CAUTION
    }
}
