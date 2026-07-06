package com.example.myapplication;

public class ChallengeItem {
    public final int id;
    public final String challengerUsername;
    public final int challengerElo;

    public ChallengeItem(int id, String challengerUsername, int challengerElo) {
        this.id = id;
        this.challengerUsername = challengerUsername;
        this.challengerElo = challengerElo;
    }
}
