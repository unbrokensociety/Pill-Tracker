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
- **Version**: 2.1.0 (versionCode 2000+ — installs over any earlier build, no uninstall needed)
- **Target SDK**: Android 16 (API 36), min SDK 26
- **Database**: Local Room Persistence (SQLite)
