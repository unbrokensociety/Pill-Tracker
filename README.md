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
  <a href="#russian"><b>[ru]</b></a>
</p>

---

<a id="english"></a>
## 🇬🇧 English

### 🌟 Key Highlights
* ⏰ **Exact Alarm Engine (`AlarmManager.setAlarmClock`)**: Guarantees alarm notifications trigger precisely on time without OS battery-saver or Doze mode delays.
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
| **Database** | Room Persistence Library with KSP |
| **Asynchronous** | Kotlin Coroutines & `StateFlow` / `SharedFlow` |
| **Notifications** | `AlarmManager`, `BroadcastReceiver`, Android Notification Channels |

---

<a id="ukrainian"></a>
## 🇺🇦 Українська

### 🌟 Основні можливості
* ⏰ **Точні нагадування (`setAlarmClock`)**: Апаратний системний будильник спрацьовує хвилина в хвилину навіть у режимі глибокого сну пристрою (Doze mode).
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
* 🧪 **Эффект "Жидкое Стекло" (Liquid Glass)**: Современный интерфейс с аппаратным фоновым размытием (`RenderEffect`) и стеклянными карточками.
* 🎯 **Плавная скользящая навигация**: Скользящий индикатор переключения вкладок нижней панели с физикой пружинных анимаций.
* 📅 **Интерактивный Календарь**: Удобный график приема на выбранный день с подтверждением в один клик.
* 💊 **Персональный каталог**: Настройка дозировки, нескольких времен приема, формы препарата и цветовых меток.
* 📊 **Аналитика приемов**: Отслеживание процента выполнения дневного плана и серии регулярности.
* 🌍 **Мультиязычность**: Динамическое переключение языка интерфейса без перезапуска приложения.

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
