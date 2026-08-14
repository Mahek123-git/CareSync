package com.caresync.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.caresync.app.R;
import java.util.List;

public class PersonAdapter extends RecyclerView.Adapter<PersonAdapter.VH> {

    public interface OnPersonClickListener {
        void onPersonClick(String[] person);
    }

    private final List<String[]> list; // [name, subtitle, optional_id]
    private OnPersonClickListener listener;

    public PersonAdapter(List<String[]> list) { this.list = list; }

    public void setOnPersonClickListener(OnPersonClickListener listener) {
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_person, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        String[] person = list.get(pos);
        h.tvName.setText(person[0]);
        h.tvSub.setText(person.length > 1 ? person[1] : "");
        String initial = (person[0] != null && !person[0].isEmpty())
                ? String.valueOf(person[0].charAt(0)).toUpperCase() : "?";
        h.tvInitial.setText(initial);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPersonClick(person);
        });
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvSub, tvInitial;
        LinearLayout layoutAvatar;
        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvName);
            tvSub = v.findViewById(R.id.tvSubtitle);
            tvInitial = v.findViewById(R.id.tvInitial);
            layoutAvatar = v.findViewById(R.id.layoutAvatar);
        }
    }
}
