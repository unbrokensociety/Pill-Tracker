# Pill Tracker 💊📱

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Language" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="UI" />
  <img src="https://img.shields.io/badge/Database-Room-0052CC?style=for-the-badge&logo=sqlite&logoColor=white" alt="Database" />
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" alt="License" />
</p>

<p align="center">
  <b>Pill Tracker</b> is a modern, high-performance, and reliable Android application designed to manage daily medication schedules, send exact intake reminders, and track adherence history. Built with modern Jetpack Compose, an experimental <b>Liquid Glass</b> design system, and modern Android architecture.
</p>

---

## 🌐 Quick Navigation / Навігація / Навигация

<p align="center">
  <a href="#english"><b>🇬🇧 English</b></a> &nbsp;|&nbsp;
  <a href="#ukrainian"><b>🇺🇦 Українська</b></a> &nbsp;|&nbsp;
  <a href="#russian"><b>[RU] Русский</b></a> &nbsp;|&nbsp;
  <a href="#privacy-terms"><b>📜 Privacy & Terms</b></a>
</p>

---

<a id="english"></a>
## 🇬🇧 English

### 🌟 Key Highlights
* ⏰ **Exact Alarm Engine (`AlarmManager.setAlarmClock`)**: Guarantees alarm notifications trigger precisely on time without OS battery-saver or Doze mode delays.
* ⚡ **Automatic Cloud Sync**: Real-time two-way synchronization between local Room DB and Firebase Firestore.
* 🔐 **Secure Firebase Authentication**: Email & Password validation with email link verification and instant Password Reset.
* 🩺 **Smart Doctor Reports & Barcode/QR Generator**: Export clinical summary reports with compliance analytics and dual-format scanner card (Code 128 & QR Code).
* 🧪 **Liquid Glass Design**: Experimental glassmorphism UI with real hardware blur (`RenderEffect`), light reflections, and translucent frosted surfaces.
* 🎯 **Smooth Tab Navigation**: Dynamic liquid capsule indicator that glides across bottom navigation tabs with spring physics.
* 📅 **Interactive Calendar & Tracker**: Track daily doses, mark intakes as taken/skipped, and review historical compliance.
* 💊 **Comprehensive Medication Management**: Customize dose amounts, pill shapes/icons, color coding, start dates, and multiple daily reminders.
* 📊 **Adherence Analytics**: Real-time progress bars, completion percentages, and streak tracking.
* 🌍 **Full Localization**: Dynamic in-app language switching between English, Ukrainian, and Russian.

### 🛠 Tech Stack & Architecture
| Layer | Technologies |
| :--- | :--- |
| **Language** | 100% Kotlin |
| **UI Framework** | Jetpack Compose, Material 3, Navigation Compose, Compose Animation |
| **Architecture** | Clean Architecture + MVVM (Model-View-ViewModel) |
| **Database** | Room Persistence Library with KSP & Firebase Firestore |
| **Asynchronous** | Kotlin Coroutines & `StateFlow` / `SharedFlow` |
| **Notifications** | `AlarmManager`, `BroadcastReceiver`, Android Notification Channels |

---

<a id="ukrainian"></a>
## 🇺🇦 Українська

### 🌟 Основні можливості
* ⏰ **Точні нагадування (`setAlarmClock`)**: Апаратний системний будильник спрацьовує хвилина в хвилину навіть у режимі глибокого сну пристрою (Doze mode).
* ⚡ **Автоматична хмарна синхронізація**: Двостороння синхронізація бази даних Room із Firebase Firestore в реальному часі.
* 🔐 **Захищена автентифікація**: Валідація пароля через Firebase Auth, підтвердження пошти та функція відновлення пароля ("Забули пароль?").
* 🩺 **Розумні звіти та QR/Штрихкод для лікаря**: Експорт клінічного звіту з відсотком дотримання розкладу та двоформатним кодом (Code 128 та QR).
* 🧪 **Ефект "Рідке Скло" (Liquid Glass)**: Сучасний напівпрозорий інтерфейс із системним розмиттям, світловими відблисками та м'якими тінями.
* 🎯 **Плавна анімована навігація**: Плаваючий індикатор нижньої панелі, який м'яко ковзає між вкладками з фізикою пружини.
* 📅 **Інтерактивний Календар**: Зручний перегляд розкладу на будь-який день із можливістю відмітити прийом ліків в один дотик.
* 💊 **Гнучкий каталог ліків**: Налаштування дозування, часу прийому, дати початку курсів та індивідуального колірного оформлення.
* 📊 **Аналітика та Статистика**: Відстеження відсотка успішного дотримання графіку та аналіз регулярності.
* 🌍 **Багатомовний інтерфейс**: Миттєве переключення мови додатку (Українська, Англійська, Російська).

---

<a id="russian"></a>
## [RU] Русский

### 🌟 Основные возможности
* ⏰ **Точные напоминания (`setAlarmClock`)**: Системный будильник срабатывает точно в указанную минуту без задержек режима энергосбережения.
* ⚡ **Автоматическая облачная синхронизация**: Двусторонняя синхронизация локальной БД Room с Firebase Firestore в реальном времени.
* 🔐 **Защищенная авторизация**: Проверка пароля через Firebase Auth, подтверждение почты и сброс пароля при утере ("Забыли пароль?").
* 🩺 **Умные отчёты и QR/Штрихкод для врача**: Экспорт медицинского отчёта с процентом регулярности приёма и выбор формата (Code 128 или QR-код).
* 🧪 **Эффект "Жидкое Стекло" (Liquid Glass)**: Современный интерфейс с аппаратным фоновым размытием (`RenderEffect`) и стеклянными карточками.
* 🎯 **Плавная скользящая навигация**: Скользящий индикатор переключения вкладок нижней панели с физикой пружинных анимаций.
* 📅 **Интерактивный Календарь**: Удобный график приема на выбранный день с подтверждением в один клик.
* 💊 **Персональный каталог**: Настройка дозировки, нескольких времен приема, формы препарата и цветовых меток.
* 📊 **Аналитика приемов**: Отслеживание процента выполнения дневного плана и серии регулярности.
* 🌍 **Мультиязычность**: Динамическое переключение языка интерфейса без перезапуска приложения.

---

<a id="privacy-terms"></a>
## 📜 Privacy Policy & Terms of Service / Політика конфіденційності / Политика конфиденциальности

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

<a id="build-instructions"></a>
## ⚙️ Building & CI/CD

This repository includes a full **GitHub Actions CI/CD pipeline** (`.github/workflows/android.yml`) that automatically builds and signs the debug APK on every commit.

### Local Build
```bash
# Clone the repository
git clone https://github.com/user/pill-tracker.git
cd pill-tracker

# Build debug APK
./gradlew assembleDebug
```
The compiled `.apk` will be generated at:
`app/build/outputs/apk/debug/pill-tracker.apk`
