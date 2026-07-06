package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PlayerAdapter extends RecyclerView.Adapter<PlayerAdapter.ViewHolder> {

    public interface OnPlayerClickListener {
        void onPlayerClick(PlayerItem player);
    }

    public interface OnChallengeClickListener {
        void onChallengeClick(PlayerItem player);
    }

    private final List<PlayerItem> players;
    private final int ownPlayerId;
    private final OnPlayerClickListener listener;
    private final OnChallengeClickListener challengeListener;

    public PlayerAdapter(List<PlayerItem> players, int ownPlayerId,
                          OnPlayerClickListener listener, OnChallengeClickListener challengeListener) {
        this.players = players;
        this.ownPlayerId = ownPlayerId;
        this.listener = listener;
        this.challengeListener = challengeListener;
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

        if (player.id == ownPlayerId) {
            holder.challengeButton.setVisibility(View.GONE);
        } else {
            holder.challengeButton.setVisibility(View.VISIBLE);
            holder.challengeButton.setOnClickListener(v -> {
                if (challengeListener != null) challengeListener.onChallengeClick(player);
            });
        }
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView rankText, usernameText, eloText;
        final Button challengeButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            rankText = itemView.findViewById(R.id.rankText);
            usernameText = itemView.findViewById(R.id.usernameText);
            eloText = itemView.findViewById(R.id.eloText);
            challengeButton = itemView.findViewById(R.id.challengeButton);
        }
    }
}
