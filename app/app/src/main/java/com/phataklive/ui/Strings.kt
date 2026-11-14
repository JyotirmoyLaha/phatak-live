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
    val liveFailed = t("Live train times couldn't be loaded just now, so times are from the timetable. Your internet is working.",
        "अभी लाइव ट्रेन समय नहीं मिल पाए, इसलिए समय टाइमटेबल से है। आपका इंटरनेट चल रहा है।",
        "এখন লাইভ ট্রেনের সময় পাওয়া গেল না, তাই সময় টাইমটেবিল থেকে। আপনার ইন্টারনেট চলছে।")
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

    // problem report after a crash or freeze
    val crashTitle = t("Sorry, Phatak Live stopped last time", "माफ़ करें, पिछली बार Phatak Live बंद हो गया", "দুঃখিত, গতবার Phatak Live বন্ধ হয়ে গিয়েছিল")
    val crashBody = t("Share a short problem report so it can be fixed? It has only the app version, phone model and the error, no location.",
        "क्या आप एक छोटी समस्या रिपोर्ट भेजेंगे ताकि इसे ठीक किया जा सके? इसमें सिर्फ़ ऐप वर्ज़न, फ़ोन मॉडल और गड़बड़ी है, लोकेशन नहीं।",
        "সমস্যাটা ঠিক করতে একটা ছোট রিপোর্ট পাঠাবেন? এতে শুধু অ্যাপের ভার্সন, ফোনের মডেল আর ত্রুটি আছে, লোকেশন নেই।")
    val crashShare = t("Share report", "रिपोर्ट भेजें", "রিপোর্ট পাঠান")
    val notNow = t("Not now", "अभी नहीं", "এখন নয়")

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
    val routeTitle = t("Fastest way across the railway", "पटरी पार करने का सबसे तेज़ रास्ता", "লাইন পার হওয়ার দ্রুততম পথ")
    val routeSub = t("Where are you going? Phatak Live compares the roads that take you there, with the likely wait at every rail gate on each, at the minute you get there.",
        "कहाँ जा रहे हैं? Phatak Live वहाँ पहुँचने के रास्तों की तुलना करता है, और बताता है कि आपके पहुँचने के समय हर रास्ते के रेल फाटक पर कितना इंतज़ार हो सकता है।",
        "কোথায় যাচ্ছেন? Phatak Live সেখানে যাওয়ার পথগুলো মিলিয়ে দেখে, আর আপনি যখন পৌঁছাবেন তখন প্রতিটি পথের রেল গেটে কত অপেক্ষা হতে পারে তা জানায়।")
    val from = t("From", "कहाँ से", "কোথা থেকে")
    val to = t("To", "कहाँ तक", "কোথায়")
    val myLocation = t("My location", "मेरी लोकेशन", "আমার লোকেশন")
    val useMyLocation = t("Use my location", "मेरी लोकेशन इस्तेमाल करें", "আমার লোকেশন ব্যবহার করুন")
    val change = t("Change", "बदलें", "বদলান")
    val fromHint = t("Place, gate or station", "जगह, फाटक या स्टेशन", "জায়গা, গেট বা স্টেশন")
    val toHint = t("Where to? e.g. hospital, market, town", "कहाँ जाना है? जैसे अस्पताल, बाज़ार, शहर", "কোথায় যাবেন? যেমন হাসপাতাল, বাজার, শহর")

    val modeBike = t("Two-wheeler", "दोपहिया", "টু-হুইলার")
    val modeCar = t("Car", "कार", "গাড়ি")
    val modeWalk = t("Walk", "पैदल", "হেঁটে")
    val findBest = t("Find best way", "सबसे अच्छा रास्ता खोजें", "সেরা পথ খুঁজুন")
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
    val openOnArrival = t("Likely open when you get there", "पहुँचने पर संभवतः खुला मिलेगा", "পৌঁছালে সম্ভবত খোলা পাবেন")
    fun nextCloseAt(time: String) = t("closes next at ~$time", "अगली बार ~$time पर बंद होगा", "পরের বার ~$time-এ বন্ধ হবে")
    fun closedWait(m: Int, time: String) = t("Likely closed when you get there: wait ~${mins(m)}, opens ~$time",
        "पहुँचने पर संभवतः बंद मिलेगा: ~${mins(m)} इंतज़ार, ~$time पर खुलेगा",
        "পৌঁছালে সম্ভবত বন্ধ পাবেন: ~${mins(m)} অপেক্ষা, ~$time-এ খুলবে")
    fun closedShort(m: Int) = t("likely closed, wait ~${mins(m)}", "संभवतः बंद, ~${mins(m)} इंतज़ार", "সম্ভবত বন্ধ, ~${mins(m)} অপেক্ষা")
    fun crossingOn(road: String) = t("Rail crossing on $road", "$road पर रेल क्रॉसिंग", "$road-এ রেল ক্রসিং")
    fun crossingNear(st: String) = t("Rail crossing near $st", "$st के पास रेल क्रॉसिंग", "$st-এর কাছে রেল ক্রসিং")
    fun via(road: String) = t("via $road", "$road से", "$road দিয়ে")
    val railCrossing = t("Rail crossing", "रेल क्रॉसिंग", "রেল ক্রসিং")
    val navStopNoteRoad = t("Google Maps will show a stop on this road to keep you on it. Don't stop there, just keep going.",
        "Google Maps आपको इसी रास्ते पर रखने के लिए एक स्टॉप दिखाएगा। वहाँ रुकना नहीं है, बस चलते रहें।",
        "Google Maps আপনাকে এই পথে রাখতে একটা স্টপ দেখাবে। সেখানে থামতে হবে না, শুধু চলতে থাকুন।")
    // simple card lines
    val likelyOpen = t("likely open", "संभवतः खुला", "সম্ভবত খোলা")
    fun likelyClosedWait(m: Int) = t("likely closed, wait ~${mins(m)}", "संभवतः बंद, ~${mins(m)} इंतज़ार", "সম্ভবত বন্ধ, ~${mins(m)} অপেক্ষা")
    val noGate = t("bridge or underpass, no gate", "पुल या अंडरपास, फाटक नहीं", "ব্রিজ বা আন্ডারপাস, গেট নেই")
    fun manyCrossings(n: Int, label: String) = t("$n × $label", "$n × $label", "$n × $label")
    val navStopShort = t("Google Maps will show a stop on the way. Just keep going.", "Google Maps रास्ते में एक स्टॉप दिखाएगा। बस चलते रहें।",
        "Google Maps পথে একটা স্টপ দেখাবে। শুধু চলতে থাকুন।")
    val navStopNote = t("Google Maps will show a stop just after this crossing. Don't stop there, just keep going.",
        "Google Maps इस क्रॉसिंग के ठीक बाद एक स्टॉप दिखाएगा। वहाँ रुकना नहीं है, बस चलते रहें।",
        "Google Maps এই ক্রসিংয়ের ঠিক পরে একটা স্টপ দেখাবে। সেখানে থামতে হবে না, শুধু চলতে থাকুন।")
    val sameSide = t("Google Maps' road for this trip doesn't cross a passenger railway line. Goods-only lines and sidings are not checked.",
        "इस यात्रा का Google Maps रास्ता किसी यात्री रेल लाइन को पार नहीं करता। सिर्फ़ मालगाड़ी वाली लाइनें और साइडिंग नहीं जाँची जातीं।",
        "এই যাত্রার Google Maps পথ কোনো যাত্রীবাহী রেল লাইন পার হয় না। শুধু মালগাড়ির লাইন আর সাইডিং দেখা হয় না।")
    val neverCloses = t("Bridge or underpass on the map: no gate expected", "नक्शे पर पुल या अंडरपास: फाटक की उम्मीद नहीं",
        "ম্যাপে ব্রিজ বা আন্ডারপাস: গেট থাকার কথা নয়")
    val navigateBtn = t("Navigate", "नेविगेट करें", "নেভিগেট করুন")
    val mapBtn = t("Map", "नक्शा", "ম্যাপ")
    val noCrossings = t("No rail gate, overbridge or underpass found on the map between these places.",
        "इन जगहों के बीच नक्शे पर कोई रेल फाटक, ओवरब्रिज या अंडरपास नहीं मिला।",
        "এই জায়গাগুলোর মধ্যে ম্যাপে কোনো রেল গেট, ওভারব্রিজ বা আন্ডারপাস পাওয়া যায়নি।")
    val routeFailed = t("Couldn't get routes from Google Maps. Check your internet and try again.",
        "Google Maps से रास्ता नहीं मिला। इंटरनेट जाँचें और फिर कोशिश करें।",
        "Google Maps থেকে পথ পাওয়া যায়নি। ইন্টারনেট দেখে আবার চেষ্টা করুন।")
    fun placeNotFound(q: String) = t("Couldn't find “$q”. Try another name.", "“$q” नहीं मिला। कोई दूसरा नाम लिखें।", "“$q” পাওয়া যায়নি। অন্য নাম লিখুন।")
    val routeSafety = t("Estimates only. A gate can close at any time, also for goods trains. Always obey the gate, the signals and the gateman.",
        "सिर्फ़ अनुमान। फाटक कभी भी बंद हो सकता है, मालगाड़ी के लिए भी। हमेशा फाटक, सिग्नल और गेटमैन की बात मानें।",
        "শুধু আনুমানিক। গেট যেকোনো সময় বন্ধ হতে পারে, মালগাড়ির জন্যও। সবসময় গেট, সিগন্যাল আর গেটম্যানের কথা মানুন।")
    fun tripOn(name: String) = t("Trip alerts on · via $name", "यात्रा अलर्ट चालू · $name से", "যাত্রার অ্যালার্ট চালু · $name দিয়ে")
    val tripAlertsOn = t("Trip alerts on", "यात्रा अलर्ट चालू", "যাত্রার অ্যালার্ট চালু")
    val tripOnSub = t("Updates every 30 seconds from your GPS, also while you drive with Google Maps.",
        "आपके GPS से हर 30 सेकंड में अपडेट, Google Maps से चलाते समय भी।",
        "আপনার GPS থেকে প্রতি ৩০ সেকেন্ডে আপডেট, Google Maps দিয়ে চালানোর সময়ও।")
    val tripStarting = t("Getting your position…", "आपकी पोज़िशन ले रहे हैं…", "আপনার অবস্থান নেওয়া হচ্ছে…")
    val stopAlerts = t("Stop alerts", "अलर्ट बंद करें", "অ্যালার্ট বন্ধ করুন")
    val tripPassed = t("You've crossed the railway. Trip alerts stopped.", "आपने पटरी पार कर ली। यात्रा अलर्ट बंद।", "আপনি লাইন পার হয়েছেন। যাত্রার অ্যালার্ট বন্ধ।")
    fun closedOnArrival(name: String, m: Int) = t("$name is likely closed when you get there: ~${mins(m)} wait",
        "$name पहुँचने पर संभवतः बंद मिलेगा: ~${mins(m)} इंतज़ार", "$name পৌঁছালে সম্ভবত বন্ধ পাবেন: ~${mins(m)} অপেক্ষা")
    fun opensAtTime(time: String) = t("Likely opens ~$time. Never cross a closed gate.", "संभवतः ~$time पर खुलेगा। बंद फाटक कभी पार न करें।",
        "সম্ভবত ~$time-এ খুলবে। বন্ধ গেট কখনো পার হবেন না।")
    val chTrip = t("Trip status", "यात्रा की स्थिति", "যাত্রার অবস্থা")
    val chAlert = t("Trip alerts", "यात्रा अलर्ट", "যাত্রার অ্যালার্ট")
    val allowNotifications = t("Allow notifications to get trip alerts while you drive.",
        "चलाते समय यात्रा अलर्ट पाने के लिए नोटिफ़िकेशन की अनुमति दें।", "চালানোর সময় যাত্রার অ্যালার্ট পেতে নোটিফিকেশনের অনুমতি দিন।")

    val close = t("Close", "बंद करें", "বন্ধ করুন")
    val startOnRailway = t("Your start is on the railway itself, so Google Maps picks which side to start from. For the right side, leave From empty and use your location.",
        "आपकी शुरुआत की जगह पटरी पर ही है, इसलिए Google Maps खुद तय करेगा कि किस तरफ़ से शुरू करें। सही तरफ़ के लिए \"कहाँ से\" खाली छोड़ें और अपनी लोकेशन इस्तेमाल करें।",
        "আপনার শুরুর জায়গা লাইনের উপরেই, তাই Google Maps নিজেই ঠিক করবে কোন দিক থেকে শুরু হবে। সঠিক দিকের জন্য \"কোথা থেকে\" খালি রাখুন আর নিজের লোকেশন ব্যবহার করুন।")
    val railsNotChecked = t("Couldn't check the railway on this trip right now. Pull down to try again.",
        "अभी इस यात्रा की रेल लाइन नहीं जाँच पाए। फिर से कोशिश करने के लिए नीचे खींचें।",
        "এখন এই যাত্রার রেল লাইন দেখা গেল না। আবার চেষ্টা করতে নিচে টানুন।")
    val crossingsOnWay = t("Railway crossings on this way", "इस रास्ते पर रेल क्रॉसिंग", "এই পথে রেল ক্রসিং")
    val alsoOnTheWay = t("Also on the way", "रास्ते में और भी", "পথে আরও")
    val mapDrawnNote = t("Simple map from Google Maps' road and railway lines", "Google Maps की सड़क और रेल लाइन से बना सादा नक्शा",
        "Google Maps-এর রাস্তা আর রেল লাইন থেকে আঁকা সহজ ম্যাপ")
    val mapFit = t("Show whole route", "पूरा रास्ता दिखाएँ", "পুরো পথ দেখান")
    fun tooFar(km: Int) = t("Route checks trips up to $km km. Choose a nearer destination.", "रास्ता $km km तक की यात्रा जाँचता है। कोई नज़दीकी जगह चुनें।",
        "রাস্তা $km km পর্যন্ত যাত্রা দেখে। কাছের কোনো জায়গা বেছে নিন।")
    // one-time safety notice before Route is used
    val noticeTitle = t("Please read before using Route", "रास्ता इस्तेमाल करने से पहले पढ़ें", "রাস্তা ব্যবহারের আগে পড়ুন")
    val notice1 = t("Gate times are estimates from train timetables and live train data. They can be wrong.",
        "फाटक का समय ट्रेन टाइमटेबल और लाइव ट्रेन जानकारी से अनुमानित है। यह गलत हो सकता है।",
        "গেটের সময় ট্রেনের টাইমটেবিল আর লাইভ ট্রেনের তথ্য থেকে আনুমানিক। এটি ভুল হতে পারে।")
    val notice2 = t("A gate can close at any time, also for goods trains, which are not shown.",
        "फाटक कभी भी बंद हो सकता है, मालगाड़ियों के लिए भी, जो इसमें नहीं दिखतीं।",
        "গেট যেকোনো সময় বন্ধ হতে পারে, মালগাড়ির জন্যও, যা এখানে দেখানো হয় না।")
    val notice3 = t("Always obey the gate, the signals and the gateman. Never cross a closed gate or the tracks when a train is coming.",
        "हमेशा फाटक, सिग्नल और गेटमैन की बात मानें। बंद फाटक या ट्रेन आते समय पटरी कभी पार न करें।",
        "সবসময় গেট, সিগন্যাল আর গেটম্যানের কথা মানুন। বন্ধ গেট বা ট্রেন আসার সময় লাইন কখনো পার হবেন না।")
    val notice4 = t("Roads come from Google Maps. Follow road signs and traffic rules, and don't use the phone while driving.",
        "रास्ते Google Maps से हैं। सड़क के संकेत और ट्रैफ़िक नियम मानें, और चलाते समय फ़ोन का इस्तेमाल न करें।",
        "রাস্তা Google Maps থেকে। রাস্তার চিহ্ন আর ট্রাফিক নিয়ম মানুন, আর চালানোর সময় ফোন ব্যবহার করবেন না।")
    val noticeAccept = t("I understand", "मैं समझ गया/गई", "আমি বুঝেছি")

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
