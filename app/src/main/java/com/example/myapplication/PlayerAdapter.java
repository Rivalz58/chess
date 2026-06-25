package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PlayerAdapter extends RecyclerView.Adapter<PlayerAdapter.ViewHolder> {

    public interface OnPlayerClickListener {
        void onPlayerClick(PlayerItem player);
    }

    private final List<PlayerItem> players;
    private final OnPlayerClickListener listener;

    public PlayerAdapter(List<PlayerItem> players, OnPlayerClickListener listener) {
        this.players = players;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_player, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlayerItem player = players.get(position);
        holder.rankText.setText("#" + (position + 1));
        holder.usernameText.setText(player.username);
        holder.eloText.setText(player.elo + " Elo");
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPlayerClick(player);
        });
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView rankText, usernameText, eloText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            rankText = itemView.findViewById(R.id.rankText);
            usernameText = itemView.findViewById(R.id.usernameText);
            eloText = itemView.findViewById(R.id.eloText);
        }
    }
}
