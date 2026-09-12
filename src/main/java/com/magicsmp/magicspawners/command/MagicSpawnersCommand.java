package com.magicsmp.magicspawners.command;

import com.magicsmp.magicspawners.MagicSpawners;
import com.magicsmp.magicspawners.gui.SpawnerPanelGui;
import com.magicsmp.magicspawners.model.SpawnerData;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;

public final class MagicSpawnersCommand implements CommandExecutor, TabCompleter {
    private final MagicSpawners plugin;
    public MagicSpawnersCommand(MagicSpawners plugin){this.plugin=plugin;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!s.hasPermission("magicspawners.admin")){s.sendMessage(plugin.messages().get("no-permission"));return true;}
        if(a.length==0||a[0].equalsIgnoreCase("help")){help(s);return true;}
        switch(a[0].toLowerCase(Locale.ROOT)){
            case "reload" -> {plugin.reloadPlugin();s.sendMessage(plugin.messages().get("reload-success"));}
            case "give" -> give(s,a);
            case "list" -> list(s);
            case "info" -> info(s);
            case "panel" -> {if(s instanceof Player p)SpawnerPanelGui.open(p);else s.sendMessage(plugin.messages().get("player-only"));}
            default -> s.sendMessage(plugin.messages().get("unknown-command"));
        } return true;
    }
    private void help(CommandSender s){s.sendMessage("§aMagicSpawners commands:");s.sendMessage("§f/ms give <player> <entity> [amount]");s.sendMessage("§f/ms panel");s.sendMessage("§f/ms list");s.sendMessage("§f/ms info");s.sendMessage("§f/ms reload");}
    private void give(CommandSender s,String[] a){
        if(a.length<3){s.sendMessage("§cUsage: /ms give <player> <entity> [amount]");return;}Player p=Bukkit.getPlayerExact(a[1]);if(p==null){s.sendMessage(plugin.messages().get("player-not-found"));return;}
        EntityType type;try{type=EntityType.valueOf(a[2].toUpperCase(Locale.ROOT));}catch(Exception e){s.sendMessage(plugin.messages().get("invalid-entity",Map.of("entity",a[2])));return;}
        if(!plugin.entities().supported().contains(type)){s.sendMessage(plugin.messages().get("invalid-entity",Map.of("entity",a[2])));return;}int amount=1;if(a.length>3)try{amount=Integer.parseInt(a[3]);}catch(Exception ignored){}
        if(amount<=0){s.sendMessage(plugin.messages().get("invalid-amount"));return;}for (var item : plugin.manager().createItems(type, amount)) { var rest=p.getInventory().addItem(item); rest.values().forEach(i->p.getWorld().dropItemNaturally(p.getLocation(),i)); }
        s.sendMessage(plugin.messages().get("give-success",Map.of("amount",String.valueOf(amount),"entity",plugin.manager().pretty(type),"player",p.getName())));p.sendMessage(plugin.messages().get("receive-spawner",Map.of("amount",String.valueOf(amount),"entity",plugin.manager().pretty(type))));
    }
    private void list(CommandSender s){Collection<SpawnerData> all=plugin.storage().all();if(all.isEmpty()){s.sendMessage(plugin.messages().get("list-empty"));return;}s.sendMessage(plugin.messages().get("list-header",Map.of("count",String.valueOf(all.size()))));for(SpawnerData d:all)s.sendMessage(plugin.messages().get("list-entry",Map.of("entity",plugin.manager().pretty(d.entityType()),"stack",String.valueOf(d.stackSize()),"x",String.valueOf(d.x()),"y",String.valueOf(d.y()),"z",String.valueOf(d.z()),"world",d.worldName())));}
    private void info(CommandSender s){s.sendMessage("§aMagicSpawners §fv"+plugin.getPluginMeta().getVersion());s.sendMessage("§7Registered spawners: §f"+plugin.storage().all().size());s.sendMessage("§7Vault: §f"+(plugin.economy().available()?"hooked":"not hooked"));}
    @Override public List<String> onTabComplete(CommandSender s,Command c,String l,String[] a){if(a.length==1)return match(a[0],List.of("give","panel","list","info","reload","help"));if(a.length==2&&a[0].equalsIgnoreCase("give"))return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n->n.toLowerCase().startsWith(a[1].toLowerCase())).toList();if(a.length==3&&a[0].equalsIgnoreCase("give"))return plugin.entities().supported().stream().map(EntityType::name).filter(n->n.toLowerCase().startsWith(a[2].toLowerCase())).sorted().toList();return List.of();}
    private List<String> match(String in,List<String> vals){String q=in.toLowerCase();return vals.stream().filter(v->v.startsWith(q)).toList();}
}
