package us.infinitydev.lrf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import net.md_5.bungee.api.ChatColor;

public class LRFlyCore extends JavaPlugin implements Listener{
	
	List<UUID> flightList = new ArrayList<UUID>();
	public WorldGuardPlugin getWorldGuard() {
		Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
		if(plugin == null || !(plugin instanceof WorldGuardPlugin)) {
			return null;
		}
		
		return (WorldGuardPlugin) plugin;
	}
	
	public RegionManager getRegionManager(World world) {
		return getWorldGuard().getRegionManager(world);
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		if(flightList.contains(e.getPlayer().getUniqueId())) {
			flightList.remove(e.getPlayer().getUniqueId());
		}
	}
	
	@EventHandler
	public void tryFlight(PlayerToggleFlightEvent e) {
		Player p = e.getPlayer();

		boolean canGo = false;
		
		for(ProtectedRegion r : getRegionManager(p.getWorld()).getApplicableRegions(p.getLocation())) {
			if(r.getId().equalsIgnoreCase("spawn")) {
				canGo = true;
			}
		}
		
		if(canGo == false) {
			
			if(flightList.contains(p.getUniqueId())) {
				flightList.remove(e.getPlayer().getUniqueId());
				e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.valueOf(getConfig().getString("FlyEndSound")), 0.6f, 1.0f);
				e.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("FlyEndMessage")));
				e.getPlayer().setFlying(false);
				e.setCancelled(true);
				return;
			}
			
			if(e.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
				return;
			}
			
			e.setCancelled(true);
			e.getPlayer().setAllowFlight(false);
			Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
				public void run() {
					e.getPlayer().setAllowFlight(true);
				}
			}, 1);
			return;
		}
		
		if(!(flightList.contains(e.getPlayer().getUniqueId()))) {
			flightList.add(e.getPlayer().getUniqueId());
			e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.valueOf(getConfig().getString("FlyStartSound")), 0.6f, 1.0f);
			e.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("FlyStartMessage")));
		}else {
			flightList.remove(e.getPlayer().getUniqueId());
			e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.valueOf(getConfig().getString("FlyEndSound")), 0.6f, 1.0f);
			e.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("FlyEndMessage")));
		}
		
		return;
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void playerMove(PlayerMoveEvent e) {
		for(UUID u : flightList) {
			if(Bukkit.getPlayer(u) == null) {
				flightList.remove(u);
			}
		}
		
		for(Player p : Bukkit.getOnlinePlayers()) {
			
			if(flightList.contains(p.getUniqueId())) {
				boolean inSpawn = false;
				
				for(ProtectedRegion r : getRegionManager(p.getWorld()).getApplicableRegions(p.getLocation())) {
					if(r.getId().equalsIgnoreCase("spawn")) {
						inSpawn = true;
					}
				}
				
				if(inSpawn == false) {
					e.getPlayer().setFlying(false);
					flightList.remove(p.getUniqueId());
					p.playSound(p.getLocation(), Sound.valueOf(getConfig().getString("FlyEndSound")), 0.6f, 1.0f);
					e.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("FlyEndMessage")));
				}
				
				if(inSpawn == true) {
					p.getWorld().playEffect(p.getLocation(), Effect.valueOf(getConfig().getString("FlyParticle")), 1);
				}
				
				return;
			}
		}
	}
	
	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, this);
		getConfig().options().copyDefaults(true);
		saveDefaultConfig();
		
		if(getWorldGuard() == null) {
			Bukkit.getLogger().log(Level.SEVERE, "WorldGuard is not found and is required for LRFly!");
		}
		
		if(getServer().getPluginManager().getPlugin("WorldEdit") == null) {
			Bukkit.getLogger().log(Level.SEVERE, "WorldEdit is not found and is required for LRFly!");
		}
		
		if(getConfig().getString("FlyStartSound") == null) {
			getConfig().set("FlyStartSound", "FIREWORK_LAUNCH");
			saveConfig();
		}
		
		if(getConfig().getString("FlyEndSound") == null) {
			getConfig().set("FlyEndSound", "FIREWORK_TWINKLE");
			saveConfig();
		}
		
		if(getConfig().getString("FlyParticle") == null) {
			getConfig().set("FlyParticle", "LARGE_SMOKE");
			saveConfig();
		}
		
		if(getConfig().getString("FlyStartMessage") == null) {
			getConfig().set("FlyStartMessage", "&aYour flight has been enabled!");
			saveConfig();
		}
		
		if(getConfig().getString("FlyEndMessage") == null) {
			getConfig().set("FlyEndMessage", "&cYour flight has been disabled!");
			saveConfig();
		}
		
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			@SuppressWarnings("deprecation")
			public void run() {
                for(Player p : Bukkit.getOnlinePlayers()) {
                	boolean inSpawn = false;
    				
    				for(ProtectedRegion r : getRegionManager(p.getWorld()).getApplicableRegions(p.getLocation())) {
    					if(r.getId().equalsIgnoreCase("spawn")) {
    						inSpawn = true;
    					}
    				}
    				
    				if(inSpawn != true && !(p.getGameMode().equals(GameMode.CREATIVE))) {
    					p.setAllowFlight(false);
    				}else {
    					p.setAllowFlight(true);
    				}
                }
			}
		}, 0, 5);
		
		return;
	}

}
