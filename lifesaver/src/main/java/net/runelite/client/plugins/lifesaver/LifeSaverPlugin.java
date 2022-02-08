package net.runelite.client.plugins.lifesaver;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.oneclickutils.BankTele;
import net.runelite.client.plugins.oneclickutils.LegacyMenuEntry;
import net.runelite.client.plugins.oneclickutils.OneClickUtilsPlugin;
import org.pf4j.Extension;
import javax.inject.Inject;
import java.util.LinkedList;
import java.util.Queue;

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

	private static final WorldArea prifSpawnArea = new WorldArea(3253,6072,25,25,0);

	private Queue<LegacyMenuEntry> actionQueue = new LinkedList<LegacyMenuEntry>();

	@Override
	protected void startUp() {
		actionQueue.clear();
		configManager.setConfiguration("autoprayflick", "onlyInNmz", false);
	}

	@Override
	protected void shutDown() {
	}


	@Subscribe
	private void onClientTick(ClientTick event) {
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
}