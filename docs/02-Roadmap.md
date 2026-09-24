# Roadmap — IntentMarket («Мой OLX»)

## Phase 1 — Project Foundation & Prototype ✅ (Выполнено)
- [x] Инициализация Git-репозитория и привязка к `github.com/sway-nick/my-olx`.
- [x] Каркас нативного Android-приложения (Kotlin DSL, Compose BOM, Material 3, SDK 35).
- [x] Двуязычный AI-парсер (UA / RU) естественного языка.
- [x] Location-First движок сопоставления по Одессе (Таирова, Аркадия, Черёмушки, Центр).
- [x] Две темы (Светлая + глубокая Тёмная OLED) с переключателем.
- [x] Универсальная «Умная анкета» с предзаполнением и выбором фото.
- [x] Интерактивный Live Demo для GitHub Pages (`index.html`).
- [x] Успешная компиляция `app-debug.apk`.

---

## Phase 2 — Supabase Cloud Backend ✅ (Выполнено)
- [x] Создание организации и проекта `IntentMarket` на Supabase.
- [x] Схема PostgreSQL: расширения `PostGIS`, `pgvector`, `pg_trgm`.
- [x] Таблицы: `profiles`, `categories`, `intents`, `listings`, `external_listings`, `matches`, `reports`.
- [x] Row Level Security (RLS) для защиты пользовательских данных.
- [x] Хранимая функция гибридного матчинга `match_demand_to_listings`.
- [x] Хранимая функция обратного матчинга для продавца `count_matching_demands`.
- [x] Базовый срез реальных данных по Одессе (генераторы, аренда жилья, услуги).

---

## Phase 3 — Edge Functions, Ingestion & Live Sync 🔄 (В процессе)
- [ ] Развертывание Supabase Edge Function `parse-intent` для интеграции с LLM API.
- [ ] Фоновый воркер `SourceAdapter` для периодического (1 раз в 1–3 дня) обновления внешних предложений.
- [ ] Валидация свежести внешних ссылок (Head-check на 404 / снятые объявления).
- [ ] Подключение Push-уведомлений через Firebase Cloud Messaging (FCM).

---

## Phase 4 — Google Play Release Preparation 📱
- [ ] Настройка Release Signing Configs и генерация Android App Bundle (`.aab`).
- [ ] Добавление Privacy Policy и страницы удаления аккаунта (`/privacy`, `/account-deletion`).
- [ ] Реализация экранов модерации и жалоб внутри приложения.
- [ ] Подготовка скриншотов и графики для Google Play Console (5.5" и 6.5" телефоны).
- [ ] Прохождение ревью в Google Play.

---

## Phase 5 — Scale & Future Features 🚀
- [ ] Расширение географии (Киев, Днепр, Львов, Харьков).
- [ ] Голосовой ввод запросов (Voice Input).
- [ ] AI-распознавание товаров по фотографии.
- [ ] Автоматическая калибровка весов формулы ранжирования на основе реального CTR.
