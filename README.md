# Pill Tracker 💊📱

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Language" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="UI" />
  <img src="https://img.shields.io/badge/Database-Room-0052CC?style=for-the-badge&logo=sqlite&logoColor=white" alt="Database" />
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" alt="License" />
</p>

<p align="center">
  <b>Pill Tracker</b> is a modern, high-performance, and reliable Android application designed to manage daily medication schedules, send exact intake reminders, and track adherence history. Built with modern Jetpack Compose, a true <b>Liquid Glass</b> design system, and modern Android architecture.
</p>

---

## 🌐 Quick Navigation / Навігація / Навигация

<p align="center">
  <a href="#english"><b>🇬🇧 English</b></a> &nbsp;|&nbsp;
  <a href="#ukrainian"><b>🇺🇦 Українська</b></a> &nbsp;|&nbsp;
  <a href="#russian"><b>[RU] Русский</b></a> &nbsp;|&nbsp;
  <a href="#install-updates"><b>📲 Install & Updates</b></a> &nbsp;|&nbsp;
  <a href="#privacy-terms"><b>📜 Privacy & Terms</b></a>
</p>

---

<a id="english"></a>
## 🇬🇧 English

### 🌟 Key Highlights
* ⏰ **Exact Alarm Engine (`AlarmManager.setAlarmClock`)**: Guarantees alarm notifications trigger precisely on time without OS battery-saver or Doze mode delays.
* 🧪 **True Liquid Glass Design**: The floating navigation island blurs the live content behind it — hardware gaussian blur (`RenderEffect`) on Android 12+, and a CPU-rendered frosted snapshot on Android 8–11, so real glass works on every device. The blur lives in its own isolated layer, so the icons and labels in the bar always stay pixel-crisp above the frost.
* 🎯 **Fluid Tab Navigation**: A soft tinted pill glides across the bottom bar with jelly stretch physics, drag-scrubbing and edge resistance.
* 🔄 **In-App Updates**: The app checks GitHub Releases by itself (on launch, at most once every 3 hours) and offers the new version in a glass card — the APK downloads right inside the app with a progress bar, and the package installer opens automatically when it's done. The first time, Android asks for a one-time install permission, and the app walks you through it with a friendly 3-step instruction — when you come back with it granted, the download continues on its own. A browser download fallback and a «Check for updates» card in Settings are included.
* 🧭 **Interactive First-Launch Tour**: After a short welcome page the guide lights up the real interface with coach-marks — a glowing "hole" is cut exactly around the actual button being explained, and tapping it really performs the action (hops to the Calendar, opens the actual add-medication form). «Skip» always on top, replay from the «Show the guide again» card in Settings. Localized EN/UK/RU.
* 📅 **Interactive Calendar & Tracker**: Track daily doses, mark intakes as taken/skipped, and review historical compliance.
* 💊 **Comprehensive Medication Management**: Customize dose amounts, medication form (capsule, tablet, syrup, drops, injection, spray, patch), color coding, start dates, and multiple daily reminders.
* 🪄 **Redrawn Medication Icons**: All seven form icons are vector-painted with volumetric gradients and glossy highlights — crisp at any size.
* 📊 **Adherence Analytics**: Animated compliance ring, completion percentages, and streak tracking.
* 🌍 **Full Localization**: Polished English, Ukrainian, and Russian translations with dynamic in-app language switching. Text layout is translation-safe: chips flow to new lines as whole pieces, so long labels never break mid-word.
* ⚡ **Performance-First Rendering**: Cached gradients, consolidated draw passes, staggered entrance animations and spring physics everywhere — smooth at 120 Hz.

### 🛠 Tech Stack & Architecture
| Layer | Technologies |
| :--- | :--- |
| **Language** | 100% Kotlin |
| **UI Framework** | Jetpack Compose, Material 3, Navigation Compose, Compose Animation |
| **Architecture** | Clean Architecture + MVVM (Model-View-ViewModel) |
| **Database** | Room Persistence Library with KSP |
| **Asynchronous** | Kotlin Coroutines & `StateFlow` / `SharedFlow` |
| **Notifications** | `AlarmManager`, `BroadcastReceiver`, Android Notification Channels |

---

<a id="ukrainian"></a>
## 🇺🇦 Українська

### 🌟 Основні можливості
* ⏰ **Точні нагадування (`setAlarmClock`)**: Апаратний системний будильник спрацьовує хвилина в хвилину навіть у режимі глибокого сну пристрою (Doze mode).
* 🧪 **Справжнє «Рідке Скло»**: Плаваюча навігаційна панель розмиває контент під собою в реальному часі — апаратне гаусове розмиття на Android 12+ і програмний морозний ефект на Android 8–11. Скло працює на будь-якому пристрої, а іконки та підписи панелі залишаються ідеально чіткими над морозом.
* 🎯 **Плавна анімована навігація**: М'який індикатор-пігулка ковзає між вкладками з фізикою пружини, підтримкою перетягування пальцем та опором на краях.
* 🔄 **Оновлення прямо із застосунку**: Застосунок сам перевіряє GitHub Releases (при запуску, не частіше ніж раз на 3 години) і пропонує нову версію у скляній картці — APK завантажується прямо в застосунку з прогрес-баром, а пакетний інсталятор відкривається автоматично. Перший раз Android попросить одноразовий дозвіл на встановлення — застосунок проведе через це дружньою інструкцією з 3 кроків, і щойно ви повернетеся з дозволом, завантаження продовжиться саме. Є запасний варіант завантаження через браузер і картка «Перевірити оновлення» в Налаштуваннях.
* 🧭 **Інтерактивний тур при першому вході**: Після короткої сторінки привітання гід підсвічує справжній інтерфейс — «дірка» у затемненні вирізається точно навколо реальної кнопки, а тап по ній справді виконує дію (перестрибує в Календар, відкриває справжню форму додавання ліків). «Пропустити» завжди зверху, повтор — карткою «Показати навчання знову» в Налаштуваннях. Переклади EN/UK/RU.
* 📅 **Інтерактивний Календар**: Зручний перегляд розкладу на будь-який день із можливістю відмітити прийом ліків в один дотик.
* 💊 **Гнучкий каталог ліків**: Налаштування дозування, форми випуску (капсула, таблетка, сироп, краплі, ін'єкція, спрей, пластир), дати початку курсів та індивідуального колірного оформлення.
* 🪄 **Перемальовані іконки ліків**: Усі сім форм намальовані векторно з об'ємними градієнтами та відблисками — чіткі на будь-якому розмірі.
* 📊 **Аналітика та Статистика**: Анімоване кільце дотримання розкладу, відсотки виконання та серії регулярності.
* 🌍 **Багатомовний інтерфейс**: Вдосконалені переклади (Українська, Англійська, Російська) з миттєвим переключенням мови. Верстка безпечна для перекладів: чіпси переносяться на новий рядок цілими, без розривів по буквах.
* ⚡ **Продуктивність**: Кешовані градієнти, об'єднані проходи малювання та пружинна фізика анімацій — плавно навіть на 120 Гц.

---

<a id="russian"></a>
## [RU] Русский

### 🌟 Основные возможности
* ⏰ **Точные напоминания (`setAlarmClock`)**: Системный будильник срабатывает точно в указанную минуту без задержек режима энергосбережения.
* 🧪 **Настоящее «Жидкое Стекло»**: Плавающая навигационная панель размывает контент под собой в реальном времени — аппаратное гауссово размытие на Android 12+ и программный морозный эффект на Android 8–11. Стекло работает на любом устройстве, а иконки и подписи панели остаются идеально чёткими поверх мороза.
* 🎯 **Плавная скользящая навигация**: Мягкий индикатор-пилюля скользит между вкладками с физикой пружины, перетаскиванием пальцем и сопротивлением на краях.
* 🔄 **Обновления прямо из приложения**: Приложение само проверяет GitHub Releases (при запуске, не чаще раза в 3 часа) и предлагает новую версию в стеклянной карточке — APK скачивается прямо в приложении с прогресс-баром, а пакетный установщик открывается автоматически по завершении. В первый раз Android попросит разовое разрешение на установку — приложение проведёт через это дружелюбной инструкцией из 3 шагов, и как только вы вернётесь с разрешением, скачивание продолжится само. Есть запасной вариант скачивания через браузер и карточка «Проверить обновления» в Настройках.
* 🧭 **Интерактивный тур при первом входе**: После короткой страницы приветствия гид подсвечивает настоящий интерфейс — «дырка» в затемнении вырезается точно вокруг реальной кнопки, а тап по ней действительно выполняет действие (перепрыгивает в Календарь, открывает настоящую форму добавления лекарства). «Пропустить» всегда сверху, повтор — карточкой «Показать обучение снова» в Настройках. Переводы EN/UK/RU.
* 📅 **Интерактивный Календарь**: Удобный график приема на выбранный день с подтверждением в один клик.
* 💊 **Персональный каталог**: Настройка дозировки, формы выпуска (капсула, таблетка, сироп, капли, инъекция, спрей, пластырь), нескольких времён приёма и цветовых меток.
* 🪄 **Перерисованные иконки лекарств**: Все семь форм нарисованы векторно с объёмными градиентами и бликами — чёткие на любом размере.
* 📊 **Аналитика приёмов**: Анимированное кольцо дисциплины, проценты выполнения дневного плана и серии регулярности.
* 🌍 **Мультиязычность**: Улучшенные переводы (Русский, Английский, Украинский) с динамическим переключением языка без перезапуска. Вёрстка безопасна для переводов: чипсы переносятся на новую строку целиком, без разрывов по буквам.
* ⚡ **Производительность**: Кешированные градиенты, объединённые проходы отрисовки и пружинная физика анимаций — плавно даже на 120 Гц.

---

<a id="privacy-terms"></a>
## 📜 Privacy Policy & Terms of Service / Політика конфіденційності / Политика конфиденциальності

### 🇬🇧 English - Privacy Policy & Terms of Service
1. **Data Protection & Encryption**: All medication records, intake schedules, and health metrics are strictly encrypted locally in an Android Room database and securely synchronized with Firebase Firestore using TLS 1.3 encryption.
2. **Medical Disclaimer**: Pill Tracker is a personal organizational assistant and does NOT substitute professional medical advice, diagnosis, or treatment. Always consult a qualified physician or pharmacist for medical decisions.
3. **Data Ownership & Right to be Forgotten**: Users maintain full ownership of their health records and can purge all account data and cloud backups at any time directly through the app settings or by signing out.

---

### 🇺🇦 Українська - Політика конфіденційності та Умови використання
1. **Захист та шифрування даних**: Усі записи про прийом ліків та розклад зберігаються в зашифрованій локальній базі даних Android Room та синхронізуються з Firebase Firestore через захищений протокол TLS 1.3.
2. **Медичне застереження**: Застосунок Pill Tracker є персональним органайзером і НЕ замінює професійну медичну консультацію, діагностику або лікування. Завжди звертайтеся до кваліфікованого лікаря або фармацевта.
3. **Право на видалення даних**: Користувач володіє всіма своїми даними та може в будь-який момент видалити акаунт і хмарні резервні копії через налаштування додатку.

---

### ru Русский - Политика конфиденциальности и Условия использования
1. **Защита и шифрование данных**: Все записи о приёме лекарств и расписании хранятся в защищенной локальной базе данных Android Room и синхронизируются с Firebase Firestore по протоколу TLS 1.3.
2. **Медицинская оговорка**: Приложение Pill Tracker является персональным помощником и НЕ заменяет профессиональную медицинскую консультацию, диагностику или назначение врача. По всем медицинским вопросам обращайтесь к квалифицированному специалисту.
3. **Право на удаление данных**: Пользователь сохраняет полный контроль над своими данными и может в любой момент удалить профиль и все облачные резервные копии в настройках приложения.

---

<a id="install-updates"></a>
## 📲 Installation & Updates / Встановлення та оновлення / Установка и обновление

### 🇬🇧 English
- **One channel**: always install and update from **GitHub Releases → latest release → `pill-tracker.apk`**. Every release is newer than the previous one (CI pins versionCode `3000 + build number`), so Android always accepts it as an in-place update. Since v2.3.0 the app can also update itself in place — see «In-App Updates» above.
- **One permanent signature**: since release `v1.80` every APK is signed with the same certificate (the keystore lives in the repo) — every release installs on top of the previous one.
- **Very old builds only**: if your installed copy is older than `v1.80`, Android will refuse to update it (those early builds were signed with throw-away keys). Uninstall the old app **once**, install the latest release — after that, every future update installs right on top, no uninstall ever needed.

### 🇺🇦 Українська
- **Один канал**: встановлюйте та оновлюйте додаток лише з **GitHub Releases → останній реліз → `pill-tracker.apk`**. Кожен новий реліз вищий за попередній (CI фіксує versionCode `3000 + номер збірки`), тож Android завжди приймає його як оновлення поверх встановленого. Починаючи з v2.3.0 застосунок уміє оновлюватися ще й сам — див. «Оновлення прямо із застосунку» вище.
- **Одна постійна підпись**: починаючи з релізу `v1.80`, кожен APK підписаний тим самим сертифікатом (кейстор зберігається в репозиторії) — кожен реліз встановлюється поверх попереднього.
- **Тільки для дуже старих збірок**: якщо встановлена версія старіша за `v1.80`, Android не дозволить оновити її (ті ранні збірки були підписані разовими ключами). Видаліть старий додаток **один раз** і встановіть останній реліз — після цього всі майбутні оновлення встановлюються поверх без видалення.

### ru Русский
- **Один канал**: устанавливайте и обновляйте приложение только из **GitHub Releases → последний релиз → `pill-tracker.apk`**. Каждый новый релиз выше предыдущего (CI фиксирует versionCode `3000 + номер сборки`), поэтому Android всегда принимает его как обновление поверх установленного. Начиная с v2.3.0 приложение умеет обновляться ещё и само — см. «Обновления прямо из приложения» выше.
- **Одна постоянная подпись**: начиная с релиза `v1.80`, каждый APK подписан одним и тем же сертификатом (кейстор хранится в репозитории) — каждый релиз ставится поверх предыдущего.
- **Только для очень старых сборок**: если установленная версия старше `v1.80`, Android не даст её обновить (те ранние сборки были подписаны одноразовыми ключами). Удалите старое приложение **один раз** и поставьте последний релиз — после этого все будущие обновления ставятся поверх, без удаления.

---

<a id="build-instructions"></a>
## ⚙️ Building & CI/CD

This repository includes a full **GitHub Actions CI/CD pipeline** (`.github/workflows/android.yml`) that automatically builds and signs the APK on every commit. The `versionCode` is computed at build time from a strictly growing formula (`2200 + run number`) defined in `app_build.gradle.kts`, and every build is signed with the permanent repo keystore — so releases never break in-place updates.

### Local Build
```bash
# Clone the repository
git clone https://github.com/unbrokensociety/Pill-Tracker.git
cd Pill-Tracker

# Build debug APK
./gradlew assembleDebug
```
The compiled `.apk` will be generated at:
`app/build/outputs/apk/debug/pill-tracker.apk`

### Project Layout
Kotlin sources are kept flat in the repository root next to `build.gradle.kts` — the root build script syncs them into the standard Android source tree (`app/src/main/java/...`) automatically before every build, so GitHub Actions always compiles the newest code.
