// PlantCatalog.kt
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PlantCatalog {
    @Parameter(names = ["--add"])
    private var addName: String? = null

    @Parameter(names = ["--type"])
    private var plantType: String? = null

    @Parameter(names = ["--frequency"])
    private var frequency: Int? = null

    @Parameter(names = ["--last"])
    private var lastWatered: String? = null

    @Parameter(names = ["--list"])
    private var list: Boolean = false

    @Parameter(names = ["--filter"])
    private var filterType: String? = null

    @Parameter(names = ["--water"])
    private var waterName: String? = null

    @Parameter(names = ["--remove"])
    private var removeName: String? = null

    @Parameter(names = ["--export-json"])
    private var exportJson: String? = null

    @Parameter(names = ["--export-csv"])
    private var exportCsv: String? = null

    @Parameter(names = ["--export-txt"])
    private var exportTxt: String? = null

    data class Plant(val name: String, val type: String, val frequency: Int, val last_watered: String)

    private val dataFile = "plants.json"
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val type = object : TypeToken<MutableList<Plant>>() {}.type
    private val plants = mutableListOf<Plant>()

    private fun load() {
        val f = File(dataFile)
        if (!f.exists()) return
        try {
            val json = f.readText()
            val list = gson.fromJson<MutableList<Plant>>(json, type)
            plants.addAll(list)
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun save() {
        val json = gson.toJson(plants)
        File(dataFile).writeText(json)
    }

    private fun addPlant(name: String, type: String, freq: Int, last: String?) {
        val lastWatered = last ?: LocalDate.now().toString()
        plants.add(Plant(name, type, freq, lastWatered))
        save()
        println("\u001B[32m🌱 Растение '$name' добавлено.\u001B[0m")
    }

    private fun waterPlant(name: String) {
        val idx = plants.indexOfFirst { it.name.equals(name, ignoreCase = true) }
        if (idx == -1) {
            println("\u001B[31m❌ Растение '$name' не найдено.\u001B[0m")
            return
        }
        plants[idx] = plants[idx].copy(last_watered = LocalDate.now().toString())
        save()
        println("\u001B[32m💧 Растение '$name' полито сегодня.\u001B[0m")
    }

    private fun removePlant(name: String) {
        val idx = plants.indexOfFirst { it.name.equals(name, ignoreCase = true) }
        if (idx == -1) {
            println("\u001B[31m❌ Растение '$name' не найдено.\u001B[0m")
            return
        }
        plants.removeAt(idx)
        save()
        println("\u001B[33m🗑️ Растение '$name' удалено.\u001B[0m")
    }

    private fun getStatus(plant: Plant): Pair<String, String> {
        val last = LocalDate.parse(plant.last_watered, formatter)
        val daysSince = LocalDate.now().toEpochDay() - last.toEpochDay()
        return if (daysSince <= plant.frequency) {
            "✅" to "\u001B[32m"
        } else if (daysSince <= plant.frequency * 1.5) {
            "⚠️" to "\u001B[33m"
        } else {
            "🚨" to "\u001B[31m"
        }
    }

    private fun listPlants(filter: String?) {
        var list = plants
        if (filter != null) {
            list = plants.filter { it.type.equals(filter, ignoreCase = true) }.toMutableList()
            if (list.isEmpty()) {
                println("\u001B[33m❌ Растения типа '$filter' не найдены.\u001B[0m")
                return
            }
        }
        if (list.isEmpty()) {
            println("\u001B[33m📭 Каталог пуст.\u001B[0m")
            return
        }
        println("\u001B[36m🌿 Каталог растений:\u001B[0m")
        for (p in list) {
            val (status, color) = getStatus(p)
            val last = LocalDate.parse(p.last_watered, formatter)
            val next = last.plusDays(p.frequency.toLong())
            println("$color$status ${p.name} (${p.type}) - полив каждые ${p.frequency} дн., след. полив: $next\u001B[0m")
        }
    }

    private fun exportJson(filename: String) {
        val json = gson.toJson(plants)
        File(filename).writeText(json)
        println("\u001B[32m📄 Экспортировано в $filename (JSON)\u001B[0m")
    }

    private fun exportCsv(filename: String) {
        File(filename).printWriter().use { pw ->
            pw.println("name,type,frequency,last_watered")
            for (p in plants) {
                pw.println("${p.name},${p.type},${p.frequency},${p.last_watered}")
            }
        }
        println("\u001B[32m📄 Экспортировано в $filename (CSV)\u001B[0m")
    }

    private fun exportTxt(filename: String) {
        File(filename).printWriter().use { pw ->
            for (p in plants) {
                pw.println("${p.name} | ${p.type} | ${p.frequency} дн. | последний полив: ${p.last_watered}")
            }
        }
        println("\u001B[32m📄 Экспортировано в $filename (TXT)\u001B[0m")
    }

    fun run() {
        load()
        when {
            addName != null -> {
                if (plantType == null || frequency == null) {
                    System.err.println("❌ Для добавления растения требуются --type и --frequency")
                    System.exit(1)
                }
                addPlant(addName!!, plantType!!, frequency!!, lastWatered)
            }
            list -> listPlants(filterType)
            waterName != null -> waterPlant(waterName!!)
            removeName != null -> removePlant(removeName!!)
            exportJson != null -> exportJson(exportJson!!)
            exportCsv != null -> exportCsv(exportCsv!!)
            exportTxt != null -> exportTxt(exportTxt!!)
            else -> println("Используйте --help для справки.")
        }
    }
}

fun main(args: Array<String>) {
    val catalog = PlantCatalog()
    JCommander.newBuilder().addObject(catalog).build().parse(*args)
    catalog.run()
}
