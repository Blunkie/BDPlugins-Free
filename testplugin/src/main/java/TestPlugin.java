import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.oneclickutils.LegacyMenuEntry;
import net.runelite.client.plugins.oneclickutils.OneClickUtilsPlugin;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.Queue;

@Extension
@PluginDescriptor(
	name = "BD Test Plugin",
	description = "BD Test Plugin"
)

@Slf4j
@PluginDependency(OneClickUtilsPlugin.class)
public class TestPlugin extends Plugin
{
	// Injects our config
	@Inject
	private TestConfig config;
	@Inject
	private OneClickUtilsPlugin oneClickUtilsPlugin;

	// Provides our config
	@Provides
	TestConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TestConfig.class);
	}

	private Queue<LegacyMenuEntry> actionQueue = new LinkedList<LegacyMenuEntry>();

	@Override
	protected void startUp() {
		actionQueue.clear();
	}

	@Override
	protected void shutDown() {
	}

	@Subscribe
	private void onGameTick(GameTick gameTick) {
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		handleClick(event);
		log.info(event.getMenuOption() + ", "
				+ event.getMenuTarget() + ", "
				+ event.getId() + ", "
				+ event.getMenuAction().name() + ", "
				+ event.getParam0() + ", "
				+ event.getParam1());
	}

	private void handleClick(MenuOptionClicked event) {
		//Only intercept cancel menu option
		if (!event.getMenuOption().equals("Cancel")) {
			return;
		}


		if(actionQueue.isEmpty()){
			oneClickUtilsPlugin.combineQueues(actionQueue, oneClickUtilsPlugin.dropItems(oneClickUtilsPlugin.parseIOs(config.dropIDs()),
					"1,5,2,6,3,7,4,8,9,13,10,14,11,15,12,16,17,21,18,22,19,23,20,24,25,26,27,28"));
		}
		if(!actionQueue.isEmpty()){
			log.info(actionQueue.toString());
			event.setMenuEntry(actionQueue.poll());
		}
	}
}