> app-version: 2.5.0

* Persistent island reminders: a reminder now also stays pinned in the
  status-bar island with "Taken" and "Snooze" buttons and only clears
  once the dose is actually taken — a normal heads-up banner still pops
  up first. Switchable ("Keep in the island")
* Optional critical alerts: reminders can ring through silent mode and
  Do Not Disturb like a real alarm. Uses a DND-bypassing channel on the
  alarm sound stream; the app asks for Do-Not-Disturb access when the
  toggle is switched on. Off by default
* New alarm-clock mode: a full-screen alarm screen turns the display on
  over the lock screen and keeps ringing until you press "Taken" or
  snooze (15/30 min). The critical sound and the alarm mode are separate
  switches, each can be turned off
* The "Taken" action (island, alarm screen or in-app) now logs the
  intake, decrements stock and also cancels any pending snooze
* If the dose is already logged for today, the fired reminder stays
  silent instead of re-appearing
