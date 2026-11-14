package com.phataklive.ui

/** All UI text in English, Hindi and Bengali; the user switches with EN / हि / বা (first launch follows the phone language). */
class Strings(lang: String) {
    private val i = when (lang) { "Hindi" -> 1; "Bengali" -> 2; else -> 0 }
    private fun t(en: String, hi: String, bn: String) = listOf(en, hi, bn)[i]

    // navigation
    val tabNearby = t("Nearby", "आसपास", "কাছাকাছি")
    val tabSearch = t("Find gate", "फाटक खोजें", "গেট খুঁজুন")
    val tabTrains = t("Trains", "ट्रेनें", "ট্রেন")
    val tabRoute = t("Route", "रास्ता", "রাস্তা")

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
    val radiusCustom = t("Custom", "अपना", "নিজের মতো")
    val radiusCustomHint = t("Distance, 1 to 100", "दूरी, 1 से 100", "দূরত্ব, ১ থেকে ১০০")
    val apply = t("Apply", "लागू करें", "প্রয়োগ")
    val radiusTooBig = t("Phatak Live can scan up to 100 km around you. For a specific town or station farther away, search it in Find gate.",
        "Phatak Live आपके आसपास 100 km तक ही खोज सकता है। दूर के किसी खास शहर या स्टेशन के लिए उसे \"फाटक खोजें\" में खोजें।",
        "Phatak Live আপনার চারপাশে ১০০ km পর্যন্ত খুঁজতে পারে। দূরের নির্দিষ্ট কোনো শহর বা স্টেশনের জন্য \"গেট খুঁজুন\"-এ খুঁজুন।")
    val radiusTooSmall = t("Enter a distance from 1 to 100 km.", "1 से 100 km के बीच की दूरी लिखें।", "১ থেকে ১০০ km-এর মধ্যে দূরত্ব লিখুন।")
    val done = t("Done", "ठीक है", "ঠিক আছে")

    // route: best way across the railway
    val routeTitle = t("Best crossing", "पटरी पार करने का सबसे अच्छा रास्ता", "লাইন পার হওয়ার সেরা পথ")
    val routeSub = t("Where are you going? Phatak Live compares the rail gates, overbridges and underpasses on the way, with the wait at each gate at the minute you get there.",
        "कहाँ जा रहे हैं? Phatak Live रास्ते के रेल फाटक, ओवरब्रिज और अंडरपास की तुलना करता है, और बताता है कि आपके पहुँचने के समय हर फाटक पर कितना इंतज़ार होगा।",
        "কোথায় যাচ্ছেন? Phatak Live পথের রেল গেট, ওভারব্রিজ আর আন্ডারপাস মিলিয়ে দেখে, আর আপনি যখন পৌঁছাবেন তখন প্রতিটি গেটে কত অপেক্ষা হবে তা জানায়।")
    val from = t("From", "कहाँ से", "কোথা থেকে")
    val to = t("To", "कहाँ तक", "কোথায়")
    val myLocation = t("My location", "मेरी लोकेशन", "আমার লোকেশন")
    val fromHint = t("Place, gate or station", "जगह, फाटक या स्टेशन", "জায়গা, গেট বা স্টেশন")
    val toHint = t("Where to? e.g. hospital, market, town", "कहाँ जाना है? जैसे अस्पताल, बाज़ार, शहर", "কোথায় যাবেন? যেমন হাসপাতাল, বাজার, শহর")
    val atGate = t("I'm at", "मैं यहाँ हूँ", "আমি আছি")
    val modeBike = t("Two-wheeler", "दोपहिया", "টু-হুইলার")
    val modeCar = t("Car", "कार", "গাড়ি")
    val modeWalk = t("Walk", "पैदल", "হেঁটে")
    val findBest = t("Find best crossing", "सबसे अच्छा रास्ता खोजें", "সেরা পথ খুঁজুন")
    val stepCrossings = t("Finding gates and bridges on the way…", "रास्ते के फाटक और पुल खोज रहे हैं…", "পথের গেট আর ব্রিজ খোঁজা হচ্ছে…")
    val stepRoutes = t("Checking each road on Google Maps…", "Google Maps पर हर रास्ता जाँच रहे हैं…", "Google Maps-এ প্রতিটি পথ দেখা হচ্ছে…")
    val stepTrains = t("Checking live trains at each gate…", "हर फाटक की लाइव ट्रेनें जाँच रहे हैं…", "প্রতিটি গেটের লাইভ ট্রেন দেখা হচ্ছে…")
    val best = t("BEST", "सबसे अच्छा", "সেরা")
    val googleWay = t("Google's usual way", "Google का सामान्य रास्ता", "Google-এর সাধারণ পথ")
    val kindGate = t("Rail gate", "रेल फाटक", "রেল গেট")
    val kindOver = t("Overbridge", "ओवरब्रिज", "ওভারব্রিজ")
    val kindUnder = t("Underpass", "अंडरपास", "আন্ডারপাস")
    val kindFoot = t("Foot overbridge", "फुट ओवरब्रिज", "ফুট ওভারব্রিজ")
    /** "1 h 5 min" as an amount (not "in 5 min") */
    fun mins(m: Int) = if (m < 60) t("$m min", "$m मिनट", "$m মিনিট")
        else if (m % 60 == 0) t("${m / 60} h", "${m / 60} घंटा", "${m / 60} ঘণ্টা")
        else t("${m / 60} h ${m % 60} min", "${m / 60} घंटा ${m % 60} मिनट", "${m / 60} ঘণ্টা ${m % 60} মিনিট")
    fun total(m: Int) = t("${mins(m)} in all", "कुल ${mins(m)}", "মোট ${mins(m)}")
    fun reachIn(m: Int, time: String) = if (m <= 0) t("You're at it now", "आप अभी यहीं हैं", "আপনি এখন এখানেই")
        else t("You reach it in ${mins(m)} (~$time)", "${mins(m)} में पहुँचेंगे (~$time)", "${mins(m)}-এ পৌঁছাবেন (~$time)")
    val openOnArrival = t("Open when you get there", "पहुँचने पर खुला मिलेगा", "পৌঁছালে খোলা পাবেন")
    fun nextCloseAt(time: String) = t("closes next at ~$time", "अगली बार ~$time पर बंद होगा", "পরের বার ~$time-এ বন্ধ হবে")
    fun closedWait(m: Int, time: String) = t("Closed when you get there: wait ~${mins(m)}, opens ~$time",
        "पहुँचने पर बंद मिलेगा: ~${mins(m)} इंतज़ार, ~$time पर खुलेगा",
        "পৌঁছালে বন্ধ পাবেন: ~${mins(m)} অপেক্ষা, ~$time-এ খুলবে")
    fun closedShort(m: Int) = t("closed, wait ~${mins(m)}", "बंद, ~${mins(m)} इंतज़ार", "বন্ধ, ~${mins(m)} অপেক্ষা")
    val neverCloses = t("No gate: never closes for trains", "फाटक नहीं: ट्रेन के लिए कभी बंद नहीं होता", "গেট নেই: ট্রেনের জন্য কখনো বন্ধ হয় না")
    val navigateBtn = t("Navigate", "नेविगेट करें", "নেভিগেট করুন")
    val mapBtn = t("Map", "नक्शा", "ম্যাপ")
    fun directFaster(m: Int) = t("Google's fastest road takes ${mins(m)}. It may not cross the railway, or it crosses at a place that has no name on the map.",
        "Google का सबसे तेज़ रास्ता ${mins(m)} का है। हो सकता है वह पटरी पार न करे, या ऐसी जगह से पार करे जिसका नक्शे पर नाम नहीं है।",
        "Google-এর সবচেয়ে দ্রুত পথে ${mins(m)} লাগে। হয়তো সেটি লাইন পার হয় না, বা এমন জায়গা দিয়ে পার হয় যার ম্যাপে নাম নেই।")
    val noCrossings = t("No rail gate, overbridge or underpass found on the map between these places.",
        "इन जगहों के बीच नक्शे पर कोई रेल फाटक, ओवरब्रिज या अंडरपास नहीं मिला।",
        "এই জায়গাগুলোর মধ্যে ম্যাপে কোনো রেল গেট, ওভারব্রিজ বা আন্ডারপাস পাওয়া যায়নি।")
    val routeFailed = t("Couldn't get routes from Google Maps. Check your internet and try again.",
        "Google Maps से रास्ता नहीं मिला। इंटरनेट जाँचें और फिर कोशिश करें।",
        "Google Maps থেকে পথ পাওয়া যায়নি। ইন্টারনেট দেখে আবার চেষ্টা করুন।")
    fun placeNotFound(q: String) = t("Couldn't find “$q”. Try another name.", "“$q” नहीं मिला। कोई दूसरा नाम लिखें।", "“$q” পাওয়া যায়নি। অন্য নাম লিখুন।")
    val routeSafety = t("Never cross a closed gate. Gate times are estimates and goods trains are not shown.",
        "बंद फाटक कभी पार न करें। फाटक का समय अनुमानित है और मालगाड़ियाँ इसमें नहीं दिखतीं।",
        "বন্ধ গেট কখনো পার হবেন না। গেটের সময় আনুমানিক, আর মালগাড়ি এখানে দেখানো হয় না।")
    fun tripOn(name: String) = t("Trip alerts on · via $name", "यात्रा अलर्ट चालू · $name से", "যাত্রার অ্যালার্ট চালু · $name দিয়ে")
    val tripAlertsOn = t("Trip alerts on", "यात्रा अलर्ट चालू", "যাত্রার অ্যালার্ট চালু")
    val tripOnSub = t("Updates every 30 seconds from your GPS, also while you drive with Google Maps.",
        "आपके GPS से हर 30 सेकंड में अपडेट, Google Maps से चलाते समय भी।",
        "আপনার GPS থেকে প্রতি ৩০ সেকেন্ডে আপডেট, Google Maps দিয়ে চালানোর সময়ও।")
    val tripStarting = t("Getting your position…", "आपकी पोज़िशन ले रहे हैं…", "আপনার অবস্থান নেওয়া হচ্ছে…")
    val stopAlerts = t("Stop alerts", "अलर्ट बंद करें", "অ্যালার্ট বন্ধ করুন")
    val tripPassed = t("You've crossed the railway. Trip alerts stopped.", "आपने पटरी पार कर ली। यात्रा अलर्ट बंद।", "আপনি লাইন পার হয়েছেন। যাত্রার অ্যালার্ট বন্ধ।")
    fun closedOnArrival(name: String, m: Int) = t("$name will be closed when you get there: ~${mins(m)} wait",
        "$name पहुँचने पर बंद मिलेगा: ~${mins(m)} इंतज़ार", "$name পৌঁছালে বন্ধ পাবেন: ~${mins(m)} অপেক্ষা")
    fun opensAtTime(time: String) = t("Opens ~$time. Check Phatak Live for a faster way.", "~$time पर खुलेगा। तेज़ रास्ते के लिए Phatak Live देखें।",
        "~$time-এ খুলবে। দ্রুত পথের জন্য Phatak Live দেখুন।")
    fun switchTo(name: String, m: Int) = t("Faster now: go via $name, saves ~${mins(m)}", "अब तेज़: $name से जाएँ, ~${mins(m)} बचेंगे",
        "এখন দ্রুত: $name দিয়ে যান, ~${mins(m)} বাঁচবে")
    val chTrip = t("Trip status", "यात्रा की स्थिति", "যাত্রার অবস্থা")
    val chAlert = t("Trip alerts", "यात्रा अलर्ट", "যাত্রার অ্যালার্ট")
    val allowNotifications = t("Allow notifications to get trip alerts while you drive.",
        "चलाते समय यात्रा अलर्ट पाने के लिए नोटिफ़िकेशन की अनुमति दें।", "চালানোর সময় যাত্রার অ্যালার্ট পেতে নোটিফিকেশনের অনুমতি দিন।")
    val mapLiveHint = t("Tap the location button on the map to follow your live position.",
        "अपनी लाइव पोज़िशन देखने के लिए नक्शे पर लोकेशन बटन दबाएँ।", "আপনার লাইভ অবস্থান দেখতে ম্যাপে লোকেশন বোতাম চাপুন।")
    val close = t("Close", "बंद करें", "বন্ধ করুন")

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
