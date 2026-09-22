> app-version: 2.5.2

* Completely reworked first-launch onboarding: three swipeable intro
  slides — what the app does, a live preview of the reminder island
  with its Taken/Snooze buttons, and privacy — followed by two short
  coach-mark steps over the real interface (light scrim, so the app
  stays visible while it is being explained)
* The notification permission is now requested in context, on the
  last intro slide after the reason is explained — instead of a cold
  system dialog over a black screen at startup; it is never asked
  twice, and early skippers get one polite fallback request right
  after the tutorial closes
* The tour always ends with an action: "Add now" opens the real
  add-medication form so the first medication is entered right away;
  "Later" leaves the user on the home screen
* Coach steps trimmed from eight to the two that matter — today's
  doses with the one-tap check-off circle, and the "+" button; the
  name input moved out of onboarding (it already lives in Settings)
* All onboarding texts rewritten shorter and clearer in English,
  Ukrainian and Russian; replay from Settings works as before
