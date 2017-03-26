package US.bittiez.KillTracker;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class main extends JavaPlugin implements Listener{
    private static Logger log;
    private FileConfiguration stats;
    private String statFile = "stats.yml";

    @Override
    public void onEnable() {
        loadStats();
        log = getLogger();
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent e){
        saveStats();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e){
        if(e.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent nEvent = (EntityDamageByEntityEvent) e.getEntity().getLastDamageCause();
            Entity ekiller = nEvent.getDamager();
            if (ekiller instanceof Player && !(e.getEntity() instanceof Player)) {
                Player killer = (Player) ekiller;
                if (stats.contains(killer.getUniqueId().toString() + ".mkills")) {
                    int kills = stats.getInt(killer.getUniqueId().toString() + ".mkills");
                    stats.set(killer.getUniqueId().toString() + ".mkills", kills + 1);
                } else {
                    stats.set(killer.getUniqueId().toString() + ".mkills", 1);
                    stats.set(killer.getUniqueId().toString() + ".name", killer.getDisplayName());
                }

                if (stats.contains(killer.getUniqueId().toString() + "." + e.getEntity().getName() + ".mkills")) {
                    int kills = stats.getInt(killer.getUniqueId().toString() + "." + e.getEntity().getName() + ".mkills");
                    stats.set(killer.getUniqueId().toString() + "." + e.getEntity().getName() + ".mkills", kills + 1);
                } else {
                    stats.set(killer.getUniqueId().toString() + "." + e.getEntity().getName() + ".mkills", 1);
                    stats.set(killer.getUniqueId().toString() + ".name", killer.getDisplayName());
                }

                killer.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6You have killed&6 &5" +
                        stats.getInt(killer.getUniqueId() + "." + e.getEntity().getName() + ".mkills")
                        + " &5" + e.getEntity().getName()
                        + "s&6 and &5"
                        + stats.getInt(killer.getUniqueId() + ".mkills")
                        + "&6 total mobs."
                ));
            }
            //saveStats();
        }

    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if(killer != null){
            rewardKiller(killer);
            punishVictim(victim);
            //saveStats();
        }
    }

    private void rewardKiller(Player killer){
        if(stats.contains(killer.getUniqueId().toString() + ".kills")){
            int kills = stats.getInt(killer.getUniqueId().toString() + ".kills");
            stats.set(killer.getUniqueId().toString() + ".kills", kills + 1);
        } else {
            stats.set(killer.getUniqueId().toString() + ".kills", 1);
            stats.set(killer.getUniqueId().toString() + ".name", killer.getDisplayName());
        }
        killer.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6Your new &5Kill&6:Death is &5" +
                stats.getInt(killer.getUniqueId() + ".kills")
                + "&6:" +
                stats.getInt(killer.getUniqueId() + ".deaths")));
    }

    private void punishVictim(Player victim){
        if(stats.contains(victim.getUniqueId().toString() + ".deaths")){
            int deaths = stats.getInt(victim.getUniqueId().toString() + ".deaths");
            stats.set(victim.getUniqueId().toString() + ".deaths", deaths + 1);
        } else {
            stats.set(victim.getUniqueId().toString() + ".deaths", 1);
            stats.set(victim.getUniqueId().toString() + ".name", victim.getDisplayName());
        }
        victim.sendMessage(ChatColor.translateAlternateColorCodes('&',"&6Your new Kill:&5Death &6is " +
                stats.getInt(victim.getUniqueId() + ".kills")
                + ":&5" +
                stats.getInt(victim.getUniqueId() + ".deaths")));
    }

    private void saveStats(){
        try {
            stats.save(new File(this.getDataFolder(), this.statFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadStats(){
        File stats = new File(this.getDataFolder(), this.statFile);
        if (!stats.exists()) {
            try {
                this.getDataFolder().mkdirs();
                stats.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.stats = new YamlConfiguration();
        }
        if (stats.exists()) {
            this.stats = YamlConfiguration.loadConfiguration(stats);
        } else {
            getServer().getPluginManager().disablePlugin(this);
        }
    }
}
