// PlantCatalog.java
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PlantCatalog {
    private static final String DATA_FILE = "plants.json";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Parameter(names = "--add")
    private String addName;
    @Parameter(names = "--type")
    private String plantType;
    @Parameter(names = "--frequency")
    private Integer frequency;
    @Parameter(names = "--last")
    private String lastWatered;
    @Parameter(names = "--list")
    private boolean list;
    @Parameter(names = "--filter")
    private String filterType;
    @Parameter(names = "--water")
    private String waterName;
    @Parameter(names = "--remove")
    private String removeName;
    @Parameter(names = "--export-json")
    private String exportJson;
    @Parameter(names = "--export-csv")
    private String exportCsv;
    @Parameter(names = "--export-txt")
    private String exportTxt;

    static class Plant {
        String name;
        String type;
        int frequency;
        String last_watered;
    }

    private List<Plant> plants = new ArrayList<>();
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Type listType = new TypeToken<List<Plant>>(){}.getType();

    private void load() {
        try {
            String json = new String(Files.readAllBytes(Paths.get(DATA_FILE)));
            plants = gson.fromJson(json, listType);
        } catch (Exception e) {
            plants = new ArrayList<>();
        }
    }

    private void save() {
        try {
            String json = gson.toJson(plants);
            Files.write(Paths.get(DATA_FILE), json.getBytes());
        } catch (IOException e) {
            System.err.println("Ошибка сохранения: " + e.getMessage());
        }
    }

    private void addPlant(String name, String type, int freq, String last) {
        if (last == null) last = LocalDate.now().toString();
        Plant p = new Plant();
        p.name = name;
        p.type = type;
        p.frequency = freq;
        p.last_watered = last;
        plants.add(p);
        save();
        System.out.println("\u001B[32m🌱 Растение '" + name + "' добавлено.\u001B[0m");
    }

    private void waterPlant(String name) {
        for (Plant p : plants) {
            if (p.name.equalsIgnoreCase(name)) {
                p.last_watered = LocalDate.now().toString();
                save();
                System.out.println("\u001B[32m💧 Растение '" + name + "' полито сегодня.\u001B[0m");
                return;
            }
        }
        System.out.println("\u001B[31m❌ Растение '" + name + "' не найдено.\u001B[0m");
    }

    private void removePlant(String name) {
        for (int i = 0; i < plants.size(); i++) {
            if (plants.get(i).name.equalsIgnoreCase(name)) {
                plants.remove(i);
                save();
                System.out.println("\u001B[33m🗑️ Растение '" + name + "' удалено.\u001B[0m");
                return;
            }
        }
        System.out.println("\u001B[31m❌ Растение '" + name + "' не найдено.\u001B[0m");
    }

    private String[] getStatus(Plant p) {
        LocalDate last = LocalDate.parse(p.last_watered, FORMATTER);
        long daysSince = LocalDate.now().toEpochDay() - last.toEpochDay();
        if (daysSince <= p.frequency) return new String[]{"✅", "\u001B[32m"};
        if (daysSince <= p.frequency * 1.5) return new String[]{"⚠️", "\u001B[33m"};
        return new String[]{"🚨", "\u001B[31m"};
    }

    private void listPlants(String filter) {
        List<Plant> list = plants;
        if (filter != null) {
            list = new ArrayList<>();
            for (Plant p : plants) {
                if (p.type.equalsIgnoreCase(filter)) list.add(p);
            }
            if (list.isEmpty()) {
                System.out.println("\u001B[33m❌ Растения типа '" + filter + "' не найдены.\u001B[0m");
                return;
            }
        }
        if (list.isEmpty()) {
            System.out.println("\u001B[33m📭 Каталог пуст.\u001B[0m");
            return;
        }
        System.out.println("\u001B[36m🌿 Каталог растений:\u001B[0m");
        for (Plant p : list) {
            String[] status = getStatus(p);
            LocalDate last = LocalDate.parse(p.last_watered, FORMATTER);
            LocalDate next = last.plusDays(p.frequency);
            System.out.printf("%s%s %s (%s) - полив каждые %d дн., след. полив: %s\u001B[0m%n",
                    status[1], status[0], p.name, p.type, p.frequency, next.toString());
        }
    }

    private void exportJson(String filename) throws IOException {
        Files.write(Paths.get(filename), gson.toJson(plants).getBytes());
        System.out.println("\u001B[32m📄 Экспортировано в " + filename + " (JSON)\u001B[0m");
    }

    private void exportCsv(String filename) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("name,type,frequency,last_watered");
            for (Plant p : plants) {
                pw.printf("%s,%s,%d,%s%n", p.name, p.type, p.frequency, p.last_watered);
            }
        }
        System.out.println("\u001B[32m📄 Экспортировано в " + filename + " (CSV)\u001B[0m");
    }

    private void exportTxt(String filename) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            for (Plant p : plants) {
                pw.printf("%s | %s | %d дн. | последний полив: %s%n", p.name, p.type, p.frequency, p.last_watered);
            }
        }
        System.out.println("\u001B[32m📄 Экспортировано в " + filename + " (TXT)\u001B[0m");
    }

    public void run() throws Exception {
        load();
        if (addName != null) {
            if (plantType == null || frequency == null) {
                System.err.println("❌ Для добавления растения требуются --type и --frequency");
                System.exit(1);
            }
            addPlant(addName, plantType, frequency, lastWatered);
        } else if (list) {
            listPlants(filterType);
        } else if (waterName != null) {
            waterPlant(waterName);
        } else if (removeName != null) {
            removePlant(removeName);
        } else if (exportJson != null) {
            exportJson(exportJson);
        } else if (exportCsv != null) {
            exportCsv(exportCsv);
        } else if (exportTxt != null) {
            exportTxt(exportTxt);
        } else {
            System.out.println("Используйте --help для справки.");
        }
    }

    public static void main(String[] args) throws Exception {
        PlantCatalog catalog = new PlantCatalog();
        JCommander.newBuilder().addObject(catalog).build().parse(args);
        catalog.run();
    }
}
