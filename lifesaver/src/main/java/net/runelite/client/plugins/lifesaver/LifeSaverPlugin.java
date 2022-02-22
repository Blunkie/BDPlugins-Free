package net.runelite.client.plugins.lifesaver;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.events.*;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.iutils.InventoryUtils;
import net.runelite.client.plugins.oneclickutils.LegacyMenuEntry;
import net.runelite.client.plugins.oneclickutils.OneClickUtilsPlugin;
import org.pf4j.Extension;
import javax.inject.Inject;
import java.util.Random;
import java.util.Set;

import static net.runelite.api.ItemID.*;

@SuppressWarnings("ALL")
@Extension
@PluginDescriptor(
	name = "BD Life Saver",
	description = "BD life Saver Plugin"
)

@Slf4j
@PluginDependency(OneClickUtilsPlugin.class)
public class LifeSaverPlugin extends Plugin {
	@Inject
	private OneClickUtilsPlugin oneClickUtilsPlugin;
	@Inject
	private ConfigManager configManager;
	@Inject
	private Client client;
	@Inject
	private LifeSaverConfig config;
	@Inject
	private InventoryUtils inventory;
	@Provides
	LifeSaverConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(LifeSaverConfig.class);
	}

	private static final WorldArea prifSpawnArea = new WorldArea(3253,6072,25,25,0);
	private int max = 35;
	private int min = 9;
	Random random = new Random();
	private int randomCoinPouchSize = 28;
	Set<Integer> pouches = Set.of(COIN_POUCH_22531,COIN_POUCH_22532,COIN_POUCH_22523,COIN_POUCH_22534);


	@Override
	protected void startUp() {
		configManager.setConfiguration("autoprayflick", "onlyInNmz", false);
		randomizePouch();
	}

	@Override
	protected void shutDown() {
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		handleClick(event);
	}

	private void handleClick(MenuOptionClicked event) {
		if (prifSpawnArea.contains(client.getLocalPlayer().getWorldLocation())){
			event.setMenuEntry(oneClickUtilsPlugin.maxCapeTeleToPOH());
			configManager.setConfiguration("autoprayflick", "onlyInNmz", true);
			log.info("Found you in Prif, teleporting to POH. Disabling other plugins");
			return;
		}

		if (event.getMenuOption().contains("BD Open Pouch")){
			event.setMenuEntry(oneClickUtilsPlugin.clickItem(inventory.getWidgetItem(pouches)));
			return;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (oneClickUtilsPlugin.getTicksSinceLastXpDrop() > config.watchDogTickTimer()){
			configManager.setConfiguration("autoprayflick", "onlyInNmz", true);
		}
	}

	@Subscribe
	private void onChatMessage(ChatMessage event){
		if (event.getMessage().contains("You are out of food")){
			configManager.setConfiguration("autoprayflick", "onlyInNmz", true);
		}
		if (event.getMessage().contains("You open all of the pouches")){
			randomizePouch();
		}
	}

	@Subscribe
	private void onClientTick(ClientTick event) {
		if(inventory.containsItemAmount(pouches, Math.min(randomCoinPouchSize,28), true, false)){
			client.insertMenuItem("BD Open Pouch","", MenuAction.UNKNOWN.getId(),0,0,0,false);
		}
	}

	private void randomizePouch(){
		randomCoinPouchSize = random.nextInt(max - min + 1) + min;
	}
}