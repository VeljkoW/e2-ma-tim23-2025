package com.example.rpgapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpgapp.R;
import com.example.rpgapp.model.AllianceInvitation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AllianceInvitationAdapter extends RecyclerView.Adapter<AllianceInvitationAdapter.InvitationViewHolder> {

    private List<AllianceInvitation> invitations;
    private OnInvitationActionListener listener;

    public interface OnInvitationActionListener {
        void onAccept(AllianceInvitation invitation);
        void onReject(AllianceInvitation invitation);
    }

    public AllianceInvitationAdapter(OnInvitationActionListener listener) {
        this.invitations = new ArrayList<>();
        this.listener = listener;
    }

    public void setInvitations(List<AllianceInvitation> invitations) {
        this.invitations = invitations;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public InvitationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alliance_invitation, parent, false);
        return new InvitationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InvitationViewHolder holder, int position) {
        AllianceInvitation invitation = invitations.get(position);
        holder.bind(invitation);
    }

    @Override
    public int getItemCount() {
        return invitations.size();
    }

    class InvitationViewHolder extends RecyclerView.ViewHolder {
        private TextView tvAllianceName, tvInviterName, tvTimestamp;
        private Button btnAccept, btnReject;

        public InvitationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAllianceName = itemView.findViewById(R.id.tvAllianceName);
            tvInviterName = itemView.findViewById(R.id.tvInviterName);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }

        public void bind(AllianceInvitation invitation) {
            tvAllianceName.setText(invitation.getAllianceName());
            tvInviterName.setText("From: " + invitation.getInviterUsername());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            tvTimestamp.setText(sdf.format(new Date(invitation.getTimestamp())));

            btnAccept.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAccept(invitation);
                }
            });

            btnReject.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReject(invitation);
                }
            });
        }
    }
}

