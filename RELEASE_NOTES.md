> app-version: 2.5.3

* Settings cleaned up: the "keep in the island" and "sound in silent &
  DND" toggles are gone — both features are now simply always on (the
  DND sound falls back to the normal channel automatically while
  policy access is missing)
* Smarter course handling: when pills run out or the end date passes,
  the medication stops reminding and moves to a new "Finished courses"
  group in the list; a refill brings it right back
* History is never lost anymore: deleting a medication keeps all past
  intakes in the calendar, and finished courses stay browsable too
* Onboarding rebuilt: 6 slides — including an interactive one-tap demo
  you can try on the spot and a stock/course explainer — plus a 4-step
  coach tour (doses, bottom navigation island, settings, add button)
