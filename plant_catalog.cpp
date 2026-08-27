// plant_catalog.cpp
#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <map>
#include <ctime>
#include <iomanip>
#include <sstream>
#include <algorithm>
#include <json/json.h> // using jsoncpp

using namespace std;

const string DATA_FILE = "plants.json";

struct Plant {
    string name;
    string type;
    int frequency;
    string last_watered;
};

class Catalog {
private:
    vector<Plant> plants;

    string today() {
        time_t t = time(nullptr);
        tm* now = localtime(&t);
        char buf[11];
        strftime(buf, sizeof(buf), "%Y-%m-%d", now);
        return string(buf);
    }

    int days_since(const string& date) {
        tm tm1 = {};
        tm tm2 = {};
        strptime(date.c_str(), "%Y-%m-%d", &tm1);
        strptime(today().c_str(), "%Y-%m-%d", &tm2);
        time_t t1 = mktime(&tm1);
        time_t t2 = mktime(&tm2);
        return (t2 - t1) / 86400;
    }

    string next_water(const string& date, int freq) {
        tm tm1 = {};
        strptime(date.c_str(), "%Y-%m-%d", &tm1);
        tm1.tm_mday += freq;
        mktime(&tm1);
        char buf[11];
        strftime(buf, sizeof(buf), "%Y-%m-%d", &tm1);
        return string(buf);
    }

public:
    Catalog() { load(); }

    void load() {
        ifstream ifs(DATA_FILE);
        if (!ifs) return;
        Json::Value root;
        ifs >> root;
        for (const auto& item : root) {
            Plant p;
            p.name = item["name"].asString();
            p.type = item["type"].asString();
            p.frequency = item["frequency"].asInt();
            p.last_watered = item["last_watered"].asString();
            plants.push_back(p);
        }
    }

    void save() {
        Json::Value root(Json::arrayValue);
        for (const auto& p : plants) {
            Json::Value item;
            item["name"] = p.name;
            item["type"] = p.type;
            item["frequency"] = p.frequency;
            item["last_watered"] = p.last_watered;
            root.append(item);
        }
        ofstream ofs(DATA_FILE);
        ofs << root.toStyledString();
    }

    void addPlant(const string& name, const string& type, int frequency, const string& last) {
        Plant p;
        p.name = name;
        p.type = type;
        p.frequency = frequency;
        p.last_watered = last.empty() ? today() : last;
        plants.push_back(p);
        save();
        cout << "\033[32m🌱 Растение '" << name << "' добавлено.\033[0m" << endl;
    }

    void waterPlant(const string& name) {
        for (auto& p : plants) {
            if (p.name == name) {
                p.last_watered = today();
                save();
                cout << "\033[32m💧 Растение '" << name << "' полито сегодня.\033[0m" << endl;
                return;
            }
        }
        cout << "\033[31m❌ Растение '" << name << "' не найдено.\033[0m" << endl;
    }

    void removePlant(const string& name) {
        for (auto it = plants.begin(); it != plants.end(); ++it) {
            if (it->name == name) {
                plants.erase(it);
                save();
                cout << "\033[33m🗑️ Растение '" << name << "' удалено.\033[0m" << endl;
                return;
            }
        }
        cout << "\033[31m❌ Растение '" << name << "' не найдено.\033[0m" << endl;
    }

    void listPlants(const string& filter) {
        vector<Plant> list = plants;
        if (!filter.empty()) {
            list.clear();
            for (const auto& p : plants) {
                if (p.type == filter) list.push_back(p);
            }
            if (list.empty()) {
                cout << "\033[33m❌ Растения типа '" << filter << "' не найдены.\033[0m" << endl;
                return;
            }
        }
        if (list.empty()) {
            cout << "\033[33m📭 Каталог пуст.\033[0m" << endl;
            return;
        }
        cout << "\033[36m🌿 Каталог растений:\033[0m" << endl;
        for (const auto& p : list) {
            int days = days_since(p.last_watered);
            string status, color;
            if (days <= p.frequency) {
                status = "✅"; color = "\033[32m";
            } else if (days <= p.frequency * 1.5) {
                status = "⚠️"; color = "\033[33m";
            } else {
                status = "🚨"; color = "\033[31m";
            }
            string next = next_water(p.last_watered, p.frequency);
            cout << color << status << " " << p.name << " (" << p.type << ") - полив каждые " << p.frequency << " дн., след. полив: " << next << "\033[0m" << endl;
        }
    }

    void exportJSON(const string& filename) {
        Json::Value root(Json::arrayValue);
        for (const auto& p : plants) {
            Json::Value item;
            item["name"] = p.name;
            item["type"] = p.type;
            item["frequency"] = p.frequency;
            item["last_watered"] = p.last_watered;
            root.append(item);
        }
        ofstream ofs(filename);
        ofs << root.toStyledString();
        cout << "\033[32m📄 Экспортировано в " << filename << " (JSON)\033[0m" << endl;
    }

    void exportCSV(const string& filename) {
        ofstream ofs(filename);
        ofs << "name,type,frequency,last_watered\n";
        for (const auto& p : plants) {
            ofs << p.name << "," << p.type << "," << p.frequency << "," << p.last_watered << "\n";
        }
        cout << "\033[32m📄 Экспортировано в " << filename << " (CSV)\033[0m" << endl;
    }

    void exportTXT(const string& filename) {
        ofstream ofs(filename);
        for (const auto& p : plants) {
            ofs << p.name << " | " << p.type << " | " << p.frequency << " дн. | последний полив: " << p.last_watered << "\n";
        }
        cout << "\033[32m📄 Экспортировано в " << filename << " (TXT)\033[0m" << endl;
    }
};

int main(int argc, char* argv[]) {
    string add, type, last, filter, water, remove, json, csv, txt;
    int frequency = 0;
    bool list = false;

    for (int i = 1; i < argc; ++i) {
        string arg = argv[i];
        if (arg == "--add" && i+1 < argc) add = argv[++i];
        else if (arg == "--type" && i+1 < argc) type = argv[++i];
        else if (arg == "--frequency" && i+1 < argc) frequency = stoi(argv[++i]);
        else if (arg == "--last" && i+1 < argc) last = argv[++i];
        else if (arg == "--list") list = true;
        else if (arg == "--filter" && i+1 < argc) filter = argv[++i];
        else if (arg == "--water" && i+1 < argc) water = argv[++i];
        else if (arg == "--remove" && i+1 < argc) remove = argv[++i];
        else if (arg == "--export-json" && i+1 < argc) json = argv[++i];
        else if (arg == "--export-csv" && i+1 < argc) csv = argv[++i];
        else if (arg == "--export-txt" && i+1 < argc) txt = argv[++i];
    }

    Catalog catalog;
    if (!add.empty()) {
        if (type.empty() || frequency == 0) {
            cerr << "❌ Для добавления растения требуются --type и --frequency" << endl;
            return 1;
        }
        catalog.addPlant(add, type, frequency, last);
    } else if (list) {
        catalog.listPlants(filter);
    } else if (!water.empty()) {
        catalog.waterPlant(water);
    } else if (!remove.empty()) {
        catalog.removePlant(remove);
    } else if (!json.empty()) {
        catalog.exportJSON(json);
    } else if (!csv.empty()) {
        catalog.exportCSV(csv);
    } else if (!txt.empty()) {
        catalog.exportTXT(txt);
    } else {
        cout << "Используйте --help для справки." << endl;
    }
    return 0;
}
