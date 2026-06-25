package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class GameHistoryAdapter extends RecyclerView.Adapter<GameHistoryAdapter.ViewHolder> {

    private final List<GameHistoryItem> games;

    public GameHistoryAdapter(List<GameHistoryItem> games) {
        this.games = games;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_game_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GameHistoryItem game = games.get(position);
        holder.opponentText.setText("vs " + game.opponentUsername);
        holder.resultText.setText(game.resultLabel);
        holder.resultText.setTextColor(game.resultColor);
        holder.dateText.setText(game.date + " · " + game.moveCount + " coups");
    }

    @Override
    public int getItemCount() {
        return games.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView opponentText, resultText, dateText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            opponentText = itemView.findViewById(R.id.opponentText);
            resultText = itemView.findViewById(R.id.resultText);
            dateText = itemView.findViewById(R.id.dateText);
        }
    }
}
