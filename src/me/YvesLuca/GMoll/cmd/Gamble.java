package me.YvesLuca.GMoll.cmd;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.libs.jline.internal.Log;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.earth2me.essentials.IEssentials;
import com.earth2me.essentials.User;
import com.google.common.graph.ElementOrder.Type;

import me.YvesLuca.GMoll.Main;
import me.YvesLuca.GMoll.helper.CasinoPlayer;
import me.YvesLuca.GMoll.helper.CasinoSlot;
import net.ess3.api.MaxMoneyException;
import net.md_5.bungee.api.ChatColor;

public class Gamble implements CommandExecutor, Listener {
	
	private CasinoSlot[] casinoSlots;
	private int numSlots = 7;
	private ArrayList<CasinoPlayer> casinoPlayers;
	
	private Main plugin;
	private IEssentials ess;
	public Gamble(Main main, IEssentials ess) {
		this.plugin = main;
		this.ess = ess;
		
		
		casinoPlayers = new ArrayList<CasinoPlayer>();
		
		casinoSlots = new CasinoSlot[5];
		casinoSlots[0] = new CasinoSlot(new ItemStack(Material.EMERALD), 0.05, 1, 2);			//5% spin chance, 100% per slot (starting at 2)
		casinoSlots[1] = new CasinoSlot(new ItemStack(Material.DIAMOND), 0.20, 0.35, 2);		//20% spin chance, 35% per slot (starting at 2)
		casinoSlots[2] = new CasinoSlot(new ItemStack(Material.GOLD_INGOT), 0.25, 0.2, 3);		//25% spin chance, 20% per slot (starting at 3)
		casinoSlots[3] = new CasinoSlot(new ItemStack(Material.IRON_INGOT), 0.30, 0.05, 3);	//30% spin chance, 5% per slot (starting at 3)
		casinoSlots[4] = new CasinoSlot(new ItemStack(Material.COAL), 0.2, 0, 3);				//20% spin chance, 0% per slot
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(label.equalsIgnoreCase("gamble")) {
			if(sender instanceof Player) {
				Player player = (Player) sender;
				
				if(args == null) {
					player.sendMessage("Verwendung: /gamble <Einsatz>");
					return false;
				}
				
				if(args.length < 1) {
					player.sendMessage("Verwendung: /gamble <Einsatz>");
					return false;
				}
				
				int stake = 0;
				try {
					stake = Integer.parseInt(args[0]);
				} catch (NumberFormatException e) {
					player.sendMessage("Dieser Wetteinsatz ist ung�ltig!");
					stake = 0;
				}
				
				User user = ess.getUser(player);
				
				if(!user.canAfford(new BigDecimal(stake))) {
					player.sendMessage(ChatColor.BLUE + "Du bist nicht " + ChatColor.GOLD + "reich" +  ChatColor.RESET + " genung um zu zocken!");
					return(false);
				}
				
				if(this.getCasinoPlayer(player) == null) {
					casinoPlayers.add(new CasinoPlayer(player, stake));
				} else {
					this.setCPlayerStake(player, stake);
				}
				
				player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1);
				
				player.playSound(player.getLocation(), Sound.MUSIC_DISC_BLOCKS, 0.2f, 1);
				this.openGambleScreen(player, stake);
			}	
			return true;
		}
		return false;
	}
	
	@EventHandler
	public void onClick(InventoryClickEvent e) {
		
		if(!ChatColor.stripColor(e.getView().getTitle().toString()).equalsIgnoreCase("Feeling lucky?")) return;
		if(e.getCurrentItem() == null) return;
		
		Player player = (Player) e.getWhoClicked();
		
		if(e.getCurrentItem().getType().equals(Material.GREEN_WOOL)) {
			CasinoPlayer cPlayer = this.getCasinoPlayer(player);
			if(cPlayer != null) {
				e.setCancelled(true);
				this.openGambleScreen(player, cPlayer.getStake());
			} else {
				Log.info("Casino Player not found! This should not be possible!");
				e.setCancelled(true);
				player.closeInventory();
			}
			return;
		}
		
		if(e.getCurrentItem().getType().equals(Material.RED_WOOL)) {
			e.setCancelled(true);
			player.closeInventory();
			player.stopSound(Sound.MUSIC_DISC_BLOCKS);
			return;
		}
		
		
		e.setCancelled(true);
		player.closeInventory();
	

	}
	
	
	@EventHandler
	public void onClose(InventoryCloseEvent e) {
		
		if(!ChatColor.stripColor(e.getView().getTitle().toString()).equalsIgnoreCase("Feeling lucky?")) return;
		
		Player player = (Player) e.getPlayer();
		player.stopSound(Sound.MUSIC_DISC_BLOCKS);
		

	}
	

	private void openGambleScreen(Player player, double stake) {
		
		User user = ess.getUser(player);
		
		if(!user.canAfford(new BigDecimal(stake))) {
			player.sendMessage(ChatColor.BLUE + "Du bist nicht " + ChatColor.GOLD + "reich" +  ChatColor.RESET + " genung um zu zocken!");
			player.closeInventory();
			return;
		}
		
		this.deductMoney(player, stake);
		
		Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_AQUA + "Feeling lucky?");
		
		ItemStack[] items = new ItemStack[numSlots];
		
		ItemStack continueButton = new ItemStack(Material.GREEN_WOOL);
		ItemMeta conMeta = continueButton.getItemMeta();
		conMeta.setDisplayName("Nochmal!");
		continueButton.setItemMeta(conMeta);
		ItemStack exitButton = new ItemStack(Material.RED_WOOL);
		ItemMeta exitMeta = exitButton.getItemMeta();
		exitMeta.setDisplayName("STOPP!");
		exitButton.setItemMeta(exitMeta);
		
		inv.setItem(18, exitButton);
		inv.setItem(26, continueButton);
		
		int preDelay = 0;
		
		for(int i = 0; i < numSlots; i++) {
			int time = 10;
			time += preDelay;
			
			final int it = i;
			
			
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, () -> setInv(items, inv, it, player, stake), time);
			
			preDelay = time;
		}
		
	    
	    player.openInventory(inv);
	}
	
	private void setInv(ItemStack[] items, Inventory inv, int i, Player player, double stake) {
		Random rnd = new Random();
	    int rndNum = rnd.nextInt(99) + 1;
	    
	    int prob1 = (int) (casinoSlots[4].getProb() * 100);
	    int prob2 = (int) (casinoSlots[3].getProb() * 100) + prob1;	//60%
	    int prob3 = (int) (casinoSlots[2].getProb() * 100) + prob2;
	    int prob4 = (int) (casinoSlots[1].getProb() * 100) + prob3;
	    int prob5 = (int) (casinoSlots[0].getProb() * 100) + prob4;
	    
	    if(rndNum < prob1 && rndNum >= 0) {	//coal
	    	items[i] = casinoSlots[4].getDisplayItem();
	    } else if(rndNum >= prob1 && rndNum < prob2) {	//iron
	    	items[i] = casinoSlots[3].getDisplayItem();
	    } else if(rndNum >= prob2 && rndNum < prob3) {	//gold
	    	items[i] = casinoSlots[2].getDisplayItem();
	    } else if(rndNum >= prob3 && rndNum < prob4) {	//diamond
	    	items[i] = casinoSlots[1].getDisplayItem();
	    } else if(rndNum >= prob4 && rndNum < prob5) {	//emerald
	    	items[i] = casinoSlots[0].getDisplayItem();
	    } else {
	    	items[i] = casinoSlots[4].getDisplayItem();	//coal
	    	Log.info("Casino Probability Error!" + rndNum);
	    }
	    
	    inv.setItem(i+10, items[i]);
	    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 0.8f, 1);
	    
	    if(i == 6) {
	    	this.calcMoney(items, stake, player);
	    }
	}
	
	private void calcMoney(ItemStack[] items, double moneyAmount, Player player) {
		ArrayList<ItemStack[]> list = this.cutSlots(items);
	    this.generatePayout(player, list, moneyAmount);
	}
	
	private void deductMoney(Player player, double moneyAmount) {
		User user = ess.getUser(player);
		user.takeMoney(new BigDecimal(moneyAmount));
	}
	
	private ArrayList<ItemStack[]> cutSlots(ItemStack[] items) {
		ArrayList<ItemStack[]> list = new ArrayList<ItemStack[]>();
		
		
		ItemStack preItem = items[0];
		int consCount = 0;
		
		for(int i = 1; i < items.length; i++) {
			if(items[i].equals(preItem)) {
				consCount++;
			} else {
				ItemStack[] tmp = new ItemStack[consCount+1];
				System.arraycopy(items, i-consCount-1, tmp, 0, consCount+1);
				list.add(tmp);
				consCount = 0;
			}
			preItem = items[i];
		}
		
		if(consCount > 0) {	//still need to copy last elements
			ItemStack[] tmp = new ItemStack[consCount+1];;
			System.arraycopy(items, items.length-consCount-1, tmp, 0, consCount+1);
			list.add(tmp);
		}
		
		return(list);
		
	}
	
	private void generatePayout(Player player, ArrayList<ItemStack[]> list, double moneyAmount) {
		User user = ess.getUser(player);
		double payout = 0;
		
		for(int i = 0; i < list.size(); i++) {
			int count = list.get(i).length;
			CasinoSlot slot = this.getSlot(list.get(i)[0]);
			
			if(count >= slot.getPayoutSlotCount()) {
				double payRate = 0;
				
				if(slot.getDisplayItem().getType().equals(Material.COAL)) {
					payRate = 0;
				} else {
					payRate = slot.getPayoutRate() * count;
					payout += moneyAmount*(1+payRate);
				}
				
				
			}
		}
		
		double winPercent = payout/moneyAmount;
		
		if(winPercent < 1) {
			player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1, 1); 	//loose sound
		} else if(winPercent >= 1 && winPercent < 2) {
			player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1);
		} else if(winPercent >= 2) {
			player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1, 1);
		}
		
		try {
			user.giveMoney(new BigDecimal(payout));
		} catch (MaxMoneyException e) {
			e.printStackTrace();
		}
		
		player.sendMessage(ChatColor.DARK_AQUA + "[Casino]" + ChatColor.RESET + " Du hast " + ChatColor.GOLD +  payout + " gewonnen!");
		
		
		
		
	}
	
	private CasinoSlot getSlot(ItemStack item) {
		for(int i = 0; i < casinoSlots.length; i++) {
			if(casinoSlots[i].getDisplayItem().equals(item)) {
				return(casinoSlots[i]);
			}
		}
		return(null);
	}
	
	private CasinoPlayer getCasinoPlayer(Player player) {
		for(CasinoPlayer cPlayer : casinoPlayers) {
			if(cPlayer.getPlayer().getName().equalsIgnoreCase(player.getName())) {
				return(cPlayer);
			}
		}
		return(null);
	}
	
	private void setCPlayerStake(Player player, int stake) {
		for(CasinoPlayer cPlayer : casinoPlayers) {
			if(cPlayer.getPlayer().getName().equalsIgnoreCase(player.getName())) {
				cPlayer.setStake(stake);
			}
		}
	}
	
	
}
