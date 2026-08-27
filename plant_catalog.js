#!/usr/bin/env node
// plant_catalog.js
const { program } = require('commander');
const fs = require('fs');
const chalk = require('chalk');

const DATA_FILE = 'plants.json';

class PlantCatalog {
    constructor() {
        this.plants = [];
        this.load();
    }

    load() {
        try {
            if (fs.existsSync(DATA_FILE)) {
                this.plants = JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
            }
        } catch (e) {
            this.plants = [];
        }
    }

    save() {
        fs.writeFileSync(DATA_FILE, JSON.stringify(this.plants, null, 2));
    }

    addPlant(name, type, frequency, lastWatered) {
        if (!lastWatered) {
            lastWatered = new Date().toISOString().split('T')[0];
        }
        this.plants.push({ name, type, frequency, last_watered: lastWatered });
        this.save();
        console.log(chalk.green(`🌱 Растение '${name}' добавлено.`));
    }

    waterPlant(name) {
        const plant = this.plants.find(p => p.name.toLowerCase() === name.toLowerCase());
        if (!plant) {
            console.log(chalk.red(`❌ Растение '${name}' не найдено.`));
            return;
        }
        plant.last_watered = new Date().toISOString().split('T')[0];
        this.save();
        console.log(chalk.green(`💧 Растение '${name}' полито сегодня.`));
    }

    removePlant(name) {
        const idx = this.plants.findIndex(p => p.name.toLowerCase() === name.toLowerCase());
        if (idx === -1) {
            console.log(chalk.red(`❌ Растение '${name}' не найдено.`));
            return;
        }
        this.plants.splice(idx, 1);
        this.save();
        console.log(chalk.yellow(`🗑️ Растение '${name}' удалено.`));
    }

    getStatus(plant) {
        const last = new Date(plant.last_watered);
        const today = new Date();
        const daysSince = Math.floor((today - last) / (1000 * 60 * 60 * 24));
        const freq = plant.frequency;
        if (daysSince <= freq) return { status: '✅', color: chalk.green };
        if (daysSince <= freq * 1.5) return { status: '⚠️', color: chalk.yellow };
        return { status: '🚨', color: chalk.red };
    }

    listPlants(filterType) {
        let plants = this.plants;
        if (filterType) {
            plants = plants.filter(p => p.type.toLowerCase() === filterType.toLowerCase());
            if (plants.length === 0) {
                console.log(chalk.yellow(`❌ Растения типа '${filterType}' не найдены.`));
                return;
            }
        }
        if (plants.length === 0) {
            console.log(chalk.yellow('📭 Каталог пуст.'));
            return;
        }
        console.log(chalk.cyan('🌿 Каталог растений:'));
        for (const plant of plants) {
            const { status, color } = this.getStatus(plant);
            const last = new Date(plant.last_watered);
            const next = new Date(last);
            next.setDate(next.getDate() + plant.frequency);
            const nextStr = next.toISOString().split('T')[0];
            console.log(`${color(status)} ${plant.name} (${plant.type}) - полив каждые ${plant.frequency} дн., след. полив: ${nextStr}`);
        }
    }

    exportJson(filename) {
        fs.writeFileSync(filename, JSON.stringify(this.plants, null, 2));
        console.log(chalk.green(`📄 Экспортировано в ${filename} (JSON)`));
    }

    exportCsv(filename) {
        const header = 'name,type,frequency,last_watered\n';
        const rows = this.plants.map(p => `${p.name},${p.type},${p.frequency},${p.last_watered}`).join('\n');
        fs.writeFileSync(filename, header + rows);
        console.log(chalk.green(`📄 Экспортировано в ${filename} (CSV)`));
    }

    exportTxt(filename) {
        const lines = this.plants.map(p => `${p.name} | ${p.type} | ${p.frequency} дн. | последний полив: ${p.last_watered}`);
        fs.writeFileSync(filename, lines.join('\n'));
        console.log(chalk.green(`📄 Экспортировано в ${filename} (TXT)`));
    }
}

program
    .option('--add <name>', 'Добавить растение')
    .option('--type <type>', 'Тип растения')
    .option('--frequency <days>', 'Частота полива (дни)', parseInt)
    .option('--last <date>', 'Дата последнего полива (YYYY-MM-DD)')
    .option('--list', 'Показать все растения')
    .option('--filter <type>', 'Фильтр по типу')
    .option('--water <name>', 'Отметить растение политым')
    .option('--remove <name>', 'Удалить растение')
    .option('--export-json <file>', 'Экспорт в JSON')
    .option('--export-csv <file>', 'Экспорт в CSV')
    .option('--export-txt <file>', 'Экспорт в TXT')
    .parse(process.argv);

const opts = program.opts();
const catalog = new PlantCatalog();

if (opts.add) {
    if (!opts.type || !opts.frequency) {
        console.error(chalk.red('❌ Для добавления растения требуются --type и --frequency'));
        process.exit(1);
    }
    catalog.addPlant(opts.add, opts.type, opts.frequency, opts.last);
} else if (opts.list) {
    catalog.listPlants(opts.filter);
} else if (opts.water) {
    catalog.waterPlant(opts.water);
} else if (opts.remove) {
    catalog.removePlant(opts.remove);
} else if (opts.exportJson) {
    catalog.exportJson(opts.exportJson);
} else if (opts.exportCsv) {
    catalog.exportCsv(opts.exportCsv);
} else if (opts.exportTxt) {
    catalog.exportTxt(opts.exportTxt);
} else {
    program.help();
}
