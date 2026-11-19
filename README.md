# 🍔 MLBB Undersea Restaurant Auto-Bot (Android)

Автоматический бот-помощник для кулинарного события **Undersea Restaurant** (коллаборация SpongeBob / Undersea Bash / Friend Fest) в **Mobile Legends: Bang Bang**.

---

## 📥 Быстрая установка загрузчика на Android (Без ПК и без Root)

Чтобы бот работал прямо на вашем телефоне, требуется Android-загрузчик скриптов **AutoX.js**:

### 1. Скачайте загрузчик APK:
* 📥 **[Скачать AutoX.js APK (Официальный релиз v6.6.4)](https://github.com/flysnow-org/AutoX/releases/download/v6.6.4/autox-v6.6.4-universal.apk)**  
  *(Зеркало всех версий: [AutoX Releases](https://github.com/flysnow-org/AutoX/releases))*

### 2. Скачайте скрипт бота:
* 📄 **[Скачать mlbb_undersea_restaurant_bot.js](https://raw.githubusercontent.com/avtotronix/Mobile-Legend-even-helper/main/scripts/mlbb_undersea_restaurant_bot.js)** *(нажмите правой кнопкой / долгим тапом -> Сохранить как...)*

### 3. Инструкция по запуску (за 1 минуту):
1. Установите скачанный **AutoX.js APK** на телефон.
2. Откройте AutoX.js и выдайте два разрешения:
   * **Спец. возможности (Accessibility)** — чтобы бот мог нажимать по экрану.
   * **Отображение поверх других окон** — чтобы висел плавающий виджет с кнопкой «Старт».
3. Нажмите в AutoX.js кнопку **«+» -> Импорт** и выберите файл `mlbb_undersea_restaurant_bot.js`.
4. Нажмите **Запустить** — на экране появится аккуратная плавающая кнопка `[ ▶ СТАРТ ]`.
5. Откройте **Mobile Legends: Bang Bang**, зайдите на кухню ивента и нажмите `▶ СТАРТ`!

---

## 🚀 Особенности бота

* **Однокнопочный запуск:** Нажал `▶ СТАРТ` — бот всё делает сам.
* **Плавающий виджет поверх игры:** Кнопки `[ ▶ СТАРТ ]` и `[ ⏸ ПАУЗА ]` всегда под рукой.
* **Полная автоматизация 6 блюд:**
  * 🍔 **Бургеры:** Непрерывная жарка котлет на 4 конфорках + Сборка (булка, котлета, томат, салат).
  * 🍕 **Пицца:** Сборка теста, лука, ветчины + Запекание в печи.
  * 🍟 **Фритюр:** Картофель фри и жареная курица.
  * 🥤 **Напитки:** Моментальный налив Колы и Мороженого.
  * 👥 **Отдача:** Мгновенное обслуживание всех 3 слотов клиентов и приоритет для срочных заказов (Rush Orders).
* **Адаптивные координаты:** Автоматически подстраивается под любое разрешение экрана (1080p, 1440p, 720p).

---

## ⚙️ Сборка нативного APK через GitHub Actions

В репозиторий добавлен автоматический конвейер CI/CD:
* При каждом обновлении кода GitHub автоматически компилирует готовый файл **`MLBB-Restaurant-Bot-APK`**.
* Скачать свежесобранный APK можно во вкладке **[Actions -> Build Android APK](https://github.com/avtotronix/Mobile-Legend-even-helper/actions)** внизу страницы сборки (раздел *Artifacts*).

---

## 📂 Структура проекта

```text
├── .github/workflows/
│   └── build-apk.yml        # Автосборка APK на GitHub Actions
├── app/                     # Исходный код нативного Android-приложения (Kotlin)
│   ├── src/main/java/com/mlbb/chefbot/
│   │   ├── MainActivity.kt               # Минималистичный экран с 1 кнопкой
│   │   ├── FloatingOverlayService.kt     # Плавающий поверх MLBB виджет
│   │   ├── BotAccessibilityService.kt    # Служба выполнения нажатий
│   │   └── CookingEngine.kt              # Логика кухни и тайминги
│   └── src/main/res/        # Разметка, иконки, цвета
├── scripts/
│   └── mlbb_undersea_restaurant_bot.js   # Готовый автономный скрипт для загрузчика
└── README.md
```

---

## 📜 Лицензия
MIT License.
