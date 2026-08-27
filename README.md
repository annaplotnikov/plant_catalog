# Каталог растений (полив)

Многоязычное консольное приложение для управления каталогом комнатных растений с напоминаниями о поливе.  
Позволяет добавлять растения, отслеживать дату последнего полива, получать уведомления о необходимости полива и экспортировать каталог.

## Особенности
- Добавление растений с указанием названия, типа, частоты полива (дни) и даты последнего полива.
- Автоматический расчёт следующей даты полива.
- Цветная индикация статуса полива:
  - **Зелёный** – полив сегодня или завтра.
  - **Жёлтый** – пора поливать (прошло > частоты).
  - **Красный** – срочно (прошло > частоты × 1.5).
- Просмотр всего каталога с фильтром по типу.
- Отметка о поливе растения (обновление даты).
- Удаление растений из каталога.
- Экспорт каталога в JSON, CSV, TXT.
- Сохранение данных в локальном JSON-файле.
- Поддержка аргументов командной строки для быстрого управления.

## Установка и запуск
Для каждого языка требуются соответствующие инструменты.

### Запуск на разных языках

1. **Python**  
   Запуск: `python plant_catalog.py --add "Фикус" --type "дерево" --frequency 7 --last 2026-08-20`

2. **JavaScript (Node.js)**  
   Установка: `npm install commander chalk`  
   Запуск: `node plant_catalog.js --add "Фикус" --type "дерево" --frequency 7 --last 2026-08-20`

3. **Go**  
   Запуск: `go run plant_catalog.go --add "Фикус" --type "дерево" --frequency 7`

4. **Rust**  
   Сборка: `cargo build --release`  
   Запуск: `cargo run -- --add "Фикус" --type "дерево" --frequency 7`

5. **Java**  
   Сборка: `javac -cp gson.jar PlantCatalog.java`  
   Запуск: `java -cp .;gson.jar PlantCatalog --add "Фикус" --type "дерево" --frequency 7`

6. **C# (.NET Core)**  
   Запуск: `dotnet run -- --add "Фикус" --type "дерево" --frequency 7`

7. **C++ (Linux)**  
   Сборка: `g++ -std=c++11 -o plant_catalog plant_catalog.cpp -ljsoncpp`  
   Запуск: `./plant_catalog --add "Фикус" --type "дерево" --frequency 7`

8. **Kotlin (JVM)**  
   Сборка: `kotlinc -cp gson.jar PlantCatalog.kt`  
   Запуск: `kotlin -cp .;gson.jar PlantCatalogKt --add "Фикус" --type "дерево" --frequency 7`

## Использование

Общие аргументы командной строки (везде, где поддерживается):

- `--add <название>` – добавить растение (требует `--type`, `--frequency`, опционально `--last`).
- `--type <тип>` – тип растения (например, "дерево", "цветок", "суккулент").
- `--frequency <дни>` – частота полива в днях.
- `--last <YYYY-MM-DD>` – дата последнего полива (по умолчанию сегодня).
- `--list` – показать все растения.
- `--filter <тип>` – показать растения только указанного типа.
- `--water <название>` – отметить растение политым (обновить дату).
- `--remove <название>` – удалить растение по названию.
- `--export-json <файл>` – экспорт в JSON.
- `--export-csv <файл>` – экспорт в CSV.
- `--export-txt <файл>` – экспорт в TXT.

Пример (Python):
```bash
python plant_catalog.py --add "Монстера" --type "лиана" --frequency 5 --last 2026-08-18
python plant_catalog.py --list
python plant_catalog.py --water "Монстера"
python plant_catalog.py --export-json catalog.json
Структура репозитория
text
/
├── README.md
├── plant_catalog.py
├── plant_catalog.js
├── plant_catalog.go
├── plant_catalog.rs
├── PlantCatalog.java
├── PlantCatalog.cs
├── plant_catalog.cpp
└── PlantCatalog.kt
Лицензия
MIT
