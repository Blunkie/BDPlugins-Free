package net.runelite.client.plugins.lifesaver;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.oneclickutils.OneClickUtilsPlugin;
import org.pf4j.Extension;
import javax.inject.Inject;

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
	@Provides
	LifeSaverConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(LifeSaverConfig.class);
	}

	private static final WorldArea prifSpawnArea = new WorldArea(3253,6072,25,25,0);

	@Override
	protected void startUp() {
		configManager.setConfiguration("autoprayflick", "onlyInNmz", false);
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
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (oneClickUtilsPlugin.getTicksSinceLastXpDrop() > config.watchDogTickTimer()){
			configManager.setConfiguration("autoprayflick", "onlyInNmz", true);
		}
	}

	@Subscribe
	private void onChatMessage(ChatMessage event){
		if (event.getMessage().equals("You are out of food")){
			configManager.setConfiguration("autoprayflick", "onlyInNmz", true);
		}
	}
}