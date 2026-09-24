# Changelog — IntentMarket («Мой OLX»)

## [0.2.0] — 2026-09-25
### Added
- Полная интеграция с облаком **Supabase** (`IntentMarket` в регионе `eu-west-1`).
- Применены миграции: PostGIS, pgvector, RLS-политики.
- Реализованы и протестированы хранимые процедуры `match_demand_to_listings` и `count_matching_demands`.
- Живое подключение веб-версии на GitHub Pages к базе данных Supabase через `@supabase/supabase-js`.
- Создана документация проекта в `docs/` (`Backlog`, `Roadmap`, `Architecture`, `ADR`, `Changelog`, `Product-Map`, `MOBILE_UI_UX_STANDARDS.md`, `PROJECT_RULES.md`).

## [0.1.0] — 2026-09-24
### Added
- Инициализация проекта Android на Jetpack Compose + Material 3 (Target SDK 35).
- Двухрежимный главный экран («🔍 Ищу» и «📦 Предлагаю»).
- Двуязычный AI Intent Parser (UA / RU) для генераторов, аренды квартир и услуг.
- Универсальная «Умная анкета» с автозаполнением и загрузкой фото.
- Location-First движок ранжирования по районам Одессы.
- Интеграция слотов Google AdMob (нативные карточки и предупреждение о переходе).
- Интерактивный Live Demo `index.html` на GitHub Pages.
- Успешная сборка первого `app-debug.apk`.
