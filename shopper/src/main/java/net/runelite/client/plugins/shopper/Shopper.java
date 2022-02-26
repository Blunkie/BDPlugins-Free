package net.runelite.client.plugins.shopper;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.iutils.BankUtils;
import net.runelite.client.plugins.iutils.InventoryUtils;
import net.runelite.client.plugins.iutils.NPCUtils;
import net.runelite.client.plugins.iutils.ObjectUtils;
import net.runelite.client.plugins.oneclickutils.LegacyMenuEntry;
import net.runelite.client.plugins.oneclickutils.OneClickUtilsPlugin;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.http.api.worlds.WorldType;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.awt.*;
import java.util.*;
import java.util.List;

import static net.runelite.api.ItemID.*;


@Extension
@PluginDescriptor(
	name = "BD Shopper",
	description = "BD Shopper"
)

@Slf4j
@PluginDependency(OneClickUtilsPlugin.class)
public class Shopper extends Plugin
{
	@Inject
	private ShopperConfig config;
	@Inject
	private OneClickUtilsPlugin oneClickUtilsPlugin;
	@Inject
	private ConfigManager configManager;
	@Inject
	private Client client;
	@Inject
	private InventoryUtils inventory;
	@Inject
	private ObjectUtils objectUtils;
	@Inject
	private BankUtils bankUtils;
	@Inject
	private NPCUtils npcUtils;


	@Provides
	ShopperConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(ShopperConfig.class);
	}

	private Queue<LegacyMenuEntry> actionQueue = new LinkedList<LegacyMenuEntry>();
	private int globalTimeout = 0;
	String menuText = "BD Shopper";
	private Set<Integer> packs = Set.of(WATER_RUNE_PACK, EARTH_RUNE_PACK, FIRE_RUNE_PACK, AIR_RUNE_PACK);
	boolean shouldHop = false;



	@Override
	protected void startUp() {
		actionQueue.clear();
	}

	@Override
	protected void shutDown() {
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if(event.getGroup().equals(menuText)) {

		}
	}

	@Subscribe
	private void onClientTick(ClientTick event) {
		if(true){
			client.insertMenuItem(menuText,"",MenuAction.UNKNOWN.getId(),0,0,0,false);
			client.setTempMenuEntry(Arrays.stream(client.getMenuEntries()).filter(x->x.getOption().equals(menuText)).findFirst().orElse(null));
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		if (event.getMenuOption().contains(menuText)){
			handleClick(event);
			log.info(event.getMenuOption() + ", "
					+ event.getMenuTarget() + ", "
					+ event.getId() + ", "
					+ event.getMenuAction().name() + ", "
					+ event.getParam0() + ", "
					+ event.getParam1());
		}
	}

	private void handleClick(MenuOptionClicked event) {
		//Check if something is blocking
		if (globalTimeout > 0){
			//log.info("Timeout was " + globalTimeout);
			event.consume();
			return;
		}

		if(actionQueue.isEmpty()) {
			NPC npc = npcUtils.findNearestNpc(config.goldOre() ? 1560 : config.npcID());
			if (shouldHop) {
				oneClickUtilsPlugin.hop(false);
				shouldHop = false;
			} else if(inventory.isFull() && inventory.containsItem(packs)) {
				for (WidgetItem pack : inventory.getItems(packs)) {
					oneClickUtilsPlugin.sanitizeEnqueue(oneClickUtilsPlugin.clickItem(pack), actionQueue, "Couldn't open pack)");
				}
			} else if(inventory.isFull() && config.goldOre()){
				if(!bankUtils.isOpen()){
					oneClickUtilsPlugin.sanitizeEnqueue(this.openBFBank(), actionQueue, "Couldn't open bank");
				}else{
					oneClickUtilsPlugin.sanitizeEnqueue(oneClickUtilsPlugin.depositAllOfItem(GOLD_ORE), actionQueue, "Coudln't deposit ore into bank");
					oneClickUtilsPlugin.sanitizeEnqueue(oneClickUtilsPlugin.clickNPC(npc, oneClickUtilsPlugin.getNPCMenuActionsWithString(npc, "Trade")), actionQueue, "Coudlnt trade with npc");
				}
			} else if (npc != null && !oneClickUtilsPlugin.isShopOpen(client)){
				oneClickUtilsPlugin.sanitizeEnqueue(oneClickUtilsPlugin.clickNPC(npc, oneClickUtilsPlugin.getNPCMenuActionsWithString(npc, "Trade")), actionQueue, "Coudlnt trade with npc");
			} else if (oneClickUtilsPlugin.isShopOpen(client)){
				Collection<WidgetItem> items = oneClickUtilsPlugin.selectWidgetItems(oneClickUtilsPlugin.getShopItems(client), config.goldOre() ? Set.of(GOLD_ORE) : Set.of(config.itemID()));
				for (WidgetItem item : items){
					int i = 0;
					int purchases = calculateBuys(item);
					log.info("purchases: " + purchases);
					if(purchases == 0){
						oneClickUtilsPlugin.sanitizeEnqueue(oneClickUtilsPlugin.closeShop(), actionQueue, "Couldn't close shop");
						shouldHop = true;
					}else{
						while (i <  purchases){
							oneClickUtilsPlugin.sanitizeEnqueue(oneClickUtilsPlugin.buyItemFromShop(item,50 ), actionQueue, "Couldn't buy from shop");
							i++;
						}
						if(config.goldOre()){
							oneClickUtilsPlugin.sanitizeEnqueue(this.openBFBank(), actionQueue, "Couldn't open bank");
						}
					}
				}
			}
		}
		if(!actionQueue.isEmpty()){
			if(actionQueue.peek().getPostActionTickDelay() > 0){
				globalTimeout = actionQueue.peek().getPostActionTickDelay();
			}
			event.setMenuEntry(actionQueue.poll());
			return;
		}
	}

	@Subscribe
	private void onGameTick(GameTick gameTick) {
		if(globalTimeout > 0){
			//log.info("Gamnetick timeout was " + globalTimeout);
			globalTimeout--;
		}
	}

	private int calculateBuys(WidgetItem item){
		//Assumes you can buy 50 at a time
		if (item == null){
			return 0;
		}
		if (config.stackable()){
			return item.getQuantity() > config.shopQuantityThreshold() ? (item.getQuantity()-config.shopQuantityThreshold()) / 50 : 0;
		}else{
			if (item.getQuantity() > config.shopQuantityThreshold()){
				return 1;
			}else{
				return 0;
			}
		}
	}

	private LegacyMenuEntry openBFBank(){
		GameObject bankTarget = objectUtils.findNearestBank();
		if (bankTarget != null) {
			return new LegacyMenuEntry("Open",
					"Bank",
					bankTarget.getId(),
					MenuAction.GAME_OBJECT_FIRST_OPTION,
					bankTarget.getSceneMinLocation().getX(),
					bankTarget.getSceneMinLocation().getY(),
					false);
		}
		return null;
	}
}