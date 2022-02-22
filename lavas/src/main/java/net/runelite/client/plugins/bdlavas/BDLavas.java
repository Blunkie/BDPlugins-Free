package net.runelite.client.plugins.bdlavas;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
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
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import static net.runelite.api.ItemID.*;
import static net.runelite.api.ItemID.RING_OF_DUELING7;

@Extension
@PluginDescriptor(
	name = "BD Lavas",
	description = "BD Lavas"
)

@Slf4j
@PluginDependency(OneClickUtilsPlugin.class)
public class BDLavas extends Plugin
{
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

	private static final int RUINS_ID = 34817;
	private static final int ALTAR_ID = 34764;
	private static final int BANK_ID = 14886;
	private static final int POOL_ID = 40848;
	private int pouch1 = GIANT_POUCH;
	private int pouch2 = LARGE_POUCH;
	private int essenceID = PURE_ESSENCE;
	private static final Set<Integer> duelingRings = Set.of(RING_OF_DUELING8, RING_OF_DUELING1, RING_OF_DUELING2, RING_OF_DUELING3, RING_OF_DUELING4, RING_OF_DUELING5, RING_OF_DUELING6, RING_OF_DUELING7);
	private static final String MAGIC_IMBUE_MESSAGE = "You are charged to combine runes!";
	private static final String BIND_MESSAGE1 = "You bind the temple";
	private static final String BIND_MESSAGE2 = "You partially success to bind";
	private static final WorldPoint ruinsLocation = new WorldPoint(3313, 3255, 0);
	private static final WorldPoint nexToAltar = new WorldPoint(2584, 4840, 0);
	private int globalTimeout = 0;
	private long lastMagicImbueCast = 0;
	private int lavaCraftCount = 0;
	private boolean suspectPouchesFull = false;
	private boolean waitingForDueling = false;
	private boolean waitingForBinding = false;
	private boolean teleportLockout = false;
	private int runEnergyThreshold = 10;

	private static final int SPELLBOOK_VARBIT = 4070;
	private static final int LUNAR_VARBIT = 2;

	private Queue<LegacyMenuEntry> actionQueue = new LinkedList<LegacyMenuEntry>();

	@Override
	protected void startUp() {
		actionQueue.clear();
	}

	@Override
	protected void shutDown() {
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if(event.getGroup().equals("BD Lavas")) {

		}
	}


	@Subscribe
	private void onClientTick(ClientTick event) {
		if(true){
			client.insertMenuItem("BD Lavas","",MenuAction.UNKNOWN.getId(),0,0,0,false);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		if (event.getMenuOption().contains("BD Lavas")){
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
			GameObject ruins = oneClickUtilsPlugin.getGameObject(RUINS_ID);
			GameObject altar = oneClickUtilsPlugin.getGameObject(ALTAR_ID);
			GameObject pool = oneClickUtilsPlugin.getGameObject(POOL_ID);

			if (ruins != null) {
				if (client.getLocalPlayer().isMoving()){
					event.consume();
				}else{
					oneClickUtilsPlugin.sanitizeEnqueue(oneClickUtilsPlugin.clickGameObject(ruins, null), actionQueue, "Couldn't enter ruins");
					teleportLockout = false;
				}
			}

			else if (altar != null){
				if (!client.getLocalPlayer().getWorldLocation().equals(nexToAltar) && !client.getLocalPlayer().isMoving()){
					event.consume();
					LocalPoint point = LocalPoint.fromWorld(client, nexToAltar);
					oneClickUtilsPlugin.walkTile(point.getSceneX(), point.getSceneY());
					return;
				}
				if (!isMagicImbueActive()){
					oneClickUtilsPlugin.sanitizeEnqueue(castMagicImbue(), actionQueue, "Couldn't cast magic imbue");
				}
				if (inventory.containsItem(essenceID)){
					oneClickUtilsPlugin.sanitizeEnqueue(craftLavas(altar), actionQueue, "Couldn't craft lavas");
				}
				if (suspectPouchesFull && lavaCraftCount == 1){
					oneClickUtilsPlugin.sanitizeEnqueue(emptyPouch(pouch1), actionQueue, "Couldn't empty pouch1");
					oneClickUtilsPlugin.sanitizeEnqueue(emptyPouch(pouch2), actionQueue, "Couldn't empty pouch2");
					suspectPouchesFull = false;
					oneClickUtilsPlugin.sanitizeEnqueue(craftLavas(altar), actionQueue, "Couldn't craft lavas");
				}
				if(!suspectPouchesFull && lavaCraftCount == 2 && !teleportLockout){
					if(client.getEnergy() <= runEnergyThreshold){
						oneClickUtilsPlugin.sanitizeEnqueue(oneClickUtilsPlugin.maxCapeTeleToPOH(), actionQueue, "Couldn't tele to POH");
					}else {
						oneClickUtilsPlugin.sanitizeEnqueue(oneClickUtilsPlugin.maxCapeTeleToCraftingGuild(), actionQueue, "Couldn't tele to crafting guild");
					}
					teleportLockout = true;
				}
			}

			else if(pool != null){
				if (client.getEnergy() < 100){
					oneClickUtilsPlugin.sanitizeEnqueue(oneClickUtilsPlugin.clickGameObject(pool, null, 1), actionQueue, "Couldn't enter ruins");
					teleportLockout = false;
				}else if(!teleportLockout){
					oneClickUtilsPlugin.sanitizeEnqueue(oneClickUtilsPlugin.maxCapeTeleToCraftingGuild(), actionQueue, "Couldn't tele to POH");
					teleportLockout = true;
				}
			}

			else if(!bankUtils.isOpen()) {
				if (client.getLocalPlayer().isMoving()) {
					event.consume();
				} else{
					oneClickUtilsPlugin.sanitizeEnqueue(oneClickUtilsPlugin.clickGameObject(BANK_ID, MenuAction.GAME_OBJECT_FIRST_OPTION), actionQueue, "Couldn't open bank");
				teleportLockout = false;
				}
			}

			else if(bankUtils.isOpen()){
				if (inventory.containsItem(LAVA_RUNE)){
					oneClickUtilsPlugin.sanitizeEnqueue(oneClickUtilsPlugin.depositAllOfItem(LAVA_RUNE), actionQueue, "Couldn't deposit lava rune");
				}
				handleJewelry();
				if(!suspectPouchesFull) {
					oneClickUtilsPlugin.sanitizeEnqueue(oneClickUtilsPlugin.withdrawAllItem(essenceID), actionQueue, "Couldn't withdraw all essence");
					oneClickUtilsPlugin.sanitizeEnqueue(fillPouch(pouch1), actionQueue, "Couldnt fill pouch1");
					oneClickUtilsPlugin.sanitizeEnqueue(fillPouch(pouch2), actionQueue, "Couldnt fill pouch2");
					suspectPouchesFull = true;
				}
				if (!waitingForDueling && !waitingForBinding){
					oneClickUtilsPlugin.sanitizeEnqueue(oneClickUtilsPlugin.withdrawAllItem(essenceID), actionQueue, "Couldn't withdraw all essence");
					if(!teleportLockout && oneClickUtilsPlugin.isItemEquipped(duelingRings)){
						oneClickUtilsPlugin.sanitizeEnqueue(teleToDuelArena(), actionQueue, "Couldn't teleport to duel arena");
						teleportLockout = true;
						lavaCraftCount = 0;
					}
				}
			}else{
				event.consume();
			}

		}

		if(!actionQueue.isEmpty()){
			//log.info(actionQueue.toString());
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
		//log.info("Tele lockout = " + teleportLockout);
		//log.info("craftCount = " + lavaCraftCount);
		//log.info("SuspectPouchesFull = " + suspectPouchesFull);
		//log.info("isMagicImbueActive = " + isMagicImbueActive());
	}

	private LegacyMenuEntry craftLavas(GameObject altar) {
		WidgetItem highlightedItem = oneClickUtilsPlugin.getWidgetItem(EARTH_RUNE);
		if (highlightedItem == null) {
			log.info("Couldn't find earth runes to craft lavas hello");
			return null;
		} else {
			client.setSelectedItemWidget(WidgetInfo.INVENTORY.getId());
			client.setSelectedItemSlot(highlightedItem.getIndex());
			client.setSelectedItemID(highlightedItem.getId());
		}

		return new LegacyMenuEntry("Use Earth Runes",
				"Altar",
				ALTAR_ID,
				MenuAction.ITEM_USE_ON_GAME_OBJECT,
				oneClickUtilsPlugin.getObjectParam0(altar),
				oneClickUtilsPlugin.getObjectParam1(altar),
				false);
	}

	private void handleJewelry(){
		if (!oneClickUtilsPlugin.isItemEquipped(duelingRings)){
			if (inventory.containsItem(RING_OF_DUELING8)){
				oneClickUtilsPlugin.sanitizeEnqueue(oneClickUtilsPlugin.equipItemFromBank(RING_OF_DUELING8), actionQueue, "Couldnt equip ring of dueling 8 from bank");
				waitingForDueling = false;
			}else{
				if (!waitingForDueling){
					oneClickUtilsPlugin.sanitizeEnqueue(oneClickUtilsPlugin.withdrawItemAmount(RING_OF_DUELING8, 1), actionQueue, "Couldn't withdraw dueling ring 8");
					waitingForDueling = true;
				}
			}
		}
		if (!oneClickUtilsPlugin.isItemEquipped(BINDING_NECKLACE)){
			if (inventory.containsItem(BINDING_NECKLACE)){
				oneClickUtilsPlugin.sanitizeEnqueue(oneClickUtilsPlugin.equipItemFromBank(BINDING_NECKLACE), actionQueue, "Couldnt equip binding necklace from bank");
				waitingForBinding = false;
			}else{
				if (!waitingForBinding){
					oneClickUtilsPlugin.sanitizeEnqueue(oneClickUtilsPlugin.withdrawItemAmount(BINDING_NECKLACE, 1), actionQueue, "Couldn't withdraw binding necklace");
					waitingForBinding = true;
				}
			}
		}
	}

	private LegacyMenuEntry emptyPouch(int pouchID){
		if (inventory.getWidgetItem(pouchID) != null) {
			return new LegacyMenuEntry("Empty",
					"Pouch",
					pouchID,
					MenuAction.ITEM_SECOND_OPTION,
					inventory.getWidgetItem(pouchID).getIndex(),
					WidgetInfo.INVENTORY.getId(),
					false);
		}
		return null;
	}

	private LegacyMenuEntry	fillPouch(int pouchID){
		if (inventory.getWidgetItem(pouchID) != null){
			return new LegacyMenuEntry("Fill1",
					"Pouch",
					9,
					MenuAction.CC_OP_LOW_PRIORITY,
					inventory.getWidgetItem(pouchID).getIndex(),
					983043,
					false);
		}
		return null;
	}

	private LegacyMenuEntry teleToDuelArena(){
		ItemContainer equipmentContainer = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipmentContainer != null) {
			Item[] items = equipmentContainer.getItems();
			for (Item item : items) {
				if (duelingRings.contains(item.getId())) {
					return new LegacyMenuEntry("Duel Arena",
										"Ring of dueling",
										2,
										MenuAction.CC_OP,
										-1,
										WidgetInfo.EQUIPMENT_RING.getId(),
										false);

				}
			}
		}
		return null;
	}

	private LegacyMenuEntry castMagicImbue(){
		if (client.getVarbitValue(SPELLBOOK_VARBIT) != LUNAR_VARBIT){
			log.info("You need to be on Lunars to cast magic imbue");
			return null;
		}
		return new LegacyMenuEntry(
				"Cast Magic Imbue",
				"",
				1,
				MenuAction.CC_OP,
				-1,
				WidgetInfo.SPELL_MAGIC_IMBUE.getId(),
				true);
	}

	private boolean isMagicImbueActive(){
		return System.currentTimeMillis()-lastMagicImbueCast < 12*1000;
	}



	@Subscribe
	public void onChatMessage(ChatMessage event){
		if (event.getMessage().equals(MAGIC_IMBUE_MESSAGE)){
			lastMagicImbueCast = System.currentTimeMillis();
		}
		if(event.getMessage().startsWith(BIND_MESSAGE1) || event.getMessage().startsWith(BIND_MESSAGE2)){
			lavaCraftCount++;
		}
	}
}