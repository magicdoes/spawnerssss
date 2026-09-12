package com.magicsmp.magicspawners;

import com.magicsmp.magicspawners.command.MagicSpawnersCommand;
import com.magicsmp.magicspawners.config.EntityConfig;
import com.magicsmp.magicspawners.config.MessageConfig;
import com.magicsmp.magicspawners.data.SpawnerStorage;
import com.magicsmp.magicspawners.listener.GuiListener;
import com.magicsmp.magicspawners.listener.SpawnerListener;
import com.magicsmp.magicspawners.manager.SpawnerManager;
import com.magicsmp.magicspawners.task.GenerationTask;
import com.magicsmp.magicspawners.util.EconomyHook;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;

public final class MagicSpawners extends JavaPlugin {
    private static MagicSpawners instance;
    private MessageConfig messages; private EntityConfig entities; private SpawnerStorage storage; private SpawnerManager manager; private EconomyHook economy; private BukkitTask generationTask,saveTask;
    public static MagicSpawners get(){return instance;}
    @Override public void onEnable(){
        instance=this;saveDefaultConfig();copy("messages.yml");copy("guis/spawnergui.yml");copy("guis/spawnerpanel.yml");copy("guis/confirmsell.yml");
        String[] mobs={"iron_golem","cow","squid","turtle","pig","skeleton","piglin","blaze","zombie","sheep","phantom","rabbit","magma_cube","spider","guardian","chicken","sniffer","glow_squid","slime","creeper","enderman","armadillo"};for(String mob:mobs)copy("spawners/"+mob+".yml");
        messages=new MessageConfig(this);entities=new EntityConfig(this);storage=new SpawnerStorage(this);storage.load();manager=new SpawnerManager(this);economy=new EconomyHook();economy.hook();
        getServer().getPluginManager().registerEvents(new SpawnerListener(this),this);getServer().getPluginManager().registerEvents(new GuiListener(this),this);
        MagicSpawnersCommand exec=new MagicSpawnersCommand(this);PluginCommand cmd=getCommand("magicspawners");if(cmd!=null){cmd.setExecutor(exec);cmd.setTabCompleter(exec);}startTasks();getLogger().info("MagicSpawners enabled. No license system is used.");
    }
    @Override public void onDisable(){if(storage!=null)storage.save(false);if(generationTask!=null)generationTask.cancel();if(saveTask!=null)saveTask.cancel();instance=null;}
    public void reloadPlugin(){reloadConfig();messages.reload();entities.reload();startTasks();}
    private void startTasks(){if(generationTask!=null)generationTask.cancel();if(saveTask!=null)saveTask.cancel();long interval=Math.max(20,getConfig().getLong("generation.interval-ticks",200));generationTask=getServer().getScheduler().runTaskTimer(this,new GenerationTask(this),interval,interval);long save=Math.max(20,getConfig().getLong("performance.save-interval-seconds",300)*20L);boolean asyncSave=getConfig().getBoolean("performance.async-save",true);saveTask=getServer().getScheduler().runTaskTimer(this,()->storage.saveIfDirty(asyncSave),save,save);}
    private void copy(String path){File out=new File(getDataFolder(),path);if(out.exists())return;out.getParentFile().mkdirs();saveResource(path,false);}
    public MessageConfig messages(){return messages;}public EntityConfig entities(){return entities;}public SpawnerStorage storage(){return storage;}public SpawnerManager manager(){return manager;}public EconomyHook economy(){return economy;}
}
