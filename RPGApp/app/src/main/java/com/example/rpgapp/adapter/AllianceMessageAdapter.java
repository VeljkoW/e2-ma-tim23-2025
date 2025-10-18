package com.example.rpgapp.adapter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpgapp.R;
import com.example.rpgapp.model.AllianceMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AllianceMessageAdapter extends RecyclerView.Adapter<AllianceMessageAdapter.MessageViewHolder> {

    private List<AllianceMessage> messages;
    private String currentUserId;

    public AllianceMessageAdapter(String currentUserId) {
        this.messages = new ArrayList<>();
        this.currentUserId = currentUserId;
    }

    public void setMessages(List<AllianceMessage> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alliance_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        AllianceMessage message = messages.get(position);
        boolean isOwnMessage = message.getSenderId().equals(currentUserId);
        holder.bind(message, isOwnMessage);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout messageContainer;
        private TextView tvSenderName, tvMessage, tvTimestamp;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }

        public void bind(AllianceMessage message, boolean isOwnMessage) {
            tvSenderName.setText(message.getSenderUsername());
            tvMessage.setText(message.getMessage());

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            tvTimestamp.setText(sdf.format(new Date(message.getTimestamp())));

            // Align message based on sender
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) messageContainer.getLayoutParams();
            if (isOwnMessage) {
                params.gravity = Gravity.END;
                messageContainer.setBackgroundResource(R.drawable.message_bubble_own);
            } else {
                params.gravity = Gravity.START;
                messageContainer.setBackgroundResource(R.drawable.message_bubble_other);
            }
            messageContainer.setLayoutParams(params);
        }
    }
}

