package de.mstein.geotracker;

/**
 * Created by Mattes on 04.12.2015.
 */
public class ListItem {
    private String name;
    private int icon;

    public ListItem(String name, int icon) {
        this.name = name;
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }
}
