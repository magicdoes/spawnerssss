package com.magicsmp.magicspawners.config;

import com.magicsmp.magicspawners.MagicSpawners;
import com.magicsmp.magicspawners.util.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;

public final class MessageConfig {
    private final MagicSpawners plugin;
    private YamlConfiguration cfg;
    public MessageConfig(MagicSpawners plugin) { this.plugin = plugin; reload(); }
    public void reload() { cfg = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml")); }
    public Component get(String key, Map<String,String> replacements) {
        String s = cfg.getString(key, key);
        for (var e : replacements.entrySet()) s = s.replace("%"+e.getKey()+"%", e.getValue());
        return ColorUtil.component(s);
    }
    public Component get(String key) { return get(key, Map.of()); }
}
