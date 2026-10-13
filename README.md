# Pill Tracker 💊

Android app for medication schedules: exact-time reminders, intake history and stock tracking.
Kotlin · Jetpack Compose · Room · no accounts, no analytics - data stays on the device.

[English](#english) · [Українська](#українська) · [Русский](#русский)

## English

**Features**
- Exact reminders via `AlarmManager.setAlarmClock`; alarms are restored after reboot, app update and time-zone/clock changes.
- Reminder notification with **Taken** / **Snooze** buttons; optional pinned "island" notification, critical (DND-bypassing) sound and full-screen alarm mode - each is a separate switch.
- Schedules: daily, every N days, as needed; several reminder times per day; 7 medication forms with colour coding.
- Calendar with taken/skipped marks, adherence ring and streaks.
- Stock tracking: decrements on every taken dose, low-stock warning, refill.
- English / Ukrainian / Russian UI, switchable inside the app.
- Adaptive "glass" visuals with an automatic performance tier (Full / Adaptive / Matte) and an intensity slider in Settings.
- First-launch tour and in-app updates from GitHub Releases.

**Install and update**
- Download `pill-tracker.apk` from the latest **GitHub Release**. Every release has a higher `versionCode` (`2420 + build number`) and the same signature, so it installs over the previous one; the app can also update itself.
- Builds older than `v1.80` were signed with throw-away keys: uninstall them once, then install the latest release.

**Privacy.** Everything is stored locally (Room + DataStore). The only network request is the update check against the GitHub Releases API. Full text of the privacy policy and terms is available in the app (Settings).

## Українська

Застосунок для Android: розклад прийому ліків, точні нагадування, історія прийому та облік запасів. Дані зберігаються лише на пристрої.

- Точні нагадування (`setAlarmClock`), відновлення після перезавантаження, оновлення та зміни часу.
- Кнопки «Прийняв» / «Відкласти» в сповіщенні; опційно закріплене сповіщення, гучний сигнал у режимі DND і повноекранний будильник.
- Розклад: щодня, раз на N днів, за потреби; календар, відсоток дотримання, серії.
- Облік запасів із попередженням про закінчення; інтерфейс EN / UK / RU.
- Встановлення й оновлення: `pill-tracker.apk` з останнього **GitHub Release** (збірки старіші за `v1.80` слід один раз видалити й поставити заново).

## Русский

Приложение для Android: расписание приёма лекарств, точные напоминания, история приёма и учёт запасов. Данные хранятся только на устройстве.

- Точные напоминания (`setAlarmClock`), восстановление после перезагрузки, обновления и смены времени.
- Кнопки «Принял» / «Отложить» в уведомлении; опционально закреплённое уведомление, громкий сигнал в режиме DND и полноэкранный будильник.
- Расписание: ежедневно, раз в N дней, по необходимости; календарь, процент соблюдения, серии.
- Учёт запасов с предупреждением о том, что лекарство заканчивается; интерфейс EN / UK / RU.
- Установка и обновление: `pill-tracker.apk` из последнего **GitHub Release** (сборки старше `v1.80` нужно один раз удалить и поставить заново).

## Build

```bash
git clone https://github.com/unbrokensociety/Pill-Tracker.git && cd Pill-Tracker
./gradlew assembleDebug                          # debug APK
./gradlew assembleRelease                        # release APK (see signing below)
./gradlew testDebugUnitTest                      # unit tests
```

Release signing: with `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` set, the real key is used. Otherwise `allowDebugSigning=true` in `gradle.properties` signs with the repo's `debug.keystore` - one permanent key that keeps in-app updates working. That key is public, so such builds must not go to a store.

CI: `.github/workflows/android.yml` builds on every push and publishes a GitHub Release whose body is `RELEASE_NOTES.md`. `versionName` in `app/build.gradle.kts` must equal the `app-version` marker in that file.

Layout: `app/src/main/java/com/aistudio/meditracker/{alarms,data,ui}`, resources in `app/src/main/res`. Project rules for changes (fixed `applicationId`, Room backward compatibility) are in `AGENTS.md`.
