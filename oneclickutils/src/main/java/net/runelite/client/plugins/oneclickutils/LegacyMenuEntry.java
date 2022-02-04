package net.runelite.client.plugins.oneclickutils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;

import java.util.function.Consumer;

@AllArgsConstructor
@Getter
public class LegacyMenuEntry implements MenuEntry
{
    private String option;
    private String target;
    private int identifier;
    private MenuAction menuAction;
    private int param0;
    private int param1;
    private boolean forceLeftClick;

    @Override
    public boolean isDeprioritized()
    {
        return false;
    }

    @Override
    public MenuEntry setDeprioritized(boolean deprioritized)
    {
        return this;
    }

    @Override
    public MenuEntry onClick(Consumer<MenuEntry> callback)
    {
        return null;
    }

    public int getOpcode()
    {
        return menuAction.getId();
    }

    public void setOpcode(int opcode)
    {
        menuAction = MenuAction.of(opcode);
    }

    public int getActionParam0()
    {
        return param0;
    }

    public void setActionParam0(int param0)
    {
        this.param0 = param0;
    }

    public int getActionParam1()
    {
        return param1;
    }

    public void setActionParam1(int param1)
    {
        this.param1 = param1;
    }

    public MenuAction getMenuAction()
    {
        return menuAction;
    }

    public MenuEntry setOption(String option)
    {
        this.option = option;
        return this;
    }

    public MenuEntry setTarget(String target)
    {
        this.target = target;
        return this;
    }

    public MenuEntry setIdentifier(int identifier)
    {
        this.identifier = identifier;
        return this;
    }

    public MenuAction getType()
    {
        return menuAction;
    }

    public MenuEntry setType(MenuAction menuAction)
    {
        this.menuAction = menuAction;
        return this;
    }

    public MenuEntry setParam0(int param0)
    {
        this.param0 = param0;
        return this;
    }

    public MenuEntry setParam1(int param1)
    {
        this.param1 = param1;
        return this;
    }

    public MenuEntry setForceLeftClick(boolean forceLeftClick)
    {
        this.forceLeftClick = forceLeftClick;
        return this;
    }

    @Override
    public String toString() {
        return "LegacyMenuEntry{" +
                "option='" + option + '\'' +
                ", target='" + target + '\'' +
                ", identifier=" + identifier +
                ", menuAction=" + menuAction +
                ", param0=" + param0 +
                ", param1=" + param1 +
                ", forceLeftClick=" + forceLeftClick +
                '}';
    }
}