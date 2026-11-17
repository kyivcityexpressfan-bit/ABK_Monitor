# ABK Monitor + ABK Control

Два отдельных Android-приложения на Kotlin + Jetpack Compose:
- `monitor` — просмотр официальных районных статусов и уведомлений.
- `control` — вход администратора и публикация текстовых уведомлений/статусов.

Проект намеренно не отображает координаты, маршруты, траектории или прогнозы движения реальных БПЛА. На карте/экране мониторинга показываются только официальные предупреждения на уровне области/района.

## Требования
- Android Studio Ladybug или новее
- JDK 17
- Android SDK 35
- Firebase project

## Подключение Firebase
1. Создайте проект в Firebase Console.
2. Добавьте два Android-приложения:
   - `ua.abk.monitor`
   - `ua.abk.control`
3. Скачайте `google-services.json` отдельно для каждого приложения.
4. Положите файлы сюда:
   - `monitor/google-services.json`
   - `control/google-services.json`
5. Включите Authentication → Email/Password и Cloud Firestore.
6. В Firebase Authentication создайте пользователя-администратора.
7. Назначьте этому пользователю custom claim `admin: true` через доверенный серверный Firebase Admin SDK. Не назначайте роль из Android-приложения.
8. Примените правила из `firebase/firestore.rules`.

## Сборка APK
Откройте папку проекта в Android Studio и дождитесь Gradle Sync.
Затем:
- Build → Build Bundle(s) / APK(s) → Build APK(s)
- APK мониторинга: `monitor/build/outputs/apk/debug/monitor-debug.apk`
- APK админки: `control/build/outputs/apk/debug/control-debug.apk`

Или из терминала:
```bash
./gradlew :monitor:assembleDebug :control:assembleDebug
```

## Структура Firestore
`alerts/{alertId}`:
- `title`: string
- `body`: string
- `region`: string
- `severity`: `info | warning | alert | all_clear`
- `createdAt`: server timestamp
- `active`: boolean
- `createdBy`: uid

`regional_status/{regionId}`:
- `region`: string
- `status`: `unknown | safe | alert`
- `updatedAt`: server timestamp
- `updatedBy`: uid

Приложение мониторинга читает активные уведомления и статусы. Админка требует Firebase Auth и custom claim `admin=true` для записи.

## Важно
Это исходный Android Studio проект, а не готовые подписанные APK. Для сборки нужны Android Studio/SDK и два Firebase `google-services.json`. Не публикуйте service-account ключи или приватные секреты в приложениях.

## Сборка APK через GitHub с телефона

В репозитории должен быть весь проект из ZIP. Workflow находится в `.github/workflows/build-apks.yml`.

1. Создайте на GitHub новый репозиторий, например `ABK-Monitor-Control`.
2. Загрузите содержимое ZIP в корень репозитория (именно содержимое, не дополнительную вложенную папку).
3. Откройте вкладку **Actions** и выберите **Build ABK Android APKs**.
4. Нажмите **Run workflow**. Также сборка запускается при push в `main`.
5. После успешной сборки откройте завершённый запуск и скачайте два отдельных Artifacts:
   - `ABK-Monitor-APK`
   - `ABK-Control-APK`

### Важное про Firebase
Без GitHub Secrets workflow использует демонстрационные Firebase-конфигурации: APK смогут собраться, но не подключатся к вашей базе. Для реальной работы добавьте в Settings → Secrets and variables → Actions два Repository secrets:
- `MONITOR_FIREBASE_JSON` — полное содержимое `google-services.json` приложения `ua.abk.monitor`.
- `CONTROL_FIREBASE_JSON` — полное содержимое `google-services.json` приложения `ua.abk.control`.

Не добавляйте service-account JSON, приватные ключи или пароли в репозиторий. Firebase Admin custom claim `admin=true` должен назначаться доверенным сервером, не Android-клиентом.

Сборка в workflow — debug APK для тестирования, не релизная подписанная публикация в Google Play.
