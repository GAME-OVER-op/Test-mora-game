package com.mora.gamedock;

/** Модель карточки-плитки (аналог TileView / DockTile). */
public class Tile {
    public String name;
    public String icon;
    public boolean active;

    public Tile(String name, String icon, boolean active) {
        this.name = name;
        this.icon = icon;
        this.active = active;
    }
}
