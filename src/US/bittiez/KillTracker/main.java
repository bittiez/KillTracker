package US.bittiez.KillTracker;

import US.bittiez.KillTracker.Config.Configurator;
import US.bittiez.KillTracker.Updater.UpdateChecker;
import US.bittiez.KillTracker.Updater.UpdateStatus;
import org.bukkit.Bukkit;
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
    private Configurator config = new Configurator();

    @Override
    public void onEnable() {
        loadStats();
        log = getLogger();
        config.setConfig(this);
        config.saveDefaultConfig(this);
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);

        UpdateStatus updater = new UpdateChecker("https://github.com/bittiez/PvPLB/raw/master/src/plugin.yml", getDescription().getVersion()).getStatus();
        if(updater.HasUpdate)
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                @Override
                public void run() {
                    log.info("There is a new version of KillTracker available, please check https://github.com/bittiez/PvPLB/releases or https://www.spigotmc.org/resources/killtracker.36640/");
                }
            }, (20*60)*5);
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

                String msg = config.config.getString("mob_kill");
                msg = msg.replace("[THIS_MOB_AMT]", stats.getInt(killer.getUniqueId() + "." + e.getEntity().getName() + ".mkills") + "")
                        .replace("[THIS_MOB]", e.getEntity().getName())
                        .replace("[TOTAL_MOBS]", stats.getInt(killer.getUniqueId() + ".mkills") + "");
                msg = ChatColor.translateAlternateColorCodes('&', msg);
                killer.sendMessage(msg);
            }
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
        String msg = config.config.getString("player_kill");
        msg = msg.replace("[KILLS]", stats.getInt(killer.getUniqueId() + ".kills") + "")
                .replace("[DEATHS]", stats.getInt(killer.getUniqueId() + ".deaths") + "");
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        killer.sendMessage(msg);
    }

    private void punishVictim(Player victim){
        if(stats.contains(victim.getUniqueId().toString() + ".deaths")){
            int deaths = stats.getInt(victim.getUniqueId().toString() + ".deaths");
            stats.set(victim.getUniqueId().toString() + ".deaths", deaths + 1);
        } else {
            stats.set(victim.getUniqueId().toString() + ".deaths", 1);
            stats.set(victim.getUniqueId().toString() + ".name", victim.getDisplayName());
        }
        String msg = config.config.getString("player_death");
        msg = msg.replace("[KILLS]", stats.getInt(victim.getUniqueId() + ".kills") + "")
                .replace("[DEATHS]", stats.getInt(victim.getUniqueId() + ".deaths") + "");
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        victim.sendMessage(msg);
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
