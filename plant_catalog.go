// plant_catalog.go
package main

import (
	"encoding/csv"
	"encoding/json"
	"flag"
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"
)

const dataFile = "plants.json"

type Plant struct {
	Name        string `json:"name"`
	Type        string `json:"type"`
	Frequency   int    `json:"frequency"`
	LastWatered string `json:"last_watered"`
}

type Catalog struct {
	Plants []Plant `json:"plants"`
}

func (c *Catalog) load() {
	data, err := os.ReadFile(dataFile)
	if err != nil {
		c.Plants = []Plant{}
		return
	}
	if err := json.Unmarshal(data, c); err != nil {
		c.Plants = []Plant{}
	}
}

func (c *Catalog) save() {
	data, _ := json.MarshalIndent(c, "", "  ")
	os.WriteFile(dataFile, data, 0644)
}

func (c *Catalog) addPlant(name, plantType string, frequency int, lastWatered string) {
	if lastWatered == "" {
		lastWatered = time.Now().Format("2006-01-02")
	}
	c.Plants = append(c.Plants, Plant{Name: name, Type: plantType, Frequency: frequency, LastWatered: lastWatered})
	c.save()
	fmt.Printf("\033[32m🌱 Растение '%s' добавлено.\033[0m\n", name)
}

func (c *Catalog) waterPlant(name string) {
	for i := range c.Plants {
		if strings.EqualFold(c.Plants[i].Name, name) {
			c.Plants[i].LastWatered = time.Now().Format("2006-01-02")
			c.save()
			fmt.Printf("\033[32m💧 Растение '%s' полито сегодня.\033[0m\n", name)
			return
		}
	}
	fmt.Printf("\033[31m❌ Растение '%s' не найдено.\033[0m\n", name)
}

func (c *Catalog) removePlant(name string) {
	for i := range c.Plants {
		if strings.EqualFold(c.Plants[i].Name, name) {
			c.Plants = append(c.Plants[:i], c.Plants[i+1:]...)
			c.save()
			fmt.Printf("\033[33m🗑️ Растение '%s' удалено.\033[0m\n", name)
			return
		}
	}
	fmt.Printf("\033[31m❌ Растение '%s' не найдено.\033[0m\n", name)
}

func (c *Catalog) getStatus(p Plant) (string, string) {
	last, _ := time.Parse("2006-01-02", p.LastWatered)
	daysSince := int(time.Now().Sub(last).Hours() / 24)
	if daysSince <= p.Frequency {
		return "✅", "\033[32m"
	} else if daysSince <= p.Frequency*3/2 {
		return "⚠️", "\033[33m"
	}
	return "🚨", "\033[31m"
}

func (c *Catalog) listPlants(filterType string) {
	plants := c.Plants
	if filterType != "" {
		var filtered []Plant
		for _, p := range plants {
			if strings.EqualFold(p.Type, filterType) {
				filtered = append(filtered, p)
			}
		}
		if len(filtered) == 0 {
			fmt.Printf("\033[33m❌ Растения типа '%s' не найдены.\033[0m\n", filterType)
			return
		}
		plants = filtered
	}
	if len(plants) == 0 {
		fmt.Println("\033[33m📭 Каталог пуст.\033[0m")
		return
	}
	fmt.Println("\033[36m🌿 Каталог растений:\033[0m")
	for _, p := range plants {
		status, color := c.getStatus(p)
		last, _ := time.Parse("2006-01-02", p.LastWatered)
		next := last.AddDate(0, 0, p.Frequency)
		fmt.Printf("%s%s %s (%s) - полив каждые %d дн., след. полив: %s\033[0m\n",
			color, status, p.Name, p.Type, p.Frequency, next.Format("2006-01-02"))
	}
}

func (c *Catalog) exportJSON(filename string) {
	data, _ := json.MarshalIndent(c, "", "  ")
	os.WriteFile(filename, data, 0644)
	fmt.Printf("\033[32m📄 Экспортировано в %s (JSON)\033[0m\n", filename)
}

func (c *Catalog) exportCSV(filename string) {
	f, _ := os.Create(filename)
	defer f.Close()
	w := csv.NewWriter(f)
	defer w.Flush()
	w.Write([]string{"name", "type", "frequency", "last_watered"})
	for _, p := range c.Plants {
		w.Write([]string{p.Name, p.Type, strconv.Itoa(p.Frequency), p.LastWatered})
	}
	fmt.Printf("\033[32m📄 Экспортировано в %s (CSV)\033[0m\n", filename)
}

func (c *Catalog) exportTXT(filename string) {
	var lines []string
	for _, p := range c.Plants {
		lines = append(lines, fmt.Sprintf("%s | %s | %d дн. | последний полив: %s", p.Name, p.Type, p.Frequency, p.LastWatered))
	}
	os.WriteFile(filename, []byte(strings.Join(lines, "\n")), 0644)
	fmt.Printf("\033[32m📄 Экспортировано в %s (TXT)\033[0m\n", filename)
}

func main() {
	var (
		add        string
		plantType  string
		frequency  int
		last       string
		list       bool
		filter     string
		water      string
		remove     string
		exportJSON string
		exportCSV  string
		exportTXT  string
	)
	flag.StringVar(&add, "add", "", "Добавить растение")
	flag.StringVar(&plantType, "type", "", "Тип растения")
	flag.IntVar(&frequency, "frequency", 0, "Частота полива (дни)")
	flag.StringVar(&last, "last", "", "Дата последнего полива (YYYY-MM-DD)")
	flag.BoolVar(&list, "list", false, "Показать все растения")
	flag.StringVar(&filter, "filter", "", "Фильтр по типу")
	flag.StringVar(&water, "water", "", "Отметить растение политым")
	flag.StringVar(&remove, "remove", "", "Удалить растение")
	flag.StringVar(&exportJSON, "export-json", "", "Экспорт в JSON")
	flag.StringVar(&exportCSV, "export-csv", "", "Экспорт в CSV")
	flag.StringVar(&exportTXT, "export-txt", "", "Экспорт в TXT")
	flag.Parse()

	catalog := &Catalog{}
	catalog.load()

	if add != "" {
		if plantType == "" || frequency == 0 {
			fmt.Println("\033[31m❌ Для добавления растения требуются --type и --frequency\033[0m")
			os.Exit(1)
		}
		catalog.addPlant(add, plantType, frequency, last)
	} else if list {
		catalog.listPlants(filter)
	} else if water != "" {
		catalog.waterPlant(water)
	} else if remove != "" {
		catalog.removePlant(remove)
	} else if exportJSON != "" {
		catalog.exportJSON(exportJSON)
	} else if exportCSV != "" {
		catalog.exportCSV(exportCSV)
	} else if exportTXT != "" {
		catalog.exportTXT(exportTXT)
	} else {
		fmt.Println("Используйте --help для справки.")
	}
}
