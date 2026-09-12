package com.magicsmp.magicspawners.util;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class EconomyHook {
    private Economy economy;
    public boolean hook() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        economy = rsp == null ? null : rsp.getProvider(); return economy != null;
    }
    public boolean available(){return economy!=null;}
    public void deposit(Player p,double amount){if(economy!=null&&amount>0)economy.depositPlayer(p,amount);}
}
