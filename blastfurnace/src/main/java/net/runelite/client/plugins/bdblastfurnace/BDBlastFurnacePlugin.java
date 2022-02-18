package net.runelite.client.plugins.bdblastfurnace;

import com.google.inject.Provides;
import com.openosrs.client.game.WorldLocation;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.MenuAction;
import net.runelite.api.Varbits;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.iutils.BankUtils;
import net.runelite.client.plugins.iutils.InventoryUtils;
import net.runelite.client.plugins.iutils.ObjectUtils;
import net.runelite.client.plugins.oneclickutils.LegacyMenuEntry;
import net.runelite.client.plugins.oneclickutils.OneClickUtilsPlugin;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import static net.runelite.api.ItemID.*;

@Extension
@PluginDescriptor(
	name = "BD Blast Furnace",
	description = "BD Blast Furnace"
)

@Slf4j
@PluginDependency(OneClickUtilsPlugin.class)
public class BDBlastFurnacePlugin extends Plugin
{
	@Inject
	private BDBlastFurnaceConfig config;
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

	@Provides
	BDBlastFurnaceConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(BDBlastFurnaceConfig.class);
	}

	private Queue<LegacyMenuEntry> actionQueue = new LinkedList<LegacyMenuEntry>();
	private static final int CONVEYOR_ID = 9100;
	private static final int DISPENSER_ID = 9092;
	private static final int DISPENSER_WIDGET_ID = 17694734;
	private static final int COALBAG_ID = 12019;
	private static final int BANK_INTERACTION_PARAM1 = 983043;
	private boolean coalBagIsEmpty = true;
	private int barID;
	private int oreID;
	private int coalThreshold;
	private boolean bringCoal = false;
	private boolean expectingBarsToBeMade = false;
	private static final WorldArea bfArea = new WorldArea(1933,4956,30,30,0);
	private static final WorldPoint nextToConveyor = new WorldPoint(1942, 4967, 0);
	private int globalTimeout = 0;


	@Override
	protected void startUp() {
		actionQueue.clear();
		setBars();
	}

	@Override
	protected void shutDown() {
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if(event.getGroup().equals("BDOneClickBF")) {
			setBars();
		}
	}


	@Subscribe
	private void onClientTick(ClientTick event) {
		if(bfArea.contains(client.getLocalPlayer().getWorldLocation())){
			client.insertMenuItem("BD One Click Blast Furnace","",MenuAction.UNKNOWN.getId(),0,0,0,false);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		if (event.getMenuOption().contains("BD One Click Blast Furnace")){
			handleClick(event);
		}

		log.info(event.getMenuOption() + ", "
				+ event.getMenuTarget() + ", "
				+ event.getId() + ", "
				+ event.getMenuAction().name() + ", "
				+ event.getParam0() + ", "
				+ event.getParam1());
	}

	private void handleClick(MenuOptionClicked event) {
		//Check if something is blocking
		if (globalTimeout > 0){
			log.info("Timeout was " + globalTimeout);
			event.consume();
			return;
		}

		//Handle annoying timing dependant actions uniquely
		if (inventory.containsItem(STAMINA_POTION1)){
			if (bankUtils.isOpen()){
				event.setMenuEntry(oneClickUtilsPlugin.drinkPotionFromBank(STAMINA_POTION1));
			}else{
				event.setMenuEntry(oneClickUtilsPlugin.eatFood());
			}
			return;
		}

		if(actionQueue.isEmpty()) {
			int dispenserState = client.getVar(Varbits.BAR_DISPENSER);

			//if bank is not open and you have ores
			if(!bankUtils.isOpen() && (inventory.containsItem(oreID) || inventory.containsItem(COAL))){
				oneClickUtilsPlugin.sanitizeEnqueue(depositToConveyor(-1), actionQueue, "Couldn't load conveyor");
			}

			//if bank is open, run bank sequence
			else if (bankUtils.isOpen()){
				//Deposit bars
				if(inventory.containsItem(barID)){
					oneClickUtilsPlugin.sanitizeEnqueue(oneClickUtilsPlugin.depositAllOfItem(barID), actionQueue, "Couldn't desposit bars");
				}

				//Withdraw stamina
				if (client.getVar(Varbits.RUN_SLOWED_DEPLETION_ACTIVE) == 0 && client.getEnergy() <= 60){
					oneClickUtilsPlugin.sanitizeEnqueue(oneClickUtilsPlugin.withdrawItemAmount(STAMINA_POTION1,1,-1), actionQueue, "Couldn't withdraw 1 dose stamina");
				}

				//If using coal, fill up coal bag
				if (bringCoal){
					oneClickUtilsPlugin.sanitizeEnqueue(fillCoalBagFromBank(), actionQueue, "Couldn't fill coal bag from bank");
					log.info("Coal bag is not empty");
					coalBagIsEmpty = false;
				}

				//There is enough coal in the machine, bring ores, otherwise bring coal.
				if (client.getVar(Varbits.BLAST_FURNACE_COAL) >= coalThreshold || !bringCoal){
					oneClickUtilsPlugin.sanitizeEnqueue(oneClickUtilsPlugin.withdrawAllItem(oreID), actionQueue, "Couldn't withdraw ores");
					expectingBarsToBeMade = true;
				}else{
					oneClickUtilsPlugin.sanitizeEnqueue(oneClickUtilsPlugin.withdrawAllItem(COAL), actionQueue, "Couldn't withdraw ores");
					expectingBarsToBeMade = false;
				}

				//Go to conveyor
				oneClickUtilsPlugin.sanitizeEnqueue(depositToConveyor(-1), actionQueue, "Couldn't load conveyor");
			}

			//if inventory is full && have bars && bank isnt open, open bank
			else if (inventory.isFull() && inventory.containsItem(barID) && !bankUtils.isOpen()){
				oneClickUtilsPlugin.sanitizeEnqueue(this.openBank(), actionQueue, "Couldn't open bank");
			}

			//if by the conveyor and coal ag is not empty
			else if (client.getLocalPlayer().getWorldLocation().equals(nextToConveyor) && !coalBagIsEmpty){
				oneClickUtilsPlugin.sanitizeEnqueue(emptyCoalBag(), actionQueue, "Couldn't empty coal bag");
				oneClickUtilsPlugin.sanitizeEnqueue(depositToConveyor(1), actionQueue, "Couldn't load conveyor");
				if (oneClickUtilsPlugin.isItemEquipped(Set.of(MAX_CAPE_13342))){
					oneClickUtilsPlugin.sanitizeEnqueue(emptyCoalBag(), actionQueue, "Couldn't empty coal bag");
					oneClickUtilsPlugin.sanitizeEnqueue(depositToConveyor(1), actionQueue, "Couldn't load conveyor");
				}
				log.info("Coal bag is empty");
				coalBagIsEmpty = true;
			}

			//if dispenser has bars ready
			else if (dispenserState == 2 || dispenserState == 3  && !inventory.isFull()){
				if (client.getWidget(DISPENSER_WIDGET_ID) != null){
					oneClickUtilsPlugin.sanitizeEnqueue(takeBarsWidget(), actionQueue, "Couldn't take bars via widget");
					oneClickUtilsPlugin.sanitizeEnqueue(this.openBank(), actionQueue, "Couldn't open bank");
				}else{
					oneClickUtilsPlugin.sanitizeEnqueue(takeFromDispenser(), actionQueue, "Could not find dispenser");
				}
			}

			//if inventory is just a coal bag and coal bag is empty, walk to the dispenser
			else if(coalBagIsEmpty && inventory.containsItemAmount(COALBAG_ID, 1, false, true)){
				if(expectingBarsToBeMade){
					event.consume();
					LocalPoint point = LocalPoint.fromWorld(client, new WorldPoint(1940,4962,client.getPlane()));
					oneClickUtilsPlugin.walkTile(point.getSceneX(), point.getSceneY());
					return;
				}else{
					oneClickUtilsPlugin.sanitizeEnqueue(this.openBank(), actionQueue, "Couldn't open bank");
				}
			}
		}
		if(!actionQueue.isEmpty()){
			log.info(actionQueue.toString());
			if(actionQueue.peek().getPostActionTickDelay() > 0){
				globalTimeout = actionQueue.peek().getPostActionTickDelay();
			}
			event.setMenuEntry(actionQueue.poll());
		}
	}

	@Subscribe
	private void onGameTick(GameTick gameTick) {
		if(globalTimeout > 0){
			globalTimeout--;
		}
	}

	private void setBars(){
		int thresholdMultiplier = 27;
		bringCoal = false;
		switch (config.barType()){
			case SILVER:
				coalThreshold = 0;
				barID = SILVER_BAR;
				oreID = SILVER_ORE;
				break;
			case GOLD:
				coalThreshold = 0;
				barID = GOLD_BAR;
				oreID = GOLD_ORE;
				break;
			case IRON:
				coalThreshold = 0;
				barID = IRON_BAR;
				oreID = IRON_ORE;
				break;
			case STEEL:
				bringCoal = true;
				coalThreshold = thresholdMultiplier+thresholdMultiplier*0;
				barID = STEEL_BAR;
				oreID = IRON_ORE;
				break;
			case MITHRIL:
				coalThreshold = thresholdMultiplier+thresholdMultiplier*1;
				bringCoal = true;
				barID = MITHRIL_BAR;
				oreID = MITHRIL_ORE;
				break;
			case ADAMANTITE:
				coalThreshold = thresholdMultiplier+thresholdMultiplier*2;
				bringCoal = true;
				barID = ADAMANTITE_BAR;
				oreID = ADAMANTITE_ORE;
				break;
			case RUNITE:
				coalThreshold = thresholdMultiplier+thresholdMultiplier*3;
				bringCoal = true;
				barID = RUNITE_BAR;
				oreID = RUNITE_ORE;
				break;
		}
	}


	private LegacyMenuEntry depositToConveyor(int afterActionTickDelay){
		GameObject conveyor = oneClickUtilsPlugin.getGameObject(CONVEYOR_ID);
		if(conveyor != null){
			return new LegacyMenuEntry("Put ore on",
					"Conveyor",
					conveyor.getId(),
					MenuAction.GAME_OBJECT_FIRST_OPTION,
					oneClickUtilsPlugin.getObjectParam0(conveyor),
					oneClickUtilsPlugin.getObjectParam1(conveyor),
					false,
					afterActionTickDelay);
		}
		return null;
	}

	private LegacyMenuEntry takeFromDispenser(){
		GameObject dispenser = oneClickUtilsPlugin.getGameObject(DISPENSER_ID);
		if (dispenser != null){
			return new LegacyMenuEntry("Take",
					"Bar dispenser",
					dispenser.getId(),
					MenuAction.GAME_OBJECT_FIRST_OPTION,
					oneClickUtilsPlugin.getObjectParam0(dispenser),
					oneClickUtilsPlugin.getObjectParam1(dispenser),
					false);
		}
		return null;
	}

	private LegacyMenuEntry openBank(){
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

	private LegacyMenuEntry takeBarsWidget(){
		if (client.getWidget(DISPENSER_WIDGET_ID) != null){
			return new LegacyMenuEntry("Take",
					"Bars",
					1,
					MenuAction.CC_OP,
					-1,
					DISPENSER_WIDGET_ID,
					false);
		}
		return null;
	}


	private LegacyMenuEntry fillCoalBagFromBank(){
		WidgetItem coalBagWidget = oneClickUtilsPlugin.getWidgetItem(COALBAG_ID);
		if(coalBagWidget != null){
			return new LegacyMenuEntry( "Fill",
					"Coal Bag",
					9,
					MenuAction.CC_OP,
					coalBagWidget.getIndex(),
					BANK_INTERACTION_PARAM1,
					false);
		}
		return null;
	}

	private LegacyMenuEntry emptyCoalBag(){
		WidgetItem coalBagWidget = oneClickUtilsPlugin.getWidgetItem(COALBAG_ID);
		if(coalBagWidget != null){
			return new LegacyMenuEntry( "Empty",
					"Coal Bag",
					coalBagWidget.getId(),
					MenuAction.ITEM_FOURTH_OPTION,
					coalBagWidget.getIndex(),
					WidgetInfo.INVENTORY.getId(),
					false);
		}
		return null;
	}
}