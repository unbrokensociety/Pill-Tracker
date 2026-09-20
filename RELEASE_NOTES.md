# 🚀 PillTracker v2.1.1 — Release Notes

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
- **Version**: 2.1.1 (versionCode 2110 — installs over any earlier build, no uninstall needed)
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
