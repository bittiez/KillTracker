package US.bittiez.KillTracker;

import US.bittiez.KillTracker.Config.Configurator;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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

import static US.bittiez.KillTracker.STATIC.*;

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
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player)
        {
            Player player = (Player) sender;
            String playerUUID = player.getUniqueId().toString();
            if(args[0].equalsIgnoreCase("stats")){
                if(stats.contains(playerUUID)) {
                    stats.getConfigurationSection(playerUUID).getKeys(false).forEach(key -> {
                        if(key.equals(MONSTER_KILLS)) {
                            String tmkMessage = config.config.getString("total_monster_kills")
                                .replace("[TOTAL_MOBS]", String.valueOf(stats.getInt(COMBINE_PATH(playerUUID, MONSTER_KILLS))));
                            tmkMessage = ChatColor.translateAlternateColorCodes('&', tmkMessage);
                            player.sendMessage(tmkMessage);
                        } else if(key.equals(PLAYER_KILLS)){
                            String pkMessage = config.config.getString("player_kill_total")
                                    .replace("[KILLS]", String.valueOf(stats.getInt(COMBINE_PATH(playerUUID, PLAYER_KILLS))));
                            pkMessage = ChatColor.translateAlternateColorCodes('&', pkMessage);
                            player.sendMessage(pkMessage);
                        } else if(key.equals(PLAYER_DEATHS)) {
                            String pdMessage = config.config.getString("player_death_total")
                                    .replace("[DEATHS]", String.valueOf(stats.getInt(COMBINE_PATH(playerUUID, PLAYER_DEATHS))));
                            pdMessage = ChatColor.translateAlternateColorCodes('&', pdMessage);
                            player.sendMessage(pdMessage);
                        } else if(!key.equals(PLAYER_NAME)) {
                            String tmkMessage = config.config.getString("total_mob_kills")
                                .replace("[THIS_MOB_AMT]", String.valueOf(stats.getInt(COMBINE_PATH(playerUUID, key, MONSTER_KILLS))))
                                .replace("[THIS_MOB]", key);
                            tmkMessage = ChatColor.translateAlternateColorCodes('&', tmkMessage);
                            player.sendMessage(tmkMessage);
                        }
                    });
                }
            }
            return true;
        } else {
            sender.sendMessage("This command is only available to players.");
            return true;
        }
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
                String killerUUID = killer.getUniqueId().toString();
                if (stats.contains(COMBINE_PATH(killerUUID, MONSTER_KILLS))) {
                    int kills = stats.getInt(COMBINE_PATH(killerUUID, MONSTER_KILLS));
                    stats.set(COMBINE_PATH(killerUUID, MONSTER_KILLS), kills + 1);
                } else {
                    stats.set(COMBINE_PATH(killerUUID, MONSTER_KILLS), 1);
                    stats.set(COMBINE_PATH(killerUUID, PLAYER_NAME), killer.getDisplayName());
                }
                String entityName = e.getEntity().getName();
                if (stats.contains(COMBINE_PATH(killerUUID, entityName, MONSTER_KILLS))) {
                    int kills = stats.getInt(COMBINE_PATH(killerUUID, entityName, MONSTER_KILLS));
                    stats.set(COMBINE_PATH(killerUUID, entityName, MONSTER_KILLS), kills + 1);
                } else {
                    stats.set(COMBINE_PATH(killerUUID, entityName, MONSTER_KILLS), 1);
                    stats.set(COMBINE_PATH(killerUUID, PLAYER_NAME), killer.getDisplayName());
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
        String killerUUID = killer.getUniqueId().toString();
        if(stats.contains(COMBINE_PATH(killerUUID, PLAYER_KILLS))){
            int kills = stats.getInt(COMBINE_PATH(killerUUID, PLAYER_KILLS));
            stats.set(COMBINE_PATH(killerUUID, PLAYER_KILLS), kills + 1);
        } else {
            stats.set(COMBINE_PATH(killerUUID, PLAYER_KILLS), 1);
            stats.set(COMBINE_PATH(killerUUID, PLAYER_NAME), killer.getDisplayName());
        }
        String msg = config.config.getString("player_kill");
        msg = msg.replace("[KILLS]", stats.getInt(killer.getUniqueId() + ".kills") + "")
                .replace("[DEATHS]", stats.getInt(killer.getUniqueId() + ".deaths") + "");
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        killer.sendMessage(msg);
    }

    private void punishVictim(Player victim){
        String victimUUID = victim.getUniqueId().toString();
        if(stats.contains(COMBINE_PATH(victimUUID, PLAYER_DEATHS))){
            int deaths = stats.getInt(COMBINE_PATH(victimUUID, PLAYER_DEATHS));
            stats.set(COMBINE_PATH(victimUUID, PLAYER_DEATHS), deaths + 1);
        } else {
            stats.set(COMBINE_PATH(victimUUID, PLAYER_DEATHS), 1);
            stats.set(COMBINE_PATH(victimUUID, PLAYER_NAME), victim.getDisplayName());
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
