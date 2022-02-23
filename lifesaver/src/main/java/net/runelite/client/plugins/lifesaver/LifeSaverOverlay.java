package net.runelite.client.plugins.lifesaver;

import com.openosrs.client.ui.overlay.components.table.TableAlignment;
import com.openosrs.client.ui.overlay.components.table.TableComponent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.plugins.oneclickutils.OneClickUtilsPlugin;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.ColorUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatDuration;

@Slf4j
@Singleton
class LifeSaverOverlay extends OverlayPanel {
    private final Client client;
    private final LifeSaverPlugin plugin;
    private final LifeSaverConfig config;
    String timeFormat;

    @Inject
    private OneClickUtilsPlugin oneClickUtilsPlugin;

    @Inject
    private LifeSaverOverlay(final Client client, final LifeSaverPlugin plugin, final LifeSaverConfig config) {
        super(plugin);
        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Life Saver Overlay"));
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.enableUI()) {
            log.debug("Overlay conditions not met, not starting overlay");
            return null;
        }

        TableComponent tableComponent = new TableComponent();
        tableComponent.setColumnAlignments(TableAlignment.LEFT, TableAlignment.RIGHT);

        tableComponent.addRow("State:", plugin.state.name());

        Duration duration = Duration.between(plugin.startTime, Instant.now());
        timeFormat = (duration.toHours() < 1) ? "mm:ss" : "HH:mm:ss";
        tableComponent.addRow("Time running:", formatDuration(duration.toMillis(), timeFormat));


        switch(plugin.state){
            case RUNNING:
                Duration duration2 = Duration.between(Instant.now(), plugin.nextBreakStartTime);
                timeFormat = (duration2.toHours() < 1) ? "mm:ss" : "HH:mm:ss";
                try {
                    tableComponent.addRow("Time until break:", formatDuration(duration2.toMillis(), timeFormat));
                }catch(IllegalArgumentException e){
                    tableComponent.addRow("Time until break:", "NOW");
                }
                break;
            case STOPPED:
                tableComponent.addRow("Stop Reason:", plugin.stopReason);
                break;
            case BREAKING:
                duration2 = Duration.between(Instant.now(), plugin.nextResumeStartTime);
                timeFormat = (duration2.toHours() < 1) ? "mm:ss" : "HH:mm:ss";
                try {
                    tableComponent.addRow("Time until resume:", formatDuration(duration2.toMillis(), timeFormat));
                }catch(IllegalArgumentException e){
                    tableComponent.addRow("Time until resume:", "NOW");
                }
        }
        tableComponent.addRow("Total Breaks:", Integer.toString(plugin.totalBreaks));
        tableComponent.addRow("Ticks since last xp drop:", Integer.toString(oneClickUtilsPlugin.getTicksSinceLastXpDrop()));

        if (!tableComponent.isEmpty()) {
            panelComponent.setBackgroundColor(ColorUtil.fromHex("#121212")); //Material Dark default
            panelComponent.setPreferredSize(new Dimension(200, 200));
            panelComponent.setBorder(new Rectangle(5, 5, 5, 5));
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Life Saver")
                    .color(ColorUtil.fromHex("#40C4FF"))
                    .build());
            panelComponent.getChildren().add(tableComponent);
        }
        return super.render(graphics);
    }
}
