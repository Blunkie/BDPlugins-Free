package net.runelite.client.plugins.genericocp;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
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

@Extension
@PluginDescriptor(
	name = "BD Generic OCP",
	description = "BD Generic OCP"
)

@Slf4j
@PluginDependency(OneClickUtilsPlugin.class)
public class GenericOCP extends Plugin
{
	@Inject
	private GenericOCPConfig config;
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
	GenericOCPConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(GenericOCPConfig.class);
	}

	private Queue<LegacyMenuEntry> actionQueue = new LinkedList<LegacyMenuEntry>();
	private int globalTimeout = 0;


	@Override
	protected void startUp() {
		actionQueue.clear();
	}

	@Override
	protected void shutDown() {
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if(event.getGroup().equals("BD Generic OCP")) {

		}
	}


	@Subscribe
	private void onClientTick(ClientTick event) {
		if(false){
			client.insertMenuItem("BD Generic OCP","",MenuAction.UNKNOWN.getId(),0,0,0,false);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		if (event.getMenuOption().contains("BD Generic OCP")){
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
			//populate actions
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
	}
}