# 🦻 DeafCall — Звонилка для глухонемых

**Android приложение для людей с нарушением слуха**
Превращает голос собеседника в текст на экране в реальном времени,
а ваш набранный ответ — озвучивает через динамик.

---

## 📱 Требования

| Параметр | Значение |
|---|---|
| minSdk | **26** (Android 8.0 Oreo) |
| targetSdk | **35** (Android 15) |
| compileSdk | **35** |
| Kotlin | 2.0.21 |
| Gradle | 8.7.0 |

---

## 🏗️ Архитектура

```
MVVM + Hilt DI + Coroutines + Flow
```

```
com.deafcall/
├── DeafCallApp.kt              — Application class + Notification Channels
├── di/
│   └── AppModule.kt            — Hilt DI: Room, DataStore
├── model/
│   ├── Models.kt               — CallState, CallInfo, TranscriptEntry, UserSettings
│   └── Database.kt             — Room DB + DAO
├── service/
│   ├── DeafCallInCallService.kt  ← ГЛАВНЫЙ: управление звонком
│   ├── SpeechRecognitionService.kt ← STT в реальном времени
│   └── OtherServices.kt          — Screening, Boot, Notification
├── viewmodel/
│   └── MainViewModel.kt        — UI State, бизнес-логика
├── utils/
│   ├── SettingsRepository.kt   — DataStore preferences
│   ├── CallRepository.kt       — Room operations
│   ├── TtsManager.kt           — TextToSpeech
│   └── VibrationHelper.kt      — VibrationEffect API
└── ui/
    ├── MainActivity.kt         — Navigation host
    ├── theme/Theme.kt          — MaterialTheme + Colors
    ├── components/Components.kt — Reusable Composables
    └── screens/
        ├── IncomingCallScreen.kt
        ├── ActiveCallScreen.kt
        └── Screens.kt           — Home, Settings, History, Permissions
```

---

## 🔑 Ключевые Android API

### InCallService (API 23+)
```kotlin
class DeafCallInCallService : InCallService() {
    override fun onCallAdded(call: Call) {
        // Вызывается системой при КАЖДОМ звонке
        // Здесь запускаем STT и показываем UI
    }
}
```
> ⚠️ Требует: приложение должно быть **звонилкой по умолчанию**
> AndroidManifest: `android.telecom.IN_CALL_SERVICE_UI = true`

### SpeechRecognizer — Partial Results (real-time)
```kotlin
putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)  // ← КЛЮЧЕВОЕ!

override fun onPartialResults(bundle: Bundle?) {
    // Вызывается МГНОВЕННО пока человек говорит
    val partial = bundle.getStringArrayList(RESULTS_RECOGNITION)?.first()
    // Показываем на экране без ожидания паузы
}

override fun onResults(bundle: Bundle?) {
    // Финальный точный результат
    // Перезапускаем для непрерывного распознавания
    if (isActiveCall) startListening()
}
```

### VibrationEffect API (API 26+)
```kotlin
// Входящий звонок — ритмичная вибрация
VibrationEffect.createWaveform(
    longArrayOf(0, 600, 300, 600),  // паузы/вибрации
    intArrayOf(0, 200, 0, 200),     // амплитуды
    0  // повторять с индекса 0
)

// Одиночный удар при новом слове
VibrationEffect.createOneShot(30, 60)
```

---

## 🚀 Сборка и запуск

### 1. Клонировать
```bash
git clone https://github.com/yourname/DeafCall.git
cd DeafCall
```

### 2. Открыть в Android Studio
- Android Studio Ladybug (2024.2+) или новее
- Gradle sync произойдёт автоматически

### 3. Добавить иконки-заглушки
Создайте файлы в `app/src/main/res/drawable/`:
- `ic_hearing.xml` — иконка приложения (ухо)
- `ic_call_accept.xml` — принять звонок
- `ic_call_end.xml` — завершить звонок

### 4. Запустить на устройстве
```bash
./gradlew installDebug
```

> ⚠️ **Эмулятор не подходит** для тестирования звонков!
> Нужно реальное Android устройство.

---

## 📋 Необходимые шаги после установки

1. **Установить как звонилку по умолчанию** — приложение попросит само
2. **Выдать разрешения**:
   - Микрофон (RECORD_AUDIO)
   - Телефон (READ_PHONE_STATE, ANSWER_PHONE_CALLS)
   - Контакты (READ_CONTACTS)
   - Уведомления (Android 13+)
3. **Интернет** — нужен для Google Speech-to-Text (облачное STT)

---

## 🔮 Что можно улучшить

| Фича | Сложность | Описание |
|---|---|---|
| Офлайн STT | ⭐⭐⭐ | ML Kit Speech Recognition (без интернета) |
| Жестовое управление | ⭐⭐ | Свайп вверх = принять, вниз = отклонить |
| Flashlight API | ⭐ | Camera2 API — вспышка при звонке |
| Запись звонка | ⭐⭐⭐ | MediaRecorder + хранение MP3 |
| Shared preferences UI | ⭐ | Экспорт транскрипций в TXT/PDF |
| Wearable поддержка | ⭐⭐⭐ | WearOS companion app |
| AI Summary | ⭐⭐⭐⭐ | Краткое резюме разговора (Gemini API) |

---

## 📄 Лицензия
MIT License — свободное использование и модификация
