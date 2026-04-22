package fr.sannoxx.ubuwool.manager;

import fr.sannoxx.ubuwool.UbuWool;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class MapConfig {

    private static final Map<String, UbuMap> maps = new LinkedHashMap<>();

    // FIX : volatile — selectedMap est lue depuis plusieurs threads (schedulers void check, capture)
    private static volatile String selectedMap = null;

    private static File configFile;

    public static class UbuMap {
        public String name;
        public String displayName;
        public String iconBlock;
        public boolean enabled;
        public double spawnRedX, spawnRedY, spawnRedZ;
        public double spawnBlueX, spawnBlueY, spawnBlueZ;
        public double banosX1, banosY1, banosZ1;
        public double banosX2, banosY2, banosZ2;
        public boolean hasBanos = false;
        public List<int[]> centerBlocks;
        public List<int[]> starPositions;
        public int woolCleanCenterX, woolCleanCenterY, woolCleanCenterZ;
        public int woolCleanRadius;
        public int voidY = -64;

        public UbuMap(String name) {
            this.name = name;
            this.enabled = true;
        }
    }

    public static void load(UbuWool plugin) {
        configFile = new File(plugin.getDataFolder(), "maps.yml");
        try {
            Files.createDirectories(plugin.getDataFolder().toPath());
            if (!configFile.exists()) {
                plugin.saveResource("maps.yml", false);
                if (!configFile.exists()) createDefault();
            }
            parse();
            plugin.getLogger().info("[MapConfig] " + maps.size() + " map(s) chargée(s).");
        } catch (Exception e) {
            plugin.getLogger().warning("[MapConfig] Erreur chargement maps.yml : " + e.getMessage());
        }
    }

    private static void createDefault() throws IOException {
        String content = """
# UbuWool — Configuration des maps
maps:
  TestMap:
    display_name: "TestMap"
    icon_block: "grass_block"
    enabled: true
    spawn_red: [30, 65, 30]
    spawn_blue: [-30, 65, -30]
    wool_clean_center: [0, 64, 0]
    wool_clean_radius: 50
    void_y: -64
    banos_pos_1: [101, 78, -11]
    banos_pos_2: [103, 78, -17]
    center_blocks:
      - [0, 64, 0]
      - [1, 64, 0]
      - [-1, 64, 0]
      - [0, 64, 1]
      - [0, 64, -1]
      - [1, 64, 1]
      - [1, 64, -1]
      - [-1, 64, 1]
      - [-1, 64, -1]
    star_positions:
      - [10, 65, 0]
      - [-10, 65, 0]
""";
        Files.writeString(configFile.toPath(), content);
    }

    private static void parse() {
        maps.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(configFile);

        ConfigurationSection mapsSection = yaml.getConfigurationSection("maps");
        if (mapsSection == null) {
            UbuWool.getInstance().getLogger().warning("[MapConfig] Aucune section 'maps' trouvée dans maps.yml !");
            return;
        }

        for (String mapName : mapsSection.getKeys(false)) {
            ConfigurationSection sec = mapsSection.getConfigurationSection(mapName);
            if (sec == null) continue;

            UbuMap map = new UbuMap(mapName);
            map.displayName     = sec.getString("display_name", mapName);
            map.iconBlock       = sec.getString("icon_block", "grass_block");
            map.enabled         = sec.getBoolean("enabled", true);
            map.voidY           = sec.getInt("void_y", -64);
            map.woolCleanRadius = sec.getInt("wool_clean_radius", 50);

            List<Double> spawnRed = getDoubleList(sec, "spawn_red");
            if (spawnRed.size() >= 3) {
                map.spawnRedX = spawnRed.get(0);
                map.spawnRedY = spawnRed.get(1);
                map.spawnRedZ = spawnRed.get(2);
            }

            List<Double> spawnBlue = getDoubleList(sec, "spawn_blue");
            if (spawnBlue.size() >= 3) {
                map.spawnBlueX = spawnBlue.get(0);
                map.spawnBlueY = spawnBlue.get(1);
                map.spawnBlueZ = spawnBlue.get(2);
            }

            List<Double> cleanCenter = getDoubleList(sec, "wool_clean_center");
            if (cleanCenter.size() >= 3) {
                map.woolCleanCenterX = cleanCenter.get(0).intValue();
                map.woolCleanCenterY = cleanCenter.get(1).intValue();
                map.woolCleanCenterZ = cleanCenter.get(2).intValue();
            }

            if (sec.contains("banos_pos_1") && sec.contains("banos_pos_2")) {
                List<Double> b1 = getDoubleList(sec, "banos_pos_1");
                List<Double> b2 = getDoubleList(sec, "banos_pos_2");
                if (b1.size() >= 3 && b2.size() >= 3) {
                    map.banosX1 = b1.get(0); map.banosY1 = b1.get(1); map.banosZ1 = b1.get(2);
                    map.banosX2 = b2.get(0); map.banosY2 = b2.get(1); map.banosZ2 = b2.get(2);
                    map.hasBanos = true;
                }
            }

            map.centerBlocks = new ArrayList<>();
            List<?> centerList = sec.getList("center_blocks", Collections.emptyList());
            for (Object entry : centerList) {
                int[] pos = parseIntVec3(entry);
                if (pos != null) map.centerBlocks.add(pos);
            }

            map.starPositions = new ArrayList<>();
            List<?> starList = sec.getList("star_positions", Collections.emptyList());
            for (Object entry : starList) {
                int[] pos = parseIntVec3(entry);
                if (pos != null) map.starPositions.add(pos);
            }

            maps.put(mapName, map);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Double> getDoubleList(ConfigurationSection sec, String key) {
        Object raw = sec.get(key);
        List<Double> result = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Number n) result.add(n.doubleValue());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static int[] parseIntVec3(Object entry) {
        if (entry instanceof List<?> list && list.size() >= 3) {
            try {
                int x = ((Number) list.get(0)).intValue();
                int y = ((Number) list.get(1)).intValue();
                int z = ((Number) list.get(2)).intValue();
                return new int[]{x, y, z};
            } catch (ClassCastException ignored) {}
        }
        return null;
    }

    public static List<UbuMap> getEnabledMaps() {
        return maps.values().stream().filter(m -> m.enabled).toList();
    }

    public static UbuMap getMap(String name)        { return maps.get(name); }
    public static void setSelectedMap(String name)  { selectedMap = name; }
    public static UbuMap getSelectedMap()           { return selectedMap == null ? null : maps.get(selectedMap); }

    public static void reload() {
        try {
            parse();
            UbuWool.getInstance().getLogger().info("[MapConfig] Maps rechargées : " + maps.size());
        } catch (Exception e) {
            UbuWool.getInstance().getLogger().warning("[MapConfig] Erreur reload : " + e.getMessage());
        }
    }
}