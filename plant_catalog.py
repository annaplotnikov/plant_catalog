
#!/usr/bin/env python3
# plant_catalog.py
import argparse
import json
import csv
import sys
from datetime import datetime, timedelta
from colorama import init, Fore, Style

init(autoreset=True)

DATA_FILE = "plants.json"

class PlantCatalog:
    def __init__(self):
        self.plants = []
        self.load()

    def load(self):
        try:
            with open(DATA_FILE, 'r', encoding='utf-8') as f:
                self.plants = json.load(f)
        except (FileNotFoundError, json.JSONDecodeError):
            self.plants = []

    def save(self):
        with open(DATA_FILE, 'w', encoding='utf-8') as f:
            json.dump(self.plants, f, ensure_ascii=False, indent=2)

    def add_plant(self, name, plant_type, frequency, last_watered=None):
        if last_watered is None:
            last_watered = datetime.now().date().isoformat()
        plant = {
            "name": name,
            "type": plant_type,
            "frequency": frequency,
            "last_watered": last_watered
        }
        self.plants.append(plant)
        self.save()
        print(f"🌱 Растение '{name}' добавлено.")

    def water_plant(self, name):
        for plant in self.plants:
            if plant["name"].lower() == name.lower():
                plant["last_watered"] = datetime.now().date().isoformat()
                self.save()
                print(f"💧 Растение '{name}' полито сегодня.")
                return
        print(f"❌ Растение '{name}' не найдено.")

    def remove_plant(self, name):
        for i, plant in enumerate(self.plants):
            if plant["name"].lower() == name.lower():
                del self.plants[i]
                self.save()
                print(f"🗑️ Растение '{name}' удалено.")
                return
        print(f"❌ Растение '{name}' не найдено.")

    def get_status(self, plant):
        last = datetime.fromisoformat(plant["last_watered"]).date()
        today = datetime.now().date()
        days_since = (today - last).days
        freq = plant["frequency"]
        if days_since <= freq:
            return "✅", Fore.GREEN
        elif days_since <= freq * 1.5:
            return "⚠️", Fore.YELLOW
        else:
            return "🚨", Fore.RED

    def list_plants(self, filter_type=None):
        if not self.plants:
            print("📭 Каталог пуст.")
            return
        plants = self.plants
        if filter_type:
            plants = [p for p in self.plants if p["type"].lower() == filter_type.lower()]
            if not plants:
                print(f"❌ Растения типа '{filter_type}' не найдены.")
                return
        print("🌿 Каталог растений:")
        for plant in plants:
            status, color = self.get_status(plant)
            next_water = datetime.fromisoformat(plant["last_watered"]).date() + timedelta(days=plant["frequency"])
            print(f"{color}{status} {plant['name']} ({plant['type']}) - полив каждые {plant['frequency']} дн., след. полив: {next_water.isoformat()}{Style.RESET_ALL}")

    def export_json(self, filename):
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(self.plants, f, ensure_ascii=False, indent=2)
        print(f"📄 Экспортировано в {filename} (JSON)")

    def export_csv(self, filename):
        with open(filename, 'w', newline='', encoding='utf-8') as f:
            writer = csv.DictWriter(f, fieldnames=["name", "type", "frequency", "last_watered"])
            writer.writeheader()
            writer.writerows(self.plants)
        print(f"📄 Экспортировано в {filename} (CSV)")

    def export_txt(self, filename):
        with open(filename, 'w', encoding='utf-8') as f:
            for plant in self.plants:
                f.write(f"{plant['name']} | {plant['type']} | {plant['frequency']} дн. | последний полив: {plant['last_watered']}\n")
        print(f"📄 Экспортировано в {filename} (TXT)")

def main():
    parser = argparse.ArgumentParser(description="Каталог растений (полив)")
    parser.add_argument("--add", help="Добавить растение")
    parser.add_argument("--type", help="Тип растения")
    parser.add_argument("--frequency", type=int, help="Частота полива (дни)")
    parser.add_argument("--last", help="Дата последнего полива (YYYY-MM-DD)")
    parser.add_argument("--list", action="store_true", help="Показать все растения")
    parser.add_argument("--filter", help="Фильтр по типу")
    parser.add_argument("--water", help="Отметить растение политым")
    parser.add_argument("--remove", help="Удалить растение")
    parser.add_argument("--export-json", help="Экспорт в JSON")
    parser.add_argument("--export-csv", help="Экспорт в CSV")
    parser.add_argument("--export-txt", help="Экспорт в TXT")
    args = parser.parse_args()

    catalog = PlantCatalog()

    if args.add:
        if not args.type or not args.frequency:
            print("❌ Для добавления растения требуются --type и --frequency")
            sys.exit(1)
        catalog.add_plant(args.add, args.type, args.frequency, args.last)
    elif args.list:
        catalog.list_plants(args.filter)
    elif args.water:
        catalog.water_plant(args.water)
    elif args.remove:
        catalog.remove_plant(args.remove)
    elif args.export_json:
        catalog.export_json(args.export_json)
    elif args.export_csv:
        catalog.export_csv(args.export_csv)
    elif args.export_txt:
        catalog.export_txt(args.export_txt)
    else:
        parser.print_help()

if __name__ == "__main__":
    main()
