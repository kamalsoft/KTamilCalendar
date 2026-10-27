package com.kamalsoft.tamilcalendar.tv.domain.model

/** Bilingual name tables. Indices are 0-based and match the astronomical engine's output. */
object TamilNames {

    val months = listOf(
        "சித்திரை" to "Chithirai", "வைகாசி" to "Vaikasi", "ஆனி" to "Aani", "ஆடி" to "Aadi",
        "ஆவணி" to "Aavani", "புரட்டாசி" to "Purattasi", "ஐப்பசி" to "Aippasi", "கார்த்திகை" to "Karthigai",
        "மார்கழி" to "Margazhi", "தை" to "Thai", "மாசி" to "Maasi", "பங்குனி" to "Panguni",
    )

    /** 60-year cycle; index 0 (Prabhava) began with Chithirai 1987. */
    val years = listOf(
        "பிரபவ" to "Prabhava", "விபவ" to "Vibhava", "சுக்ல" to "Sukla", "பிரமோதூத" to "Pramodhoota",
        "பிரசோற்பத்தி" to "Prajorpathi", "ஆங்கீரச" to "Aangirasa", "ஸ்ரீமுக" to "Srimukha", "பவ" to "Bhava",
        "யுவ" to "Yuva", "தாது" to "Dhaatu", "ஈஸ்வர" to "Eswara", "வெகுதானிய" to "Vehudhanya",
        "பிரமாதி" to "Pramathi", "விக்கிரம" to "Vikrama", "விஷு" to "Vishu", "சித்திரபானு" to "Chitrabhanu",
        "சுபானு" to "Subhanu", "தாரண" to "Dharana", "பார்த்திப" to "Parthiba", "விய" to "Viya",
        "சர்வசித்து" to "Sarvajith", "சர்வதாரி" to "Sarvadhari", "விரோதி" to "Virodhi", "விக்ருதி" to "Vikruthi",
        "கர" to "Kara", "நந்தன" to "Nandhana", "விஜய" to "Vijaya", "ஜய" to "Jaya",
        "மன்மத" to "Manmatha", "துன்முகி" to "Dhunmukhi", "ஹேவிளம்பி" to "Hevilambi", "விளம்பி" to "Vilambi",
        "விகாரி" to "Vikari", "சார்வரி" to "Sarvari", "பிலவ" to "Plava", "சுபகிருது" to "Subakrith",
        "சோபகிருது" to "Sobakrith", "குரோதி" to "Krodhi", "விசுவாவசு" to "Visuvasu", "பராபவ" to "Parabhava",
        "பிலவங்க" to "Plavanga", "கீலக" to "Keelaka", "சௌமிய" to "Saumya", "சாதாரண" to "Sadharana",
        "விரோதகிருது" to "Virodhakruthu", "பரிதாபி" to "Paridhabi", "பிரமாதீச" to "Pramadhisa", "ஆனந்த" to "Aanandha",
        "ராட்சச" to "Rakshasa", "நள" to "Nala", "பிங்கள" to "Pingala", "காளயுக்தி" to "Kalayukthi",
        "சித்தார்த்தி" to "Siddharthi", "ரௌத்திரி" to "Raudhri", "துன்மதி" to "Dhunmathi", "துந்துபி" to "Dhundhubhi",
        "ருத்ரோத்காரி" to "Rudhrodhgari", "ரக்தாட்சி" to "Raktakshi", "குரோதன" to "Krodhana", "அட்சய" to "Akshaya",
    )

    /** 15 names; index 14 is Pournami in Shukla paksha and Amavasai in Krishna paksha. */
    val tithis = listOf(
        "பிரதமை" to "Prathamai", "துவிதியை" to "Dwitiyai", "திருதியை" to "Tritiyai", "சதுர்த்தி" to "Chathurthi",
        "பஞ்சமி" to "Panchami", "சஷ்டி" to "Sashti", "சப்தமி" to "Sapthami", "அஷ்டமி" to "Ashtami",
        "நவமி" to "Navami", "தசமி" to "Dasami", "ஏகாதசி" to "Ekadasi", "துவாதசி" to "Dwadasi",
        "திரயோதசி" to "Thrayodasi", "சதுர்த்தசி" to "Chathurdasi",
    )

    val nakshatras = listOf(
        "அசுவினி" to "Aswini", "பரணி" to "Bharani", "கார்த்திகை" to "Karthigai", "ரோகிணி" to "Rohini",
        "மிருகசீரிடம்" to "Mirugasirisham", "திருவாதிரை" to "Thiruvathirai", "புனர்பூசம்" to "Punarpoosam",
        "பூசம்" to "Poosam", "ஆயில்யம்" to "Ayilyam", "மகம்" to "Magam", "பூரம்" to "Pooram",
        "உத்திரம்" to "Uthiram", "அஸ்தம்" to "Hastham", "சித்திரை" to "Chithirai", "சுவாதி" to "Swathi",
        "விசாகம்" to "Visakam", "அனுஷம்" to "Anusham", "கேட்டை" to "Kettai", "மூலம்" to "Moolam",
        "பூராடம்" to "Pooradam", "உத்திராடம்" to "Uthiradam", "திருவோணம்" to "Thiruvonam",
        "அவிட்டம்" to "Avittam", "சதயம்" to "Sathayam", "பூரட்டாதி" to "Poorattathi",
        "உத்திரட்டாதி" to "Uthirattathi", "ரேவதி" to "Revathi",
    )

    val rasis = listOf(
        "மேஷம்" to "Mesham", "ரிஷபம்" to "Rishabam", "மிதுனம்" to "Mithunam", "கடகம்" to "Kadagam",
        "சிம்மம்" to "Simmam", "கன்னி" to "Kanni", "துலாம்" to "Thulam", "விருச்சிகம்" to "Viruchigam",
        "தனுசு" to "Dhanusu", "மகரம்" to "Magaram", "கும்பம்" to "Kumbam", "மீனம்" to "Meenam",
    )

    val yogas = listOf(
        "விஷ்கம்பம்" to "Vishkambam", "ப்ரீதி" to "Preethi", "ஆயுஷ்மான்" to "Ayushman", "சௌபாக்கியம்" to "Saubhagyam",
        "சோபனம்" to "Sobhanam", "அதிகண்டம்" to "Athigandam", "சுகர்மம்" to "Sukarmam", "திருதி" to "Dhriti",
        "சூலம்" to "Soolam", "கண்டம்" to "Gandam", "விருத்தி" to "Vriddhi", "துருவம்" to "Dhruvam",
        "வியாகாதம்" to "Vyaghatham", "ஹர்ஷணம்" to "Harshanam", "வஜ்ரம்" to "Vajram", "சித்தி" to "Siddhi",
        "வியதீபாதம்" to "Vyatheepatham", "வரியான்" to "Variyan", "பரிகம்" to "Parigam", "சிவம்" to "Sivam",
        "சித்தம்" to "Siddham", "சாத்தியம்" to "Sadhyam", "சுபம்" to "Subham", "சுப்பிரம்" to "Subhram",
        "பிராம்மம்" to "Brahmam", "ஐந்திரம்" to "Aindhram", "வைதிருதி" to "Vaidhruthi",
    )

    /** Sunday-first, matching [java.time.DayOfWeek.value] % 7. */
    val weekdays = listOf(
        "ஞாயிறு" to "Sunday", "திங்கள்" to "Monday", "செவ்வாய்" to "Tuesday", "புதன்" to "Wednesday",
        "வியாழன்" to "Thursday", "வெள்ளி" to "Friday", "சனி" to "Saturday",
    )
}
