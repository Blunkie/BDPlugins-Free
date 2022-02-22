import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.iutils.InventoryUtils;
import net.runelite.client.plugins.oneclickutils.LegacyMenuEntry;
import net.runelite.client.plugins.oneclickutils.OneClickUtilsPlugin;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

@Extension
@PluginDescriptor(
	name = "BD Herb Cleaner",
	description = "BD Herb Cleaner"
)

@Slf4j
@PluginDependency(OneClickUtilsPlugin.class)
public class BDHerbCleaner extends Plugin {
	@Inject
	private OneClickUtilsPlugin oneClickUtilsPlugin;
	@Inject
	private ConfigManager configManager;
	@Inject
	private Client client;
	@Inject
	private InventoryUtils inventory;
	@Inject
	private ItemManager itemManager;
	@Inject
	private ClientThread clientThread;

	private Queue<LegacyMenuEntry> actionQueue = new LinkedList<LegacyMenuEntry>();
	private int globalTimeout = 0;
	private boolean cleaning = false;


	@Override
	protected void startUp() {
		actionQueue.clear();
		clientThread.execute(this::checkingCleaning);
	}

	@Override
	protected void shutDown() {
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if(event.getGroup().equals("BD Herb Cleaner")) {

		}
	}


	@Subscribe
	private void onClientTick(ClientTick event) {
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		if (cleaning && event.getMenuOption().contains("Cancel")){
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
			Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
			if (inventoryWidget != null) {
				Collection<WidgetItem> items = inventoryWidget.getWidgetItems();
				for (WidgetItem item : items) {
					String[] menuActions = itemManager.getItemComposition(item.getId()).getInventoryActions();
					for (String action : menuActions) {
						if (action != null && action.contains("Clean")) {
							oneClickUtilsPlugin.sanitizeEnqueue(oneClickUtilsPlugin.clickItem(item), actionQueue, "Couldn't clean herb");
						}
					}
				}
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
	}

	@Subscribe
	private void onItemContainerChanged(ItemContainerChanged event){
		clientThread.execute(this::checkingCleaning);
	}

	private void checkingCleaning(){
		cleaning = false;
		Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
		if (inventoryWidget != null) {
			Collection<WidgetItem> items = inventoryWidget.getWidgetItems();
			for (WidgetItem item : items) {
				String[] menuActions = itemManager.getItemComposition(item.getId()).getInventoryActions();
				for (String action : menuActions) {
					if (action != null && action.contains("Clean")) {
						cleaning = true;
						return;
					}
				}
			}
		}
	}
}