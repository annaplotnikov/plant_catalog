// PlantCatalog.cs
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace PlantCatalog
{
    class Program
    {
        static void Main(string[] args)
        {
            var opts = ParseArgs(args);
            var catalog = new Catalog();
            if (opts.Add != null)
            {
                if (opts.Type == null || opts.Frequency == null)
                {
                    Console.Error.WriteLine("❌ Для добавления растения требуются --type и --frequency");
                    return;
                }
                catalog.AddPlant(opts.Add, opts.Type, opts.Frequency.Value, opts.Last);
            }
            else if (opts.List) catalog.ListPlants(opts.Filter);
            else if (opts.Water != null) catalog.WaterPlant(opts.Water);
            else if (opts.Remove != null) catalog.RemovePlant(opts.Remove);
            else if (opts.ExportJson != null) catalog.ExportJson(opts.ExportJson);
            else if (opts.ExportCsv != null) catalog.ExportCsv(opts.ExportCsv);
            else if (opts.ExportTxt != null) catalog.ExportTxt(opts.ExportTxt);
            else Console.WriteLine("Используйте --help для справки.");
        }

        static Options ParseArgs(string[] args)
        {
            var opts = new Options();
            for (int i = 0; i < args.Length; i++)
            {
                switch (args[i])
                {
                    case "--add": opts.Add = args[++i]; break;
                    case "--type": opts.Type = args[++i]; break;
                    case "--frequency": opts.Frequency = int.Parse(args[++i]); break;
                    case "--last": opts.Last = args[++i]; break;
                    case "--list": opts.List = true; break;
                    case "--filter": opts.Filter = args[++i]; break;
                    case "--water": opts.Water = args[++i]; break;
                    case "--remove": opts.Remove = args[++i]; break;
                    case "--export-json": opts.ExportJson = args[++i]; break;
                    case "--export-csv": opts.ExportCsv = args[++i]; break;
                    case "--export-txt": opts.ExportTxt = args[++i]; break;
                }
            }
            return opts;
        }

        class Options
        {
            public string Add { get; set; }
            public string Type { get; set; }
            public int? Frequency { get; set; }
            public string Last { get; set; }
            public bool List { get; set; }
            public string Filter { get; set; }
            public string Water { get; set; }
            public string Remove { get; set; }
            public string ExportJson { get; set; }
            public string ExportCsv { get; set; }
            public string ExportTxt { get; set; }
        }

        class Plant
        {
            public string Name { get; set; }
            public string Type { get; set; }
            public int Frequency { get; set; }
            public string LastWatered { get; set; }
        }

        class Catalog
        {
            private const string DataFile = "plants.json";
            private List<Plant> plants = new List<Plant>();

            public Catalog() => Load();

            private void Load()
            {
                try
                {
                    if (File.Exists(DataFile))
                    {
                        string json = File.ReadAllText(DataFile);
                        plants = JsonSerializer.Deserialize<List<Plant>>(json) ?? new List<Plant>();
                    }
                }
                catch { plants = new List<Plant>(); }
            }

            private void Save()
            {
                string json = JsonSerializer.Serialize(plants, new JsonSerializerOptions { WriteIndented = true });
                File.WriteAllText(DataFile, json);
            }

            public void AddPlant(string name, string type, int frequency, string last)
            {
                if (last == null) last = DateTime.UtcNow.ToString("yyyy-MM-dd");
                plants.Add(new Plant { Name = name, Type = type, Frequency = frequency, LastWatered = last });
                Save();
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"🌱 Растение '{name}' добавлено.");
                Console.ResetColor();
            }

            public void WaterPlant(string name)
            {
                var plant = plants.FirstOrDefault(p => p.Name.Equals(name, StringComparison.OrdinalIgnoreCase));
                if (plant == null)
                {
                    Console.ForegroundColor = ConsoleColor.Red;
                    Console.WriteLine($"❌ Растение '{name}' не найдено.");
                    Console.ResetColor();
                    return;
                }
                plant.LastWatered = DateTime.UtcNow.ToString("yyyy-MM-dd");
                Save();
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"💧 Растение '{name}' полито сегодня.");
                Console.ResetColor();
            }

            public void RemovePlant(string name)
            {
                var idx = plants.FindIndex(p => p.Name.Equals(name, StringComparison.OrdinalIgnoreCase));
                if (idx == -1)
                {
                    Console.ForegroundColor = ConsoleColor.Red;
                    Console.WriteLine($"❌ Растение '{name}' не найдено.");
                    Console.ResetColor();
                    return;
                }
                plants.RemoveAt(idx);
                Save();
                Console.ForegroundColor = ConsoleColor.Yellow;
                Console.WriteLine($"🗑️ Растение '{name}' удалено.");
                Console.ResetColor();
            }

            private (string status, ConsoleColor color) GetStatus(Plant p)
            {
                var last = DateTime.ParseExact(p.LastWatered, "yyyy-MM-dd", null);
                var daysSince = (DateTime.UtcNow - last).Days;
                if (daysSince <= p.Frequency) return ("✅", ConsoleColor.Green);
                if (daysSince <= p.Frequency * 1.5) return ("⚠️", ConsoleColor.Yellow);
                return ("🚨", ConsoleColor.Red);
            }

            public void ListPlants(string filter)
            {
                var list = plants;
                if (filter != null)
                {
                    list = plants.Where(p => p.Type.Equals(filter, StringComparison.OrdinalIgnoreCase)).ToList();
                    if (list.Count == 0)
                    {
                        Console.ForegroundColor = ConsoleColor.Yellow;
                        Console.WriteLine($"❌ Растения типа '{filter}' не найдены.");
                        Console.ResetColor();
                        return;
                    }
                }
                if (list.Count == 0)
                {
                    Console.ForegroundColor = ConsoleColor.Yellow;
                    Console.WriteLine("📭 Каталог пуст.");
                    Console.ResetColor();
                    return;
                }
                Console.ForegroundColor = ConsoleColor.Cyan;
                Console.WriteLine("🌿 Каталог растений:");
                Console.ResetColor();
                foreach (var p in list)
                {
                    var (status, color) = GetStatus(p);
                    var last = DateTime.ParseExact(p.LastWatered, "yyyy-MM-dd", null);
                    var next = last.AddDays(p.Frequency);
                    Console.ForegroundColor = color;
                    Console.WriteLine($"{status} {p.Name} ({p.Type}) - полив каждые {p.Frequency} дн., след. полив: {next:yyyy-MM-dd}");
                    Console.ResetColor();
                }
            }

            public void ExportJson(string filename)
            {
                string json = JsonSerializer.Serialize(plants, new JsonSerializerOptions { WriteIndented = true });
                File.WriteAllText(filename, json);
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"📄 Экспортировано в {filename} (JSON)");
                Console.ResetColor();
            }

            public void ExportCsv(string filename)
            {
                using var sw = new StreamWriter(filename);
                sw.WriteLine("name,type,frequency,last_watered");
                foreach (var p in plants)
                    sw.WriteLine($"{p.Name},{p.Type},{p.Frequency},{p.LastWatered}");
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"📄 Экспортировано в {filename} (CSV)");
                Console.ResetColor();
            }

            public void ExportTxt(string filename)
            {
                using var sw = new StreamWriter(filename);
                foreach (var p in plants)
                    sw.WriteLine($"{p.Name} | {p.Type} | {p.Frequency} дн. | последний полив: {p.LastWatered}");
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"📄 Экспортировано в {filename} (TXT)");
                Console.ResetColor();
            }
        }
    }
}
