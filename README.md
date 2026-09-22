# Pill Tracker 💊📱

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Language" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="UI" />
  <img src="https://img.shields.io/badge/Database-Room-0052CC?style=for-the-badge&logo=sqlite&logoColor=white" alt="Database" />
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" alt="License" />
</p>

<p align="center">
  <b>Pill Tracker</b> is a modern, high-performance, and reliable Android application designed to manage daily medication schedules, send exact intake reminders, and track adherence history. Built with modern Jetpack Compose, a calm <b>frosted-glass</b> design system, and modern Android architecture.
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
* 🧪 **Glass Design (v2.4.3–v2.4.12: calm, adaptive, tunable)**: The floating navigation island blurs the live content behind it — hardware gaussian blur (`RenderEffect`) on Android 12+, and a CPU-rendered frosted snapshot on Android 8–11 (re-snapshotted only when the content actually changed). The tint is whisper-thin so the blur itself does the visual work, and a second wider-blurred, slightly zoomed copy adds a real refractive edge bleed. **v2.4.7 «iOS-style liquid glass»: on Android 13+ a per-pixel AGSL `RuntimeShader` bends the backdrop around the panel rim — real refraction streaks, chromatic aberration and a specular glint that slides along the bevel as the panel moves; older Androids keep the zoom-bleed look.** The app **measures the phone's power** (CPU cores, RAM, low-RAM flag, Android version) and picks a tier: *Full* liquid glass, *Adaptive* (lighter blur) or *Matte* (no recording, no blur — a fully opaque panel: nothing shows through, so it never changes as content scrolls). A live frame-time monitor steps down on sustained jank and battery saver gates to Matte instantly — weak phones never lag, strong ones shine. In Settings the mode can be re-picked manually any time (tap «Choose» — v2.4.6 rebuilt the picker: full-width rows with icons and descriptions that wrap, so nothing is ever clipped in any language), **plus an Intensity slider (v2.4.6): from calm matte all the way to «ultra liquid»** — every panel and card re-renders live under your thumb and the choice persists. The blur lives in its own isolated layer, so the icons and labels in the bar always stay pixel-crisp above the frost. **v2.4.10 «true glass»: the v2.4.9 milky veil is gone — the center is again the clear frosted blur with only a whisper-thin tint (~3–11% by intensity), and all the lens magic lives in the rim band: refraction streaks, chromatic dispersion, a hairline bevel and the gliding glint; the specular is calmer and the diagonal sheen removed.** **v2.4.11: the navigation bar is back to the classic solid look — opaque surface, hairline edge, soft shadow, no backdrop blur or recording (the bar-side liquid glass is retired). The mode/intensity pickers now only tune the calm frosted-card look.** **v2.4.12: transparency is back — the classic bar is now a calm translucent panel (~72–82% by theme): scrolling content softly shows through while the icons stay crisp; still no blur, no lens, no backdrop recording.**
* 📦 **Tiny on the phone (v2.4.6)**: R8 code shrinking + resource shrinking strip everything the app doesn't use (the unused icon-pack classes were ~4× the app itself). The dex shrank ~4×, so the download is roughly half the size and the *installed* footprint (Android used to extract a full 44 MB copy of the unminified code to /data) drops by tens of megabytes.
* 🎯 **Fluid Tab Navigation**: A soft tinted pill glides across the bottom bar with jelly stretch physics, drag-scrubbing and edge resistance. Since v2.4.2 tab labels are *measured* and deterministically fitted to their slots: «Налаштування» in Ukrainian — or any label at any system font scale — always stays inside its pill, and switching pages is completely silent (no vibration; the motion itself is the feedback).
* 🔄 **In-App Updates**: The app checks GitHub Releases by itself (on launch, at most once every 3 hours) and offers the new version in a glass card with a **compact 3-line changelog**. Version comparison is by the `app-version` marker in the release notes — crash-proof and immune to false "update available" cards (a rebuild of the same version stays silent). The APK downloads **invisibly inside the app** (its private cache — nothing in your file manager, no notification), and the package installer opens automatically the moment the download finishes; the update file is deleted right after updating. The first time, Android asks for a one-time install permission with a friendly 3-step instruction — when you come back with it granted, the download continues on its own. A browser download fallback and a «Check for updates» card in Settings are included.
* 🧭 **Interactive First-Launch Tour**: After a short welcome page the guide lights up the real interface with coach-marks — an opaque dark scrim with a glowing "hole" cut exactly around the actual button being explained, a pulsing "tap here" ring in the highlight, and tooltips that measure their real height, carry an arrow pointing at the button, and never cover the highlighted zone. Progress dots for the **8 steps** (v2.4.1 adds a dose-logging step: the tour explains the one-tap check circle, walks all four screens, and ends with a proper "You are all set!" card instead of an abrupt cut). Tapping the highlight really performs the action (hops to the Calendar, opens the actual add-medication form). v2.4.9: the spotlight glides between steps (a spring-animated hole) and the tooltip waits for the highlighted element to actually appear — no more jumping mid-tour. «Skip» always on top, replay from the «Show the guide again» card in Settings. Localized EN/UK/RU.
* 🤚 **Axis-Locked Swiping (v2.3.2)**: pages respond to any *confidently horizontal* swipe — the decision is re-evaluated continuously, so a swipe that starts slightly diagonal still turns the page (no more "dead" swipes), while vertical and diagonal scrolling always belongs to the lists underneath. Settling a page is felt only visually: the spring, the pill stretch — no haptic tick (v2.4.2 removed it).
* 🌅 **Personal Greeting (v2.4.0)**: morning now lasts until 10:00, day until 17:00, evening until 22:00, night after — and the greeting can carry your name: enter it once (optional) on the welcome screen or in Settings - "Personal greeting" (v2.4.1: the pencil lives only in Settings now, the home screen stays clean). The name is stored locally only and removable with one tap. The clock is live: greeting and date update by themselves after midnight. The update card also gained a proper close: X, OK, system Back and tapping the dimmed backdrop all dismiss it.
* 📅 **Interactive Calendar & Tracker**: Track daily doses, mark intakes as taken/skipped, and review historical compliance.
* 💊 **Comprehensive Medication Management**: Customize dose amounts, medication form (capsule, tablet, syrup, drops, injection, spray, patch), color coding, start dates, and multiple daily reminders.
* 🪄 **Redrawn Medication Icons**: All seven form icons are vector-painted with volumetric gradients and glossy highlights — crisp at any size.
* 📊 **Adherence Analytics**: Animated compliance ring, completion percentages, and streak tracking.
* 🌍 **Full Localization**: Polished English, Ukrainian, and Russian translations with dynamic in-app language switching. v2.4.9: 128 unused template strings removed per language and the Russian tone unified (no ты/вы mixing). Text layout is translation-safe: chips flow to new lines as whole pieces, so long labels never break mid-word.
* ⚡ **Performance-First Rendering**: Cached gradients, consolidated draw passes, staggered entrance animations and spring physics everywhere — smooth at 120 Hz. v2.4.9: the window prefers a 60 Hz display mode (GPU headroom on 90/120 Hz screens) and glass panels draw fewer layers per frame.

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
* 🧪 **Скляний дизайн (v2.4.3–v2.4.12: спокійний, адаптивний, налаштовуваний)**: Плаваюча навігаційна панель розмиває контент під собою в реальному часі — апаратне гаусове розмиття на Android 12+ і програмний морозний ефект на Android 8–11 (оновлюється лише коли контент справді змінився). Тон ледь помітний — роботу робить саме розмиття, а другий, ширший і трохи збільшений шар додає справжнє заломлення по краях. **v2.4.7 «як в iOS»: на Android 13+ попиксельний AGSL-шейдер заломлює фон по обідцю панелі — справжні рефракційні смуги, хроматична дисперсія та спекулярний відблиск, що сковзає по фасці, коли панель рухається; старіші Android зберігають вигляд v5.** Застосунок **сам міряє потужність телефона** (ядра, RAM, low-RAM, Android) і обирає рівень: *повний* ефект, *адаптивний* (легше розмиття) або *матовий* (без запису й розмиття — повністю непрозора панель: нічого не просвічує, вигляд ніколи не змінюється). Монітор кадрів знижує рівень при стабільних підгальмовуваннях, режим економії заряду миттєво вмикає матовий — слабкі телефони не лагають, потужні сяють. У Налаштуваннях режим можна перебрати вручну будь-коли (кнопка «Обрати» — у v2.4.6 вибір перероблено: рядки на всю ширину з іконками й описами, які переносяться, тож нічого не обрізається жодною мовою), **а новий повзунок «Наскільки рідке» (v2.4.6) — від спокійного матового до «дуже рідкого» скла**: панель і картки перемальовуються наживо під пальцем, вибір зберігається. Іконки та підписи панелі залишаються ідеально чіткими над морозом. **v2.4.10 «справжнє скло»: молочна вуаль v2.4.9 прибрана — у центрі знову чистий морозний блюр із ледь помітним тоном (~3–11% залежно від інтенсивності), а вся магія лінзи — в обідку: рефракція, хроматична дисперсія, хайрлайн-фаска та ковзний відблиск; спекуляр спокійніший, діагональний sheen прибрано.** **v2.4.11: навігаційна панель повернулася до класичного суцільного вигляду — непрозора поверхня, тонка риска по краю, м'яка тінь; без живого розмиття та запису фону (експеримент зі «рідким склом» на панелі завершено). Режим і інтенсивність тепер керують лише виглядом карток.** **v2.4.12: прозорість повернулася — класична панель знову напівпрозора (~72–82% залежно від теми): контент м’яко просвічує крізь неї, а іконки лишаються чіткими; як і раніше — без розмиття, лінзи та запису фону.**
* 📦 **Мінімум місця на телефоні (v2.4.6)**: R8-стиснення коду та ресурсів прибирає все невикористане (зайві класи бібліотеки іконок важили вчетеро більше за сам застосунок). Dex став меншим ~у 4 рази — завантаження приблизно вдвічі легше, а місце на телефоні (Android раніше розпаковував копію 44 МБ немінімізованого коду в /data) зменшується на десятки мегабайтів.
* 🎯 **Плавна анімована навігація**: М'який індикатор-пігулка ковзає між вкладками з фізикою пружини, підтримкою перетягування пальцем та опором на краях. З v2.4.2 підписи вкладок замірюються та детерміновано вміщуються у свої слоти («Налаштування» українською — за будь-якого масштабу шрифту — більше ніколи не виходить за пігулку), а перемикання сторінок повністю безшумне: без вібрації, рух сам є відгуком.
* 🔄 **Оновлення прямо із застосунку**: Застосунок сам перевіряє GitHub Releases (при запуску, не частіше ніж раз на 3 години) і пропонує нову версію у скляній картці. APK завантажується **непомітно всередині застосунку** (у приватний кеш — у провіднику нічого не з’явиться, без сповіщень), а пакетний інсталятор відкривається сам одразу після завантаження; файл оновлення видаляється одразу після оновлення. Порівняння версій — за ім’ям версії, тож пересбірка тієї ж версії не показує хибне «доступне оновлення». Перший раз Android попросить одноразовий дозвіл — з дружньою інструкцією з 3 кроків, і щойно ви повернетеся з дозволом, завантаження продовжиться саме. Є запасний варіант через браузер і картка «Перевірити оновлення» в Налаштуваннях.
* 🧭 **Інтерактивний тур при першому вході**: Після короткої сторінки привітання гід підсвічує справжній інтерфейс — «дірка» у затемненні вирізається точно навколо реальної кнопки, а тап по ній справді виконує дію (перестрибує в Календар, відкриває справжню форму додавання ліків). У v2.4.1 — **8 кроків**: тур пояснює позначення прийому одним дотиком, проходить усі чотири сторінки, включно з Налаштуваннями, а завершується екраном «Все готово!» замість різкого обриву; на екрані вітання можна (необов'язково) ввести ім'я. v2.4.9: підсвітка плавно ковзає між кроками (пружинна «дірка»), а картка чекає, поки цільовий елемент справді з’явиться — стрибків під час туру більше немає. «Пропустити» завжди зверху, повтор — карткою «Показати навчання знову» в Налаштуваннях. Переклади EN/UK/RU.
* 🌅 **Персональне привітання (v2.4.0)**: ранок тепер до 10:00, день до 17:00, вечір до 22:00, потім ніч — і привітання може носити ваше ім'я: введіть його раз на екрані вітання або в Налаштуваннях — картка «Персональне привітання» (у v2.4.1 олівець живе лише там, головний екран чистіший). Ім'я зберігається лише локально й прибирається одним дотиком. Годинник живий: привітання та дата оновлюються самі після півночі. Картка оновлення теж почувається чемно: хрестик, кнопка «Гаразд», системне «назад» і тап по затемненню — усе закриває її.
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
* 🧪 **Стеклянный дизайн (v2.4.3–v2.4.12: спокойный, адаптивный, настраиваемый)**: Плавающая навигационная панель размывает контент под собой в реальном времени — аппаратное гауссово размытие на Android 12+ и программный морозный эффект на Android 8–11 (обновляется только когда контент действительно изменился). Тон едва заметен — работу делает само размытие, а второй, более широкий и слегка увеличенный слой добавляет настоящее преломление по краям. **v2.4.7 «как в iOS»: на Android 13+ попиксельный AGSL-шейдер преломляет фон по ободку панели — настоящие рефракционные полосы, хроматическая дисперсия и спекулярный блик, скользящий по фаске при движении панели; более старые Android сохраняют вид v5.** Приложение **само измеряет мощность телефона** (ядра, RAM, low-RAM, Android) и выбирает уровень: *полный* эффект, *адаптивный* (легче размытие) или *матовый* (без записи и размытия — полностью непрозрачная панель: ничего не просвечивает, вид никогда не меняется). Монитор кадров понижает уровень при устойчивых подлагиваниях, режим экономии заряда мгновенно включает матовый — слабые телефоны не лагают, мощные сияют. В Настройках режим можно перебрать вручную в любой момент (кнопка «Выбрать» — в v2.4.6 выбор переработан: строки на всю ширину с иконками и описаниями, которые переносятся, так что ничего не обрезается ни на одном языке), **плюс новый ползунок «Насколько жидкое» (v2.4.6) — от спокойного матового до «очень жидкого» стекла**: панель и карточки перерисовываются прямо под пальцем, выбор сохраняется. Иконки и подписи панели остаются идеально чёткими поверх мороза. **v2.4.10 «настоящее стекло»: молочная пелена v2.4.9 убрана — в центре снова чистый морозный блюр с едва заметным тоном (~3–11% в зависимости от интенсивности), а вся магия линзы — в ободке: рефракция, хроматическая дисперсия, хайрлайн-фаска и скользящий блик; спекуляр спокойнее, диагональный sheen убран.** **v2.4.11: навигационная панель вернулась к классическому сплошному виду — непрозрачная поверхность, тонкая линия по краю, мягкая тень; без живого размытия и записи фона (эксперимент с «жидким стеклом» на панели завершён). Режим и интенсивность теперь управляют только видом карточек.** **v2.4.12: прозрачность вернулась — классическая панель снова полупрозрачная (~72–82% в зависимости от темы): контент мягко просвечивает сквозь неё, а иконки остаются чёткими; как и прежде — без размытия, линзы и записи фона.**
* 📦 **Минимум места на телефоне (v2.4.6)**: R8-сжатие кода и ресурсов убирает всё неиспользуемое (лишние классы библиотеки иконок весили вчетверо больше самого приложения). Dex стал меньше ~в 4 раза — скачивание примерно вдвое легче, а место на телефоне (Android раньше распаковывал копию 44 МБ неминифицированного кода в /data) уменьшается на десятки мегабайт.
* 🎯 **Плавная скользящая навигация**: Мягкий индикатор-пилюля скользит между вкладками с физикой пружины, перетаскиванием пальцем и сопротивлением на краях. С v2.4.2 подписи вкладок замеряются и детерминированно вмещаются в свои слоты («Налаштування» по-украински при любом масштабе шрифта больше не выходит за пилюлю), а переключение страниц полностью беззвучно: без вибрации, движение само по себе отклик.
* 🔄 **Обновления прямо из приложения**: Приложение само проверяет GitHub Releases (при запуске, не чаще раза в 3 часа) и предлагает новую версию в стеклянной карточке. APK скачивается **незаметно внутри приложения** (в приватный кеш — в проводнике ничего не появится, без уведомлений), а пакетный установщик открывается сам сразу после скачивания; файл обновления удаляется сразу после обновления. Сравнение версий — по имени версии, так что пересборка той же версии не показывает ложное «доступно обновление». В первый раз Android попросит разовое разрешение — с дружелюбной инструкцией из 3 шагов, и как только вы вернётесь с разрешением, скачивание продолжится само. Есть запасной вариант через браузер и карточка «Проверить обновления» в Настройках.
* 🧭 **Интерактивный тур при первом входе**: После короткой страницы приветствия гид подсвечивает настоящий интерфейс — «дырка» в затемнении вырезается точно вокруг реальной кнопки, а тап по ней действительно выполняет действие (перепрыгивает в Календарь, открывает настоящую форму добавления лекарства). В v2.4.1 — **8 шагов**: тур объясняет отметку приёма одним касанием, проходит все четыре страницы, включая Настройки, а завершается экраном «Всё готово!» вместо резкого обрыва; на экране приветствия можно (необязательно) ввести имя. v2.4.9: подсветка плавно скользит между шагами (пружинная «дырка»), а карточка ждёт, пока целевой элемент действительно появится, — прыжков во время тура больше нет. «Пропустить» всегда сверху, повтор — карточкой «Показать обучение снова» в Настройках. Переводы EN/UK/RU.
* 🌅 **Персональное приветствие (v2.4.0)**: утро теперь до 10:00, день до 17:00, вечер до 22:00, затем ночь — и приветствие может носить ваше имя: введите его один раз на экране приветствия или в Настройках — карточка «Персональное приветствие» (в v2.4.1 карандашик живёт только там, главный экран чище). Имя хранится только локально и убирается одним касанием. Часы живые: приветствие и дата обновляются сами после полуночи. Карточка обновления тоже стала вежливой: крестик, кнопка «Ок», системное «назад» и тап по затемнению — всё закрывает её.
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
1. **Local-only data**: All medication records, schedules and intake history live in a local Room database on your device. No account, no cloud, no Firebase — nothing to sign into. Android auto-backup is disabled (allowBackup=false), so your data never reaches any cloud, even via system mechanisms.
2. **Network = updates only**: The single network activity is checking/downloading app updates from GitHub (api.github.com, at most once every 3 hours or on your request). No medical data ever leaves the device. No analytics, no ads, no tracking.
3. **Medical Disclaimer**: Pill Tracker is a personal organizational assistant and does NOT substitute professional medical advice, diagnosis, or treatment. Always consult a qualified physician or pharmacist for medical decisions.
4. **Your rights (Ukraine № 2297-VI / GDPR)**: You own your data. Uninstalling the app or clearing its data erases everything instantly and irreversibly.

---

### 🇺🇦 Українська - Політика конфіденційності та Умови використання
1. **Дані лише локально**: Усі записи про ліки, розклади та історію прийому зберігає локальна база Room на вашому пристрої. Без акаунтів, без хмари, без Firebase. Автоматичне резервне копіювання Android вимкнено (allowBackup=false) — дані не потрапляють у хмару навіть системними засобами.
2. **Мережа — лише оновлення**: Єдина мережева активність — перевірка й завантаження оновлень із GitHub (api.github.com, не частіше ніж раз на 3 години або за вашим запитом). Медичні дані не залишають пристрій. Аналітики, реклами й стеження немає.
3. **Медичне застереження**: Pill Tracker — особистий органайзер і НЕ замінює професійну медичну консультацію, діагностику чи лікування. З медичних питань звертайтеся до лікаря чи фармацевта.
4. **Ваші права (№ 2297-VI / GDPR)**: Дані — ваші. Видалення застосунку або очищення даних знищує все одразу й безповоротно.

---

### ru Русский - Политика конфиденциальности и Условия использования
1. **Данные только локально**: Все записи о лекарствах, расписаниях и история приёма хранятся в локальной базе Room на вашем устройстве. Без аккаунтов, без облака, без Firebase. Автоматическое резервное копирование Android отключено (allowBackup=false) — данные не попадают в облако даже системными средствами.
2. **Сеть — только обновления**: Единственная сетевая активность — проверка и загрузка обновлений с GitHub (api.github.com, не чаще раза в 3 часа или по вашему запросу). Медицинские данные не покидают устройство. Аналитики, рекламы и слежки нет.
3. **Медицинская оговорка**: Pill Tracker — личный органайзер и НЕ заменяет профессиональную медицинскую консультацию, диагностику или назначение врача. По медицинским вопросам обращайтесь к квалифицированному специалисту.
4. **Ваши права (№ 2297-VI / GDPR)**: Данные — ваши. Удаление приложения или очистка данных уничтожает всё сразу и безвозвратно.

---

<a id="install-updates"></a>
## 📲 Installation & Updates / Встановлення та оновлення / Установка и обновление

### 🇬🇧 English
- **One channel**: always install and update from **GitHub Releases → latest release → `pill-tracker.apk`**. Every release is newer than the previous one (versionCode = `2311 + build number`, e.g. v1.92 = 2403 — strictly growing), so Android always accepts it as an in-place update. Since v2.3.2 the app can also update itself in place — see «In-App Updates» above.
- **One permanent signature**: since release `v1.80` every APK is signed with the same certificate (the keystore lives in the repo) — every release installs on top of the previous one.
- **Very old builds only**: if your installed copy is older than `v1.80`, Android will refuse to update it (those early builds were signed with throw-away keys). Uninstall the old app **once**, install the latest release — after that, every future update installs right on top, no uninstall ever needed.

### 🇺🇦 Українська
- **Один канал**: встановлюйте та оновлюйте додаток лише з **GitHub Releases → останній реліз → `pill-tracker.apk`**. Кожен новий реліз вищий за попередній (versionCode = `2311 + номер збірки`, напр. v1.92 = 2403 — зростає строго), тож Android завжди приймає його як оновлення поверх встановленого. Починаючи з v2.3.2 застосунок уміє оновлюватися ще й сам — див. «Оновлення прямо із застосунку» вище.
- **Одна постійна підпись**: починаючи з релізу `v1.80`, кожен APK підписаний тим самим сертифікатом (кейстор зберігається в репозиторії) — кожен реліз встановлюється поверх попереднього.
- **Тільки для дуже старих збірок**: якщо встановлена версія старіша за `v1.80`, Android не дозволить оновити її (ті ранні збірки були підписані разовими ключами). Видаліть старий додаток **один раз** і встановіть останній реліз — після цього всі майбутні оновлення встановлюються поверх без видалення.

### ru Русский
- **Один канал**: устанавливайте и обновляйте приложение только из **GitHub Releases → последний релиз → `pill-tracker.apk`**. Каждый новый релиз выше предыдущего (versionCode = `2311 + номер сборки`, напр. v1.92 = 2403 — растёт строго), поэтому Android всегда принимает его как обновление поверх установленного. Начиная с v2.3.2 приложение умеет обновляться ещё и само — см. «Обновления прямо из приложения» выше.
- **Одна постоянная подпись**: начиная с релиза `v1.80`, каждый APK подписан одним и тем же сертификатом (кейстор хранится в репозитории) — каждый релиз ставится поверх предыдущего.
- **Только для очень старых сборок**: если установленная версия старше `v1.80`, Android не даст её обновить (те ранние сборки были подписаны одноразовыми ключами). Удалите старое приложение **один раз** и поставьте последний релиз — после этого все будущие обновления ставятся поверх, без удаления.

---

<a id="build-instructions"></a>
## ⚙️ Building & CI/CD

This repository includes a full **GitHub Actions CI/CD pipeline** (`.github/workflows/android.yml`) that automatically builds and signs the APK on every commit. The `versionCode` is computed at build time from a strictly growing formula (`2311 + run number`) defined in `app_build.gradle.kts`.

**Release signing is an explicit policy (v2.4.7, fixed in v2.4.8)** — never a silent fallback:
- With the `KEYSTORE_BASE64` / `STORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` secrets set, CI signs with the real release key.
- Without them, the build opts in **from `gradle.properties`** (`allowDebugSigning=true` — a visible, commented line shipped with the repo) and **loudly warns** that the APK is debug-signed. That is an honest open-source distribution decision (GitHub Releases + the in-app updater live on the stable repo key), visible in every log.
- v2.4.8 note: the opt-in used to be a `-PallowDebugSigning=true` **flag inside the CI workflow**, which broke builds for anyone whose `.github/workflows/android.yml` was not re-uploaded (the workflow is the one file nobody syncs). The opt-in now rides in `gradle.properties` — a plain repo file that goes up with every release — so **any** workflow command, old or new, keeps building. Strict mode still exists: delete the line and every release packaging task fails with instructions instead of quietly shipping a debug-signed "release".
- Locally, `gradle assembleRelease` uses the same `gradle.properties` opt-in (or `-PallowDebugSigning=true` / `ALLOW_DEBUG_SIGNING=true`); without any opt-in it **fails** with instructions. A production APK can never be silently debug-signed.

Since v2.4.7 the code namespace is the real `com.aistudio.meditracker` (was the leftover template `com.example`). The `applicationId` did not change, so in-place updates keep working exactly as before.

### Local Build
```bash
# Clone the repository
git clone https://github.com/unbrokensociety/Pill-Tracker.git
cd Pill-Tracker

# Build debug APK
./gradlew assembleDebug

# Build release APK — pick ONE:
KEYSTORE_PATH=release.keystore STORE_PASSWORD=... KEY_ALIAS=... KEY_PASSWORD=... \
  ./gradlew assembleRelease           # real release key
./gradlew assembleRelease             # debug-signed: opt-in already in gradle.properties
./gradlew assembleRelease -PallowDebugSigning=true   # same, explicit flag
```
The compiled `.apk` will be generated at:
`app/build/outputs/apk/debug/pill-tracker.apk`

### Project Layout
Kotlin sources are kept flat in the repository root next to `build.gradle.kts` — the root build script syncs them into the standard Android source tree (`app/src/main/java/com/aistudio/meditracker/...`) automatically before every build, so GitHub Actions always compiles the newest code. The sync **wipes `app/src/main/java` first**: the flat set is the single source of truth, which is exactly what made the v2.4.7 namespace migration self-healing — upload the new files, and the next build removes the old `com/example` tree by itself.
