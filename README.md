# Pill Tracker 💊📱

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Language" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="UI" />
  <img src="https://img.shields.io/badge/Database-Room-0052CC?style=for-the-badge&logo=sqlite&logoColor=white" alt="Database" />
</p>

A modern, elegant, and reliable Android application for tracking medication intakes and managing daily pill schedules. Features a sleek **Liquid Glass** Material 3 UI, exact alarm notifications, dynamic language support, and interactive calendar views.

---

## 🌐 Languages / Мови / Языки

- [🇬🇧 English](#-english-default)
- [🇺🇦 Українська](#-українська)
- [🇷🇺 Русский](#-русский)

---

## 🇬🇧 English (Default)

### Key Features
* ⏰ **Exact Alarm Notifications**: High-priority alarm clock triggers so you never miss a dose, even in battery saver / doze mode.
* 🧪 **Liquid Glass UI**: Modern translucent glassmorphic components, fluid animations, and smooth tab slide transitions.
* 📅 **Calendar & History**: Interactive daily schedule overview with one-tap intake completion tracking.
* 💊 **Custom Medication Setup**: Configure custom dosage, intake times, start dates, and visual color coding.
* 📊 **Adherence Analytics**: Track your today's completion progress percentage and streak stats.
* 🌍 **Multi-Language**: Instant switching between English, Ukrainian, and Russian.
* 🎨 **Flexible Themes**: Light, Dark, System, and Brand accent color themes.

### Technical Stack
- **Architecture**: MVVM with Clean Repository Pattern
- **UI Framework**: Jetpack Compose + Material 3 + Animated Content
- **Database**: Room Persistence Library
- **Asynchronous Flow**: Kotlin Coroutines & `StateFlow`
- **Alarm Engine**: `AlarmManager.setAlarmClock` + `BroadcastReceiver`

---

## 🇺🇦 Українська

### Основні можливості
* ⏰ **Точні нагадування**: Алгоритм `setAlarmClock` гарантує спрацьовування сповіщень точно вчасно без затримок системи.
* 🧪 **Дизайн "Рідке Скло"**: Експериментальний інтерфейс у стилі Liquid Glass з плавними анімаціями переходу між вкладками.
* 📅 **Календар та Історія**: Зручний огляд денного розкладу прийому ліків з можливістю відмітити прийом в один дотик.
* 💊 **Гнучке налаштування**: Вказівка дозування, декількох часів прийому, дати початку та колірного позначення.
* 📊 **Аналітика та Статистика**: Відстеження відсотка виконаних прийомів за день.
* 🌍 **Багатомовність**: Підтримка української, англійської та російської мов.

---

## 🇷🇺 Русский

### Основные возможности
* ⏰ **Точные напоминания**: Алгоритм `setAlarmClock` гарантирует мгновенное срабатывание уведомлений без системных задержек.
* 🧪 **Дизайн "Жидкое Стекло"**: Современный интерфейс в стиле Liquid Glass с плавными анимированными переходами между вкладками.
* 📅 **Календарь и История**: Удобный обзор дневного расписания с отменой и подтверждением приема в один клик.
* 💊 **Гибкая настройка**: Указание дозировки, нескольких времен приема, даты начала и цветовых меток.
* 📊 **Статистика**: Отслеживание процента выполненных приемов за день.
* 🌍 **Мультиязычность**: Поддержка украинского, английского и русского языков.

---

## 🛠 Building & CI/CD

This repository includes a pre-configured **GitHub Actions CI/CD** workflow (`.github/workflows/android.yml`).

To build locally:
```bash
./gradlew assembleDebug
```
The output APK file will be generated at `app/build/outputs/apk/debug/pill-tracker.apk`.
