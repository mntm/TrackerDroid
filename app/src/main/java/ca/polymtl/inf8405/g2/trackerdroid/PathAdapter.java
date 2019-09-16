package ca.polymtl.inf8405.g2.trackerdroid;

import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import ca.polymtl.inf8405.g2.trackerdroid.data.Path;

public class PathAdapter extends RecyclerView.Adapter<PathAdapter.ViewHolder> {
    private ArrayList<Path> mPathList = new ArrayList<>();
    private static AtomicReference<ActionListener> listener = new AtomicReference<>();

    public PathAdapter(ActionListener listener) {
        PathAdapter.listener.set(listener);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.path_item_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Path path = mPathList.get(position);

        holder.setSteps(String.valueOf(path.getTotalSteps()) + " steps");
        holder.setDtStart(DateFormat.getDateTimeInstance().format(path.getStart()));
    }


    @Override
    public int getItemCount() {
        return mPathList.size();
    }

    public void addItem(Path p) {
        this.mPathList.add(p);
        this.notifyDataSetChanged();
    }

    public void removeItem(Path p) {
        this.mPathList.remove(p);
        this.notifyDataSetChanged();
    }

    public Path getItemAt(int position) {
        return this.mPathList.get(position);
    }

    public void removeItem(int i) {
        if (this.mPathList.get(i) != null) {
            this.mPathList.remove(i);
            this.notifyDataSetChanged();
        }
    }

    public interface ActionListener extends View.OnClickListener, View.OnLongClickListener {

    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private AppCompatTextView steps;
        private AppCompatTextView dtStart;

        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setClickable(true);
            itemView.setOnClickListener(listener.get());
            itemView.setLongClickable(true);
            itemView.setOnLongClickListener(listener.get());

            steps = itemView.findViewById(R.id.txt_steps);
            dtStart = itemView.findViewById(R.id.txt_dt_start);
        }

        public AppCompatTextView getSteps() {
            return steps;
        }

        public void setSteps(String steps) {
            this.steps.setText(steps);
        }

        public AppCompatTextView getDtStart() {
            return dtStart;
        }

        public void setDtStart(String dtStart) {
            this.dtStart.setText(dtStart);
        }
    }
}
