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
    val searchGateSub = t("Any gate in India: type a gate, town or station name in English, हिन्दी or বাংলা. Small spelling mistakes are fine.",
        "भारत का कोई भी फाटक: फाटक, शहर या स्टेशन का नाम English, हिन्दी या বাংলা में लिखें। छोटी स्पेलिंग गलती चलेगी।",
        "ভারতের যেকোনো গেট: গেট, শহর বা স্টেশনের নাম English, हिन्दी বা বাংলায় লিখুন। ছোটখাটো বানান ভুল চলবে।")
    fun gatesNearStation(st: String) = t("Gates near $st station", "$st स्टेशन के पास के फाटक", "$st স্টেশনের কাছের গেট")
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

    // app update
    val updateTitle = t("Software Update", "सॉफ़्टवेयर अपडेट", "সফটওয়্যার আপডেট")
    fun updateAvailable(v: String) = t("Phatak Live $v is available to download.", "Phatak Live $v डाउनलोड के लिए उपलब्ध है।",
        "Phatak Live $v ডাউনলোডের জন্য উপলব্ধ।")
    val whatsNew = t("WHAT'S NEW", "नया क्या है", "নতুন কী আছে")
    val downloadNow = t("Download Now", "अभी डाउनलोड करें", "এখনই ডাউনলোড করুন")
    val downloadingUpdate = t("Downloading…", "डाउनलोड हो रहा है…", "ডাউনলোড হচ্ছে…")
    val installNow = t("Install Now", "अभी इंस्टॉल करें", "এখনই ইনস্টল করুন")
    val remindLater = t("Remind Me Later", "बाद में याद दिलाएँ", "পরে মনে করিয়ে দিন")
    val openReleasePage = t("Open Download Page", "डाउनलोड पेज खोलें", "ডাউনলোড পেজ খুলুন")
    val updateFailed = t("Download failed. Check your internet and try again.", "डाउनलोड नहीं हुआ। इंटरनेट जाँचें और फिर कोशिश करें।",
        "ডাউনলোড হয়নি। ইন্টারনেট দেখে আবার চেষ্টা করুন।")
    val allowInstall = t("Allow “Install unknown apps” for Phatak Live, then come back and tap Install Now.",
        "Phatak Live के लिए “Install unknown apps” चालू करें, फिर वापस आकर Install Now दबाएँ।",
        "Phatak Live-এর জন্য “Install unknown apps” চালু করুন, তারপর ফিরে এসে Install Now চাপুন।")
    val cancel = t("Cancel", "रद्द करें", "বাতিল")

    // plain-language bits
    /** "4 min" / "2 h 39 min" */
    fun duration(m: Int) = if (m < 60) t("$m min", "$m मिनट में", "$m মিনিটে")
        else if (m % 60 == 0) t("${m / 60} h", "${m / 60} घंटे में", "${m / 60} ঘণ্টায়")
        else t("${m / 60} h ${m % 60} min", "${m / 60} घंटे ${m % 60} मिनट में", "${m / 60} ঘণ্টা ${m % 60} মিনিটে")
    fun delayed(m: Int) = t("Delayed $m min", "$m मिनट लेट", "$m মিনিট দেরি")
    fun accuracy(m: Int) = if (m <= 100) t("accurate to $m m", "$m मीटर तक सटीक", "$m মিটার পর্যন্ত নির্ভুল")
        else t("approximate location", "अनुमानित लोकेशन", "আনুমানিক লোকেশন")

    // settings panel
    val settings = t("Settings", "सेटिंग्स", "সেটিংস")
    val theme = t("Theme", "थीम", "থিম")
    val themeAuto = t("Auto", "ऑटो", "অটো")
    val themeLight = t("Light", "लाइट", "লাইট")
    val themeDark = t("Dark", "डार्क", "ডার্ক")
    val themeHint = t("Auto follows your phone's light or dark setting.", "ऑटो आपके फ़ोन की लाइट या डार्क सेटिंग के हिसाब से चलता है।",
        "অটো আপনার ফোনের লাইট বা ডার্ক সেটিং মেনে চলে।")
    val radius = t("Search radius", "खोज का दायरा", "খোঁজার পরিধি")
    val radiusHint = t("How far around you to look for rail gates and stations.", "आपके आसपास कितनी दूरी तक रेल फाटक और स्टेशन खोजें।",
        "আপনার চারপাশে কত দূর পর্যন্ত রেল গেট আর স্টেশন খুঁজবে।")
    val done = t("Done", "ठीक है", "ঠিক আছে")

    // info box
    val infoTitle = t("How gate times work", "फाटक का समय कैसे पता चलता है", "গেটের সময় কীভাবে জানা যায়")
    val info1 = t("A gate closes when the station master sends the train towards it, usually as the train leaves the station before the gate: about 4 to 15 minutes before it arrives. It opens about 3 minutes after the whole train has passed, if no other train is coming.",
        "स्टेशन मास्टर जब ट्रेन को फाटक की ओर भेजते हैं, तब फाटक बंद होता है, आमतौर पर जब ट्रेन फाटक से पहले वाले स्टेशन से निकलती है: ट्रेन आने से करीब 4 से 15 मिनट पहले। पूरी ट्रेन निकलने के करीब 3 मिनट बाद फाटक खुलता है, अगर कोई दूसरी ट्रेन न आ रही हो।",
        "স্টেশন মাস্টার যখন ট্রেনকে গেটের দিকে পাঠান, তখন গেট বন্ধ হয়, সাধারণত ট্রেন গেটের আগের স্টেশন ছাড়ার সময়: ট্রেন আসার প্রায় ৪ থেকে ১৫ মিনিট আগে। পুরো ট্রেন চলে যাওয়ার প্রায় ৩ মিনিট পরে গেট খোলে, যদি অন্য কোনো ট্রেন না আসে।")
    val info2 = t("LIVE means the time is checked with the train's live running. TIMETABLE means the normal schedule; it can change if the train is late.",
        "लाइव = ट्रेन की लाइव स्थिति से जाँचा गया समय। टाइमटेबल = सामान्य समय, ट्रेन लेट हो तो बदल सकता है।",
        "লাইভ = ট্রেনের লাইভ অবস্থা দেখে যাচাই করা সময়। টাইমটেবিল = সাধারণ সময়, ট্রেন দেরি করলে বদলাতে পারে।")
    val info3 = t("These are estimates, and goods trains are not shown, so a gate can also close for them. Always follow the gate, the signals and the gateman.",
        "ये अनुमानित समय हैं। मालगाड़ियाँ इसमें नहीं दिखतीं, उनके लिए भी फाटक बंद हो सकता है। हमेशा फाटक, सिग्नल और गेटमैन की बात मानें।",
        "এগুলো আনুমানিক সময়। মালগাড়ি এখানে দেখানো হয় না, তাদের জন্যও গেট বন্ধ হতে পারে। সবসময় গেট, সিগন্যাল আর গেটম্যানের কথা মানুন।")
}
