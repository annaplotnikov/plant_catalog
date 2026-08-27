// plant_catalog.rs
use chrono::{DateTime, Utc, Duration};
use clap::{App, Arg};
use serde::{Deserialize, Serialize};
use serde_json;
use std::fs;
use std::io::Write;
use colored::*;

const DATA_FILE: &str = "plants.json";

#[derive(Serialize, Deserialize, Clone)]
struct Plant {
    name: String,
    plant_type: String,
    frequency: i64,
    last_watered: String,
}

struct Catalog {
    plants: Vec<Plant>,
}

impl Catalog {
    fn new() -> Self {
        let mut c = Catalog { plants: Vec::new() };
        c.load();
        c
    }

    fn load(&mut self) {
        if let Ok(data) = fs::read_to_string(DATA_FILE) {
            if let Ok(plants) = serde_json::from_str(&data) {
                self.plants = plants;
                return;
            }
        }
        self.plants = Vec::new();
    }

    fn save(&self) {
        let json = serde_json::to_string_pretty(&self.plants).unwrap();
        fs::write(DATA_FILE, json).unwrap();
    }

    fn add_plant(&mut self, name: &str, plant_type: &str, frequency: i64, last: Option<&str>) {
        let last_watered = last.unwrap_or(&chrono::Utc::now().format("%Y-%m-%d").to_string()).to_string();
        self.plants.push(Plant {
            name: name.to_string(),
            plant_type: plant_type.to_string(),
            frequency,
            last_watered,
        });
        self.save();
        println!("{}", format!("🌱 Растение '{}' добавлено.", name).green());
    }

    fn water_plant(&mut self, name: &str) {
        for plant in &mut self.plants {
            if plant.name.to_lowercase() == name.to_lowercase() {
                plant.last_watered = chrono::Utc::now().format("%Y-%m-%d").to_string();
                self.save();
                println!("{}", format!("💧 Растение '{}' полито сегодня.", name).green());
                return;
            }
        }
        println!("{}", format!("❌ Растение '{}' не найдено.", name).red());
    }

    fn remove_plant(&mut self, name: &str) {
        let idx = self.plants.iter().position(|p| p.name.to_lowercase() == name.to_lowercase());
        if let Some(i) = idx {
            self.plants.remove(i);
            self.save();
            println!("{}", format!("🗑️ Растение '{}' удалено.", name).yellow());
        } else {
            println!("{}", format!("❌ Растение '{}' не найдено.", name).red());
        }
    }

    fn get_status(&self, plant: &Plant) -> (String, bool) {
        let last = chrono::NaiveDate::parse_from_str(&plant.last_watered, "%Y-%m-%d").unwrap();
        let today = chrono::Utc::now().date_naive();
        let days_since = (today - last).num_days();
        if days_since <= plant.frequency {
            ("✅".to_string(), true)
        } else if days_since <= plant.frequency * 3 / 2 {
            ("⚠️".to_string(), false)
        } else {
            ("🚨".to_string(), false)
        }
    }

    fn list_plants(&self, filter_type: Option<&str>) {
        let mut plants = self.plants.clone();
        if let Some(ft) = filter_type {
            plants.retain(|p| p.plant_type.to_lowercase() == ft.to_lowercase());
            if plants.is_empty() {
                println!("{}", format!("❌ Растения типа '{}' не найдены.", ft).yellow());
                return;
            }
        }
        if plants.is_empty() {
            println!("{}", "📭 Каталог пуст.".yellow());
            return;
        }
        println!("{}", "🌿 Каталог растений:".cyan());
        for plant in plants {
            let (status, is_ok) = self.get_status(&plant);
            let color = if is_ok { "green" } else { "yellow" };
            let color2 = if is_ok { "green" } else { "red" };
            let last = chrono::NaiveDate::parse_from_str(&plant.last_watered, "%Y-%m-%d").unwrap();
            let next = last + chrono::Duration::days(plant.frequency);
            println!("{} {} ({}) - полив каждые {} дн., след. полив: {}",
                status.color(color2),
                plant.name.color(color2),
                plant.plant_type.color(color2),
                plant.frequency.to_string().color(color2),
                next.format("%Y-%m-%d").to_string().color(color2)
            );
        }
    }

    fn export_json(&self, filename: &str) {
        let json = serde_json::to_string_pretty(&self.plants).unwrap();
        fs::write(filename, json).unwrap();
        println!("{}", format!("📄 Экспортировано в {} (JSON)", filename).green());
    }

    fn export_csv(&self, filename: &str) {
        let mut wtr = csv::Writer::from_path(filename).unwrap();
        wtr.write_record(&["name", "type", "frequency", "last_watered"]).unwrap();
        for p in &self.plants {
            wtr.write_record(&[&p.name, &p.plant_type, &p.frequency.to_string(), &p.last_watered]).unwrap();
        }
        wtr.flush().unwrap();
        println!("{}", format!("📄 Экспортировано в {} (CSV)", filename).green());
    }

    fn export_txt(&self, filename: &str) {
        let mut content = String::new();
        for p in &self.plants {
            content.push_str(&format!("{} | {} | {} дн. | последний полив: {}\n",
                p.name, p.plant_type, p.frequency, p.last_watered));
        }
        fs::write(filename, content).unwrap();
        println!("{}", format!("📄 Экспортировано в {} (TXT)", filename).green());
    }
}

fn main() {
    let matches = App::new("Plant Catalog")
        .arg(Arg::with_name("add").long("add").takes_value(true).help("Добавить растение"))
        .arg(Arg::with_name("type").long("type").takes_value(true).help("Тип растения"))
        .arg(Arg::with_name("frequency").long("frequency").takes_value(true).help("Частота полива (дни)"))
        .arg(Arg::with_name("last").long("last").takes_value(true).help("Дата последнего полива"))
        .arg(Arg::with_name("list").long("list").help("Показать все растения"))
        .arg(Arg::with_name("filter").long("filter").takes_value(true).help("Фильтр по типу"))
        .arg(Arg::with_name("water").long("water").takes_value(true).help("Отметить растение политым"))
        .arg(Arg::with_name("remove").long("remove").takes_value(true).help("Удалить растение"))
        .arg(Arg::with_name("export-json").long("export-json").takes_value(true).help("Экспорт в JSON"))
        .arg(Arg::with_name("export-csv").long("export-csv").takes_value(true).help("Экспорт в CSV"))
        .arg(Arg::with_name("export-txt").long("export-txt").takes_value(true).help("Экспорт в TXT"))
        .get_matches();

    let mut catalog = Catalog::new();

    if let Some(name) = matches.value_of("add") {
        let plant_type = matches.value_of("type").expect("--type required");
        let frequency: i64 = matches.value_of("frequency").expect("--frequency required").parse().unwrap();
        let last = matches.value_of("last");
        catalog.add_plant(name, plant_type, frequency, last);
    } else if matches.is_present("list") {
        catalog.list_plants(matches.value_of("filter"));
    } else if let Some(name) = matches.value_of("water") {
        catalog.water_plant(name);
    } else if let Some(name) = matches.value_of("remove") {
        catalog.remove_plant(name);
    } else if let Some(file) = matches.value_of("export-json") {
        catalog.export_json(file);
    } else if let Some(file) = matches.value_of("export-csv") {
        catalog.export_csv(file);
    } else if let Some(file) = matches.value_of("export-txt") {
        catalog.export_txt(file);
    } else {
        println!("Используйте --help для справки.");
    }
}
