> app-version: 2.3.2

### Pill Tracker v2.3.2 — hotfix: updater crash, tour overlay, dead swipes

* Fixed the crash when checking for updates — both the auto-check on launch and the «Check for updates» button
* Update dialog now shows short notes: 3 compact lines instead of a wall of text
* No more false «update available» cards when you already run the latest version
* Tour: dark non-transparent overlay, tooltips that never cover the highlighted button, progress dots
* Swiping: a swipe that starts slightly diagonal still turns the page — no more «dead» swipes

---

### 🔧 Crash on «check for updates» — root cause found and killed
* **The updater's version parser crashed the whole app.** The semver regex had no capture groups, yet the code read `groupValues[1..3]` — an `IndexOutOfBoundsException` that the `catch(NumberFormatException)` couldn't catch. It fired inside the check coroutine on **every** update check: the quiet auto-check 1.5 s after launch and the manual «Check for updates» button alike. The parser now splits the matched text, and the entire update center is exception-proof: a failed check can never take the medication tracker down.
* **False «update available» is gone for good.** The old code scraped a `versionCode` number out of the release body — and the historical notes at the bottom of that body still said «versionCode 2100» from the v2.1.0 era, so the math was nonsense (3000 vs 2100 vs 2401). The release now carries a machine-readable `app-version: X.Y.Z` marker at the very top of the notes (this line above), the app compares it with its own version name: same version — silent; rebuild of the same version — silent; genuinely newer — the update card appears.

### 📝 Release notes in the dialog — finally small
* The «What's new» block in the update card is capped at **3 short lines** (~112 characters each): markdown headers are stripped, bold/backtick noise removed, long lines end with «…», and the CI signature after the `---` separator never shows. No more scrolling through a text wall to reach the «Update» button.

### 🧭 Tour overlay — opaque and tidy
* The scrim behind the coach-marks is now **88% dark** — the app no longer bleeds through the overlay (it was 60% before, which read as a muddy semi-transparent mess).
* Tooltips measure their **real height** and reposition with a spring; a small arrow points straight at the highlighted button; the card never covers the highlight and never runs off-screen.
* A pulsing «tap here» ring breathes in the center of every highlight, and progress dots (6 steps) sit inside the tooltip.

### 👆 Swiping — the «dead swipe» fixed
* The axis-lock used to decide **once**, at the very start of a gesture, whether it was horizontal. A swipe that began slightly diagonal was locked out as «vertical» forever — no matter how horizontal it became, the page refused to turn. That was the «фигня при свайпе». The decision is now re-evaluated on every pointer event: the page follows the finger as soon as the gesture is confidently horizontal, while vertical and diagonal scrolls on the lists are still untouchable.

### 🔁 Updates — still guaranteed
* CI version code keeps the proven `2310 + build number` formula (v1.91 = 2401, every next build is higher — installs on top, data kept, same permanent signing key).

---

# 🚀 PillTracker v2.3.1 — Release Notes

### 🔧 Updater fixed: downloads to itself, installs by itself, cleans up after itself
* **No more «downloaded to the file manager».** The APK is now fetched by the app itself into its private cache (`cacheDir/updates/`) — nothing appears in the Downloads folder, no system notification, no file-manager clutter. The update literally downloads "into the app".
* **The installer now really opens by itself.** The v2.3.0 updater listened for the system download-complete broadcast, which Android 14+ silently blocks for private receivers — so the download finished and… nothing happened. The broadcast is gone from the code entirely: the moment the download completes, the package installer opens straight from the app (and if you happened to background the app, it opens the moment you come back). One tap on «Install» — Android itself requires that confirmation, no app can skip it.
* **Self-cleanup after updating.** The first launch of the new version immediately deletes the leftover update file (plus any old ones from v2.3.0) — no junk accumulating.
* **No more false "update available" on the latest version.** Versions are now compared by the version *name* (2.3.1 vs 2.3.0), not just the build number. A CI rebuild of the *same* version (bigger build number, same name) is correctly treated as "you're on the latest version" — the card only appears for a genuinely new version. Auto-check stays quiet; the manual «Check for updates» card in Settings confirms you're current.

### 🧭 Onboarding: readable, not "half-transparent mush"
* The tour scrim is now properly dark (88% instead of 60%) — the app no longer visually bleeds through the overlay; the highlighted button is the single bright thing on screen.
* **Tooltips got geometry brains:** the card now measures its real height, never overlaps the highlighted zone, never runs off-screen, and a small arrow points exactly at the highlighted button. Position glides with a spring when the step changes.
* A pulsing "tap here" ring now breathes in the center of every highlight, plus progress dots for the 6 steps; the welcome screen is equally opaque.
* System «Back» on the welcome page now exits the tour (it used to do nothing).

### 👆 Paging no longer fights vertical scrolling
* The page-flip gesture is axis-locked: a page turns **only** on a confidently horizontal swipe (horizontal movement must dominate vertical by 1.6×) and a slow release simply snaps back. Diagonal and vertical swipes always belong to the lists — scrolling up/down never flips a page by accident. A gentle haptic tick confirms when a page settles.

### 🔁 Updates — still guaranteed
* CI pins `versionCode = 3000 + build number` (this build's base: 2310) — always above every shipped build, same permanent signing key, data kept.

---

# 🚀 PillTracker v2.3.0 — Release Notes

### 🔄 New: update straight from the app (in-app updater)
* **The app now watches GitHub Releases by itself.** On every launch (at most once every 3 hours, so it never pesters you) it quietly checks the latest release. If a newer version is out — a glass card appears: version, what's new, and two buttons: **«Update now»** / **«Later»**.
* **Updates download right inside the app** via the system DownloadManager: a progress bar in the dialog, a system notification with the progress, and when the download finishes **the package installer opens automatically** — one tap on "Update" and you're on the new version. No browser, no manual downloads.
* **First-time permission, explained.** Android asks *once* to allow installing from this source. When that moment comes, the app shows a friendly 3-step instruction (Settings open → allow for Pill Tracker → come back), takes you there, and **the moment you return with the permission granted, the download starts by itself**.
* Not in the mood for permissions? One tap — **«Download in browser»** opens the direct APK link instead.
* «Later» remembers your choice for that particular version; a **«Check for updates»** card in Settings triggers a manual check anytime and tells you when you're already on the latest.

### 🧭 Onboarding v2 — now truly interactive
* The first-launch guide is no longer a stack of slides: after the welcome page it **highlights the real interface with coach-marks** — a dark scrim with a glowing "hole" cut exactly around the actual button or area being explained.
* **Tap the highlight and the app really does the thing**: the step about swiping hops you to the Calendar page when you tap the navigation bar; the «add a medication» step taps the real + button and opens the actual form, where the tour points at the card to fill in.
* Steps: Today screen → navigation & swiping → Calendar → medication list → the + button → the fill-in card. «Skip» is always on top, «Next» always available, and the pager is swipe-locked during the tour so nobody gets lost.
* The replay card in Settings runs the same interactive tour again.

### 🔁 Updates — still guaranteed
* CI pins `versionCode = 3000 + build number` (this build's base: 2300) — always above every previously shipped build, so **every future release installs right on top**, data kept, same permanent signing key.
* Manifest additions for the updater: `INTERNET` + `REQUEST_INSTALL_PACKAGES` (the one-time permission above) + a FileProvider path for the downloaded APK.

---

# 🚀 PillTracker v2.2.0 — Release Notes

### 🧭 New: detailed first-launch guide (onboarding)
* **A beautiful step-by-step tour** now greets you on the very first launch: six animated pages walk through every important function — the Today screen (logging doses, streaks, low-stock warnings), the Calendar and history, the medication list, adding medications (multiple intakes, custom intervals, as-needed meds, notes), and reminders & settings.
* **«Skip» button is always at the top** — one tap and you are straight in the app.
* Progress dots and a step counter, page transitions with soft springs, a gently breathing icon, haptic button presses — all in the app's Liquid Glass style, localized in English, Ukrainian and Russian.
* **Replay anytime**: at the bottom of Settings there is now a «Show the guide again» card — tap it and the full tour replays, so every user can (re)discover all the features.

### ⏪ Base rolled back to 2.1.1 — the last "updates always worked" release
* The app code returns to the **2.1.1 state** (the release auto-updates kept working on), and the only thing added on top is the onboarding feature itself. If you preferred any later experimental tweaks, they are gone — this build is 2.1.1's known-good base + the guide.

### 🔁 Updates — guaranteed to keep working
* The versioning scheme is the proven growing formula: `versionCode = 2200 + build number`. It only ever rises and stays above every previously shipped build, so **every future release installs right on top** of whatever you have now — no uninstall, no "app not installed", your data is kept.
* Same permanent signing key as all releases since v1.80 (the keystore lives in the repo, signature verified locally for this build).

---

# 🚀 PillTracker v2.1.2 — Release Notes

### 🔁 Update fix — versionCode restored to the growing formula
* **This build repairs the v1.84 update issue**: v1.84 accidentally shipped with a flat versionCode (2110) that was *lower* than v1.83's (2193), so Android refused to install it over the older build. The proven scheme from the older builds is restored: `versionCode = 2200 + build number` — it starts above every previously shipped build and only ever grows, so in-place updates always work again. If you installed v1.84 manually (fresh install), simply update on top — your data is kept.

### 🤚 Swiping — now strictly horizontal
* **The #1 gesture complaint is fixed**: pages used to slide sideways whenever a slightly diagonal downward scroll of the list won the touch-slop race against the pager. Paging is now **axis-locked** — the pager only reacts to *confidently* horizontal drags (horizontal movement must outweigh the vertical by 40%+); vertical and diagonal scrolls always belong to the list underneath.
* **Fling-aware page snapping**: on release the page settles by your finger's velocity first (a quick short swipe still flips the page), then by how far the drag carried it — with the same soft spring as before.
* **A gentle haptic tick** when a swipe settles on a new page, so the pager feels physical instead of slippery.

### 🕛 Midnight rollover fixes
* The greeting ("Good morning") and the date subtitle on the Home screen were computed once and went **stale after midnight**; both are now keyed to a live "today" state that refreshes every minute, together with the date strip.

### 📝 Notes field — natural wrapping
* The Notes field on the Add/Edit medication screen is multi-line now: food-preset phrases (e.g. "After meals, Before bed") wrap by words instead of scrolling off sideways in a single line.

---

# 🚀 PillTracker v2.1.1 — Release Notes

### 🔁 Update system — update conflicts eliminated for good
* **One APK channel**: install and update only from **GitHub Releases** (latest release → `pill-tracker.apk`). The delivery site's download button points to the same latest release, so there is exactly one newest APK at any moment.
* **versionCode follows a strictly growing formula** (`2200 + build number`) computed at build time, so Android can never hit a version-code downgrade when updating.
* **Stable signing key**: every build is signed with the same keystore kept in the repository, so each release is accepted as an update of the previous one.
* **Why it conflicted before**: releases up to `v1.79` were signed with throw-away keys (impossible to update in place), and the APK hosted on the delivery site carried a *lower* versionCode than the GitHub builds (a downgrade install). Both traps are now gone.
* **If your installed copy is older than v1.80**: uninstall it once and install the latest release — from then on, every update installs right on top, no uninstall ever needed.

### 🧪 Liquid Glass — the missing piece
* **The blur finally blurs only what it should.** In 2.1.0 the blur render pass was attached to the navigation bar itself, so the bar's own icons, labels and the active pill were blurred away together with the backdrop. The frosted copy of the content now lives in its **own isolated drawing layer**: the blur is applied there and only there, while the bar UI above stays pixel-crisp. This is the classic frosted-glass composition: blurred content → whisper scrim → specular rim → crisp controls.
* **Truly live frost.** Glass panels now observe every re-record of the backdrop — when the list scrolls or content animates behind the bar, the blur refreshes in the same rhythm. No frozen snapshots, no stale glass.
* Slightly deepened scrim/tint so white text and icons keep comfortable contrast over busy colorful content.

### 📐 Text layout — no more ugly line breaks
* All preset chips ("Before meals / With food / After meals / Before bed" and their translations) now flow — long labels wrap to the next line **as whole chips** instead of being crushed into a per-character ladder.
* Schedule chips (Daily / Every N days / As needed) get natural width and flow, so no translation can ever letter-wrap inside a fixed slot.
* Home-screen cards, medication list badges and the low-stock banner use the same flow layout; every chip is single-line with graceful ellipsis.
* Compact schedule badge ("3 × daily / 3 × в день / 3 × на день") reads cleaner and fits any card.

### 🌍 Localization — deeper polish
* 6 new accessibility strings in all three languages: screen-reader descriptions for the Back, Selected, interval +/- and calendar month buttons (previously hardcoded English).
* Intake badge, stock tags and food presets re-checked for natural phrasing and safe lengths in EN / RU / UK.

### 📦 Build Information
- **Package Name**: `com.aistudio.meditracker.zqxpr`
- **Version**: 2.1.2 · versionCode = 2200 + build number (always above every previously shipped build — in-place updates are always accepted)
- **Target SDK**: Android 16 (API 36), min SDK 26
- **Database**: Local Room Persistence (SQLite)

---

# 🚀 PillTracker v2.1.0 — Release Notes

### 🧪 Liquid Glass — now REAL glass
* **Live backdrop blur on every device**: the floating navigation island now blurs the actual content scrolling beneath it. Android 12+ uses the hardware gaussian blur path; Android 8–11 render a 4×-downscaled snapshot and blur it on the CPU (3-pass box filter) — a genuine frosted-glass look instead of a flat translucent fill.
* **Whisper-light scrim + specular rim**: the overlay stack was slimmed from four heavy layers to a single cached pass, so the blur itself finally reads as glass.
* **Cleaner active-tab indicator**: the old chunky bordered "glass strip" pill is replaced by a soft, borderless tinted pill with a light-pooling gradient — the bar's blur does the visual work now.
* **Content under the glass**: lists keep scrolling edge-to-edge beneath the bar, so the frost always has something real to blur.

### 💊 Medication icons — fully redrawn
* All 7 form icons (capsule, tablet, syrup, drops, injection, spray, patch) were redrawn with a consistent visual language: ~90% canvas fill, one bold outline weight, volumetric gradients and a single confident gloss stroke per glyph.
* Micro-details that turned to mush at 20–24dp (tiny graduation ticks, perforation grids, dust-size mist dots) were replaced by bold, readable equivalents.
* Icon badges now have a radial highlight and a tinted rim.

### 🌍 Localization — polished in all 3 languages
* English / Русский / Українська strings were reviewed and rephrased for naturalness (grammar, gender-neutral buttons, consistent formality).
* New: time-of-day greeting on the Home screen ("Good morning / Доброе утро / Доброго ранку") with a fully localized date subtitle.
* Version labels updated to 2.1.0.

### ✨ Animations — round two
* Staggered list entrances now fade + slide + scale with a softer settle curve.
* The FAB pops in with a spring overshoot on screen entry.
* Settings gained an animated compliance **ring** (gradient sweep + percentage in the center) replacing the linear bar.
* Day cells, date strip and check buttons keep their spring physics, tuned slightly bouncier.

### ⚡ Performance — without losing a pixel of beauty
* The aurora backdrop and glass overlays build their brushes once per size (`drawWithCache`) instead of allocating every frame while scrolling.
* The navigation bar's glass went from 5 stacked render nodes to 2; the scrim/tint pass is a single cached draw.
* The software blur path only regenerates while frames are actually produced (idle screens cost nothing) and works on a 4×-downscaled buffer.
* Removed per-frame `remember` allocations in the pager; quantized state reads cut recomposition during swipes.

### 🧹 Fixes
* List bottom clearance reduced so the last card rests right beneath the glass dock instead of floating 88dp above it.
* Active nav item color is now theme `primary` (was a low-contrast container tint).

### 📦 Build Information
- **Package Name**: `com.aistudio.meditracker.zqxpr`
- **Version**: 2.1.0 (versionCode 2100 — installs over any earlier build, no uninstall needed)
- **Target SDK**: Android 16 (API 36), min SDK 26
- **Database**: Local Room Persistence (SQLite)
