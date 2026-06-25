package com.example.myapplication;

public class GameHistoryItem {
    public final String opponentUsername;
    public final String resultLabel;
    public final int resultColor;
    public final String date;
    public final int moveCount;

    public GameHistoryItem(String opponentUsername, String resultLabel, int resultColor, String date, int moveCount) {
        this.opponentUsername = opponentUsername;
        this.resultLabel = resultLabel;
        this.resultColor = resultColor;
        this.date = date;
        this.moveCount = moveCount;
    }
}
