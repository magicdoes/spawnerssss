package com.magicsmp.magicspawners.config;

import com.magicsmp.magicspawners.MagicSpawners;
import com.magicsmp.magicspawners.model.DropDefinition;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.util.*;

public final class EntityConfig {
    private final MagicSpawners plugin;
    private final Map<EntityType,List<DropDefinition>> drops = new EnumMap<>(EntityType.class);
    private final Map<EntityType,Map<Material,Double>> prices = new EnumMap<>(EntityType.class);
    public EntityConfig(MagicSpawners plugin) { this.plugin = plugin; reload(); }
    public void reload() {
        drops.clear(); prices.clear();
        File dir = new File(plugin.getDataFolder(), "spawners");
        File[] files = dir.listFiles((d,n)->n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            String key = f.getName().substring(0,f.getName().length()-4).toUpperCase(Locale.ROOT);
            EntityType type;
            try { type = EntityType.valueOf(key); } catch (IllegalArgumentException ex) { continue; }
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            List<DropDefinition> list = new ArrayList<>();
            ConfigurationSection items = y.getConfigurationSection("items");
            if (items != null) for (String id : items.getKeys(false)) {
                Material m = Material.matchMaterial(items.getString(id+".material", ""));
                if (m != null) list.add(new DropDefinition(m, Math.max(0.01, items.getDouble(id+".weight",1))));
            }
            Map<Material,Double> p = new EnumMap<>(Material.class);
            ConfigurationSection pc = y.getConfigurationSection("prices");
            if (pc != null) for (String mat : pc.getKeys(false)) {
                Material m = Material.matchMaterial(mat);
                if (m != null) p.put(m, pc.getDouble(mat));
            }
            drops.put(type, List.copyOf(list)); prices.put(type, Map.copyOf(p));
        }
    }
    public Set<EntityType> supported() { return Collections.unmodifiableSet(drops.keySet()); }
    public List<DropDefinition> drops(EntityType type) { return drops.getOrDefault(type, List.of()); }
    public double price(EntityType type, Material material) { return prices.getOrDefault(type, Map.of()).getOrDefault(material, 0.0); }
}
