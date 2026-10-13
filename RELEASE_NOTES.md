> app-version: 2.6.0

* Privacy policy now matches what the app actually does: removed the
  leftover mentions of HIPAA and Firestore, listed every permission and
  why it is needed, dropped the "allowBackup" jargon
* Texts are plainer: shorter About card, no emoji in the streak card,
  and the onboarding no longer claims the app asks for a single
  permission or never touches the internet (updates use it)
* Behind the scenes: the repository was reorganised, the interval
  schedule is covered by unit tests and the build script was cleaned up
* Everything else is exactly as it was
