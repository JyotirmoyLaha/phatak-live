<div align="center">

# 🚦 Phatak Live

### Know before you reach the railway gate.

**Live open/closed status of every railway level crossing (*phatak*) around you, anywhere in India,
with the exact closing and reopening time, the train causing it, and how late that train is.**

[**⬇ Download the latest APK**](https://github.com/JyotirmoyLaha/phatak-live/releases/latest)

`Android 10+` · `8 MB` · `No account, no ads, no tracking` · `English · हिन्दी · বাংলা`

[Privacy Policy](PRIVACY.md) · [MIT License](LICENSE) · [Contact](https://github.com/JyotirmoyLaha/phatak-live/issues)

</div>

---

## Why this app exists

India still has **17,777 railway level crossings, 97% of them manned gates** (Indian Railways, April 2024, via Dataful).
Every day millions of people on two-wheelers, autos, buses, ambulances and school vans reach a gate that has just
gone down, and wait without knowing for how long.

That waiting is not just lost time:

- **22,695 accidents at railway level crossings** were recorded by the NCRB between 2014 and 2024. Not knowing how
  long a gate will stay down is exactly what tempts people to rush it; knowing the time removes that guess.
- In the Eastern Railway zone alone, an average of **31 trains a day were delayed in 2024-25** because road traffic
  stopped gates from closing on time (Millennium Post, 2025).

Phatak Live answers the one question nobody answers today, before you leave home: ***"Is the phatak open, and if not,
when will it open?"***

### Who it helps

| Who | How |
|---|---|
| 🛵 **Daily commuters** | Check the gate on the way; leave 5 minutes earlier or later instead of waiting 15 at a closed gate. |
| 🚑 **Ambulance and emergency drivers** | See that the gate ahead closes in 3 minutes and take another crossing or an overbridge, while it still matters. |
| 🚸 **Parents and school vans** | Know whether the gate near school will be down at drop-off time. |
| 📦 **Delivery riders and cab drivers** | Fewer minutes lost at gates means more trips per day. |
| 🌾 **Villages with one road across the track** | Plan market trips around the gate times. |
| 🚉 **People going to the station** | Check a train's live delay, platform and next stops, and whether the gate on the way will close for it. |
| 🧭 **Anyone in an unfamiliar town** | Search any gate by name, in English, Hindi or Bengali. |

---

## How it works

Phatak Live **computes** each gate's status from train movements. It doesn't wait for someone to report it.

1. **Where you are.** GPS gives your location; the app shows the place name and the railway stations within
   10, 15 or 20 km from its built-in rail database covering all of India.
2. **Which gates are near.** Rail gates and level crossings around you, and near each nearby station, are looked up
   live on the map. Look-alikes such as station entrances or streets named "Railway Gate Road" are filtered out.
3. **Which trains pass them.** The built-in timetable covers trains across India, **including stations a
   train passes without stopping**: those trains close gates too.
4. **When each train reaches the gate.** The Gate Engine places the gate on the track between two consecutive
   stations and interpolates the train's time at the gate by distance.
5. **Live check.** Live departures at the nearby stations give each train's real expected time, so the delay is
   applied to the gate. Every time is marked **LIVE** (confirmed by live data) or **TIMETABLE** (not live-confirmed).
6. **Gate status.** Closing windows are built and merged, then each gate is shown as **OPEN**, **CLOSING SOON** or
   **CLOSED**, with a countdown and the train(s) causing it.

### Gate timing rule

There is no single published "close X minutes before the train" figure for Indian Railways; the gateman closes the
gate on the station master's instruction and reopens only after the whole train has passed and the station master
allows it. Phatak Live's rule follows what is reported from the ground:

| | Phatak Live | Evidence |
|---|---|---|
| Gate closes | **~10 min before** the train reaches the gate | Gatemen get the station master's call about 15–20 min before a train ([report](https://www.dtnext.in/news/tamilnadu/lc-gate-closed-at-706-am-bikers-forced-gatekeeper-partially-open-it-839516)); at non-interlocked gates a wait of about 5–10 min after closing is observed ([ETV Bharat, Sept 2026](https://www.etvbharat.com/en/bharat/railways-asks-zones-to-modify-rules-to-strengthen-safety-at-non-interlocked-level-crossing-gates-enn26091703952)) |
| Gate opens | **~3 min after** the train has passed | Reopening needs a tail-board check and the station master's permission (same ETV Bharat report) |
| Total per train | **~13 min** | A gateman at a busy Guwahati crossing reported 10–15 min per train, ~13 min on average (Guwahati Plus) |
| Trains close together | windows less than 2 min apart are merged: the gate stays down | |
| Gate within 600 m of a station | time taken midway between the train's arrival and departure there | |
| Gate between stations | interpolated by distance along the track between two consecutive stations | |
| Live train missing from the timetable (specials) | live time at the nearest station ± travel time at ~40 km/h | |

Real closures vary by gate (interlocked or not, busy or quiet line) and by gateman, so the app always shows times as
estimates ("~") and says whether the train time behind them is live.

---

## How Phatak Live differs from other apps

We looked at the apps that already try to answer "is the crossing open?" (as of November 2026):

| App | How it works | Coverage | Weak points |
|---|---|---|---|
| **Fatak – Live Crossing Status** (Android, fatak.app) | Status for a fixed list of crossings; data said to come from NTES ([Tribune, July 2026](https://www.tribuneindia.com/news/punjab/family-tragedy-inspires-muktsar-youth-to-build-railway-crossing-alert-application/)) | **14 crossings in 7 towns of Punjab** (July 2026) | Only crossings the team has added; method not explained in the app listing; ad-supported with Google AdMob plus Firebase and Mixpanel analytics ([AppGoblin](https://appgoblin.info/apps/com.fatakv2)) |
| **RailRadar** (iOS) | Crowd reports: users tap Open / Blocked ([App Store](https://apps.apple.com/us/app/id6749937816)) | Wherever users report | Status is only as fresh as the last report; no closing time; iPhone only |
| **Train-status apps** (ixigo Trains, RailYatri, NTES, …) | Live running status of a train you search | All India | Built for passengers: no gates, and trains that pass a station without stopping are usually not shown |

**Why Phatak Live is different**

| | Fatak | Crowd-report apps | Train-status apps | **Phatak Live** |
|---|---|---|---|---|
| Coverage | 14 listed crossings, Punjab | where people report | no gates | **any rail gate on the map, anywhere in India** |
| How status is known | not explained | someone reports it | n/a | **computed from every train's timing + live delay** |
| Closing *and* reopening time, countdown | not stated | ✗ | ✗ | **✓** |
| Trains that pass without stopping | not stated | ✗ | usually hidden | **✓ included** |
| Says when data is live vs. timetable | not stated | ✗ | partly | **✓ every time labelled** |
| Live train status by number / name | not stated | ✗ | ✓ | **✓** |
| Languages | not stated | varies | mostly English | **English · हिन्दी · বাংলা, switchable** |
| Ads / tracking | ads + analytics SDKs | — | usually ads | **none** |
| Open-close notifications | ✓ | some | ✗ | not yet (on the roadmap) |

Fatak shows there is real demand: thousands of people check their local crossing every day. Phatak Live's approach
**computes** the status for any gate instead of curating a list or waiting for reports, so it works on the first day
in any town in India.

---

## Features

- **Opens straight to your area**: place name, nearest stations, every rail gate found around you.
- **Hero gate card**: pulsing state light, big countdown ("Opens in 4 min"), closing-window timeline with a "now" marker.
- **Gate list** with OPEN / CLOSING SOON / CLOSED, LIVE / TIMETABLE badge, trains and their delays, directions.
- **Find gate**: by gate number, town or station, typed in English, Hindi or Bengali.
- **Trains**: live delay, last seen near, next stops with platforms; "Did you mean…?" when a typed name is ambiguous.
  Train numbers can be typed in Hindi or Bengali digits too.
- **Station boards**: next 3 hours at each nearby station, including trains that pass without stopping.
- **Three languages**: the whole app switches between **English, हिन्दी and বাংলা** with one tap (EN / हि / বা).
  On first launch it follows your phone's language.
- **Light and dark theme**; auto-refresh every 3 minutes while open, gate states updated every 20 seconds.
- **In-app updates**: when a new version is released, the app shows what's new and updates itself in one tap.
  It installs over the current version, so there's nothing to uninstall and your settings are kept.

## Architecture

```
GPS ─► place name ─► built-in rail database (stations, trains, stops incl. pass-through)
                               │
                               ▼
              Live lookups: rail gates on the map · live departures per station (expected vs scheduled)
                               │
                               ▼
              Gate Engine: place each gate on the track between two stations, interpolate when each
              train reaches it, apply live delay, merge closing windows → OPEN / CLOSING SOON / CLOSED
                               │
                               ▼
              UI (Jetpack Compose): hero card · gate list · find gate · trains · station boards · EN/हि/বা
```

| Module | File |
|---|---|
| Rail database | `app/.../data/PhatakDb.kt` |
| Map and live-train client | `app/.../net/GoogleMaps.kt` |
| Search plan and live train status | `app/.../search/LiveSearch.kt` |
| Gate Engine | `app/.../gate/GateEngine.kt` |
| Screens, theme, translations | `app/.../ui/` |

## Privacy and permissions

- **Location** (precise or approximate, your choice) is used on the phone to find gates and stations near you.
  The app explains this before Android asks, and works without it (search by name).
- Map and live-train lookups go directly from your phone to the public map service, as when you open a map.
- No account, no ads, no analytics, no notifications permission. We run no server and receive none of your data.
- Full details: **[Privacy Policy](PRIVACY.md)**.

## Data

Timetable and station data are bundled in the app for offline use and may be out of date. Live train times and delays
are checked online when you open the app, and every time is labelled **LIVE** or **TIMETABLE** so you always know which
one you are seeing.

## Roadmap

- 🔔 Notify me when a gate on my daily route is about to close
- 🗺️ Gates on a map view, with route-aware warnings
- 🌐 More languages (Odia, Assamese, Marathi, Tamil, …)
- 🤝 Optional crowd confirmations ("gate is down now") to complement the computed status

## Build from source

Requirements: JDK 17 and Android SDK 36.

```bash
git clone https://github.com/JyotirmoyLaha/phatak-live.git
cd phatak-live/app
./gradlew assembleDebug            # app/build/outputs/apk/debug/app-debug.apk
```

Release builds read signing details from `app/keystore.properties` (not in the repo).

## Disclaimer

Phatak Live is an **independent project**. It is not affiliated with or endorsed by Indian Railways, CRIS, IRCTC or
Google.

Gate times are **estimates** computed from train timings and live data. Real gate operation depends on the gateman and
signalling. **Always obey the gate, the signals and the gateman.** Live data comes from public web endpoints that may
change without notice.

## Contact

Questions, suggestions or concerns about the app or its content: please
[open an issue](https://github.com/JyotirmoyLaha/phatak-live/issues). We respond within 7 days.

## License

The source code is released under the **[MIT License](LICENSE)**. The license covers the code only, not the bundled
data files in `app/app/src/main/assets/`.

## Sources

- Level crossing counts and NCRB accident figures: [Dataful: Level crossing accidents fell sharply, but some states remain at risk](https://insights.dataful.in/articles/level-crossing-accidents-fell-sharply-but-some-states-remain-at-risk)
- Train delays from road traffic at gates: [Millennium Post: Nearly 31 trains delayed daily due to road traffic at level crossings](https://www.millenniumpost.in/bengal/nearly-31-trains-delayed-daily-due-to-road-traffic-at-level-crossings-609249)
- Commuter delays at a busy crossing: [Free Press Journal: Diva–Kalwa level crossing delays](https://www.freepressjournal.in/mumbai/mumbai-local-train-services-halted-for-26-minutes-between-diva-and-kalwa-after-level-crossing-gate-stays-open-for-road-traffic)
- Crowd-reported crossing status app: [RailRadar on the App Store](https://apps.apple.com/us/app/id6749937816)
- Fatak app coverage and data source: [Tribune India, 3 July 2026](https://www.tribuneindia.com/news/punjab/family-tragedy-inspires-muktsar-youth-to-build-railway-crossing-alert-application/); listing details: [AppGoblin](https://appgoblin.info/apps/com.fatakv2)
- Gate closing and reopening procedure at non-interlocked gates: [ETV Bharat, 17 Sept 2026](https://www.etvbharat.com/en/bharat/railways-asks-zones-to-modify-rules-to-strengthen-safety-at-non-interlocked-level-crossing-gates-enn26091703952)
- Station master's call to the gateman 15–20 min before a train: [DT Next, July 2025](https://www.dtnext.in/news/tamilnadu/lc-gate-closed-at-706-am-bikers-forced-gatekeeper-partially-open-it-839516)
- Gate closed ~13 min per train at a busy crossing (gateman's estimate): Guwahati Plus, "Guwahati wastes 18 hours a day at rail level crossings"
