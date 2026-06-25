package com.example.myapplication;

public class PlayerItem {
    public final int id;
    public final String username;
    public final int elo;

    public PlayerItem(int id, String username, int elo) {
        this.id = id;
        this.username = username;
        this.elo = elo;
    }
}
