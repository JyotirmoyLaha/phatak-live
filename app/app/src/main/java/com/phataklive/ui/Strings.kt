package com.phataklive.ui

/** All UI text in English, Hindi and Bengali; the user switches with EN / हि / বা (first launch follows the phone language). */
class Strings(lang: String) {
    private val i = when (lang) { "Hindi" -> 1; "Bengali" -> 2; else -> 0 }
    private fun t(en: String, hi: String, bn: String) = listOf(en, hi, bn)[i]

    // navigation
    val tabNearby = t("Nearby", "आसपास", "কাছাকাছি")
    val tabSearch = t("Find gate", "फाटक खोजें", "গেট খুঁজুন")
    val tabTrains = t("Trains", "ट्रेनें", "ট্রেন")

    // onboarding / permission
    val heroTitle = t("Know before you reach the phatak", "फाटक पहुँचने से पहले जानें", "ফাটকে পৌঁছানোর আগেই জানুন")
    val heroSub = t("Live open/closed status of railway gates around you, with the train that is coming and how late it is.",
        "आपके आस-पास के रेल फाटक खुले हैं या बंद, कौन सी ट्रेन आ रही है और कितनी लेट है: सब लाइव।",
        "আপনার আশেপাশের রেল গেট খোলা না বন্ধ, কোন ট্রেন আসছে আর কত দেরিতে: সব লাইভ।")
    val feat1 = t("Gates within 10–20 km, open or closed right now", "10–20 km के अंदर फाटक, अभी खुले या बंद", "10–20 km-এর মধ্যে গেট, এখন খোলা না বন্ধ")
    val feat2 = t("Exact closing and reopening time", "बंद होने और खुलने का सही समय", "বন্ধ ও খোলার সঠিক সময়")
    val feat3 = t("Live train delays, verified online", "ट्रेन की लाइव देरी, ऑनलाइन जाँची हुई", "ট্রেনের লাইভ দেরি, অনলাইনে যাচাই করা")
    val allowLocation = t("Allow location", "लोकेशन की अनुमति दें", "লোকেশনের অনুমতি দিন")
    val locationWhy = t("Your location stays on this phone. It is only used to find gates and stations near you.",
        "आपकी लोकेशन इसी फ़ोन पर रहती है। इसका उपयोग सिर्फ़ पास के फाटक और स्टेशन खोजने में होता है।",
        "আপনার লোকেশন এই ফোনেই থাকে। শুধু কাছের গেট আর স্টেশন খুঁজতে ব্যবহার হয়।")
    val deniedTitle = t("Location is needed", "लोकेशन ज़रूरी है", "লোকেশন দরকার")
    val deniedBody = t("Without your location Phatak Live can't know which gates are near you. You can still search a gate or a train by name.",
        "लोकेशन के बिना Phatak Live नहीं जान सकता कि आपके पास कौन से फाटक हैं। आप फिर भी नाम से फाटक या ट्रेन खोज सकते हैं।",
        "লোকেশন ছাড়া Phatak Live জানতে পারবে না আপনার কাছে কোন গেট আছে। তবুও নাম দিয়ে গেট বা ট্রেন খুঁজতে পারবেন।")
    val blockedBody = t("Location permission is turned off for Phatak Live. Turn it on in Settings → Permissions → Location.",
        "Phatak Live के लिए लोकेशन की अनुमति बंद है। Settings → Permissions → Location में चालू करें।",
        "Phatak Live-এর লোকেশন অনুমতি বন্ধ আছে। Settings → Permissions → Location-এ চালু করুন।")
    val openSettings = t("Open Settings", "सेटिंग्स खोलें", "সেটিংস খুলুন")
    val tryAgain = t("Try again", "फिर से कोशिश करें", "আবার চেষ্টা করুন")
    val continueWithout = t("Continue without location", "लोकेशन के बिना जारी रखें", "লোকেশন ছাড়া চালিয়ে যান")
    val gpsOff = t("Location (GPS) is off", "लोकेशन (GPS) बंद है", "লোকেশন (GPS) বন্ধ")
    val gpsOffBody = t("Turn it on to see gates near you.", "पास के फाटक देखने के लिए इसे चालू करें।", "কাছের গেট দেখতে এটি চালু করুন।")
    val turnOn = t("Turn on", "चालू करें", "চালু করুন")

    // header / status
    val locating = t("Finding your location…", "आपकी लोकेशन ढूंढ रहे हैं…", "আপনার লোকেশন খোঁজা হচ্ছে…")
    val updated = t("Updated", "अपडेट", "আপডেট")
    val offline = t("You're offline: times are from the timetable and not live-verified.",
        "आप ऑफ़लाइन हैं: समय टाइमटेबल से है, लाइव जाँचा नहीं गया।", "আপনি অফলাইন: সময় টাইমটেবিল থেকে, লাইভ যাচাই নয়।")
    val gatesNear = t("Rail gates near you", "आपके पास के रेल फाटक", "আপনার কাছের রেল গেট")
    val searching = t("Searching…", "खोज रहे हैं…", "খোঁজা হচ্ছে…")
    val noGates = t("No rail gate found on the map in this radius. Try 20 km or search a gate by name.",
        "इस दायरे में नक्शे पर कोई रेल फाटक नहीं मिला। 20 km चुनें या नाम से खोजें।",
        "এই দূরত্বে ম্যাপে কোনো রেল গেট পাওয়া যায়নি। 20 km বেছে নিন বা নাম দিয়ে খুঁজুন।")
    val stations = t("Nearest stations", "नज़दीकी स्टेशन", "কাছের স্টেশন")
    val opensIn = t("Opens in", "खुलेगा", "খুলবে")
    val closesIn = t("Closes in", "बंद होगा", "বন্ধ হবে")
    val min = t("min", "मिनट में", "মিনিটে")
    val clearFor = t("Clear for the next 3 hours", "अगले 3 घंटे खुला", "পরের ৩ ঘণ্টা খোলা")
    val open = t("OPEN", "खुला", "খোলা")
    val closed = t("CLOSED", "बंद", "বন্ধ")
    val closingSoon = t("CLOSING SOON", "जल्द बंद", "শীঘ্রই বন্ধ")
    val live = t("LIVE", "लाइव", "লাইভ")
    val scheduled = t("TIMETABLE", "टाइमटेबल", "টাইমটেবিল")
    val closes = t("Closes", "बंद", "বন্ধ")
    val reopens = t("Reopens", "खुलेगा", "খুলবে")
    val navigate = t("Directions", "रास्ता", "পথ")
    val near = t("near", "पास", "কাছে")
    val noTrain3h = t("No train in the next 3 hours.", "अगले 3 घंटे में कोई ट्रेन नहीं।", "পরের ৩ ঘণ্টায় কোনো ট্রেন নেই।")
    val liveData = t("Live", "लाइव", "লাইভ")
    val timetableOnly = t("Timetable", "टाइमटेबल", "টাইমটেবিল")
    val noStop = t("no stop", "रुकती नहीं", "থামে না")
    val onTime = t("on time", "समय पर", "সময়মতো")
    val late = t("late", "लेट", "দেরি")
    val more = t("more", "और", "আরও")

    // search
    val searchGateTitle = t("Find a rail gate", "रेल फाटक खोजें", "রেল গেট খুঁজুন")
    val searchGateSub = t("Type a gate number or place name: English, हिन्दी or বাংলা.", "फाटक नंबर या जगह का नाम लिखें: English, हिन्दी या বাংলা।",
        "গেট নম্বর বা জায়গার নাম লিখুন: English, हिन्दी বা বাংলা।")
    val gateHint = t("e.g. LC gate 24, or a town name", "जैसे LC गेट 24, या शहर का नाम", "যেমন LC গেট 24, বা শহরের নাম")
    val trainTitle = t("Live train status", "लाइव ट्रेन स्थिति", "লাইভ ট্রেনের অবস্থা")
    val trainSub = t("Train number gives the exact match; names work too.", "ट्रेन नंबर से सबसे सही नतीजा; नाम भी चलेगा।", "ট্রেন নম্বরে সবচেয়ে সঠিক ফল; নামও চলবে।")
    val trainHint = t("Train number or name, e.g. 12301", "ट्रेन नंबर या नाम, जैसे 12301", "ট্রেন নম্বর বা নাম, যেমন 12301")
    val go = t("Search", "खोजें", "খুঁজুন")
    val clear = t("Clear", "हटाएँ", "মুছুন")
    val didYouMean = t("Not an exact match. Did you mean:", "पूरा मेल नहीं मिला। क्या आपका मतलब:", "পুরো মিল পাওয়া যায়নি। আপনি কি বোঝাতে চেয়েছেন:")
    val nextStops = t("Next stops", "अगले स्टेशन", "পরের স্টেশন")
    val lastSeen = t("Last seen near", "आखिरी बार देखी गई", "শেষ দেখা গেছে")

    // problems (shown in the selected language)
    val waitingLocation = t("Waiting for your location…", "आपकी लोकेशन का इंतज़ार…", "আপনার লোকেশনের অপেক্ষা…")
    fun noGateFound(q: String) = t("No rail gate found for “$q”. Try a nearby town or station name.",
        "“$q” के लिए कोई रेल फाटक नहीं मिला। पास के शहर या स्टेशन का नाम लिखें।",
        "“$q”-এর জন্য কোনো রেল গেট পাওয়া যায়নি। কাছের শহর বা স্টেশনের নাম লিখুন।")
    fun noTrainFound(q: String) = t("No train found for “$q”. Try the 5-digit train number.",
        "“$q” के लिए कोई ट्रेन नहीं मिली। 5 अंकों का ट्रेन नंबर लिखें।",
        "“$q”-এর জন্য কোনো ট্রেন পাওয়া যায়নি। ৫ অঙ্কের ট্রেন নম্বর লিখুন।")
    val language = t("Language", "भाषा", "ভাষা")

    val footer = t(
        "Gates usually close about 10 minutes before a train reaches them and open about 3 minutes after it passes, so expect roughly 13 minutes per train. LIVE = confirmed by live train data; TIMETABLE = may be off if the train is late. Always obey the gate and the gateman.",
        "फाटक आमतौर पर ट्रेन पहुँचने से लगभग 10 मिनट पहले बंद होता है और गुज़रने के लगभग 3 मिनट बाद खुलता है, यानी हर ट्रेन पर करीब 13 मिनट। लाइव = लाइव डेटा से पक्का; टाइमटेबल = ट्रेन लेट हो तो बदल सकता है। हमेशा फाटक और गेटमैन की बात मानें।",
        "গেট সাধারণত ট্রেন পৌঁছানোর প্রায় ১০ মিনিট আগে বন্ধ হয় আর চলে যাওয়ার প্রায় ৩ মিনিট পরে খোলে, অর্থাৎ প্রতি ট্রেনে প্রায় ১৩ মিনিট। লাইভ = লাইভ ডেটায় নিশ্চিত; টাইমটেবিল = ট্রেন দেরি করলে বদলাতে পারে। সবসময় গেট আর গেটম্যানের কথা মানুন।",
    )
}
