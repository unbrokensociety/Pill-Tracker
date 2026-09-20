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
