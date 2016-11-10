package ua.cm.sensingtheenvironment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import ua.cm.sensingtheenvironment.database.Sensor;

import java.util.List;

/**
 * An activity representing a list of Sensors. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link SensorDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class SensorListActivity extends AppCompatActivity {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        View recyclerView = findViewById(R.id.sensor_list);
        assert recyclerView != null;
        setupRecyclerView((RecyclerView) recyclerView);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (findViewById(R.id.sensor_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }
    }
    @Override
    protected void onResume() {
        View recyclerView = findViewById(R.id.sensor_list);
        assert recyclerView != null;
        setupRecyclerView((RecyclerView) recyclerView);
        super.onResume();
    }
    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        List<Sensor> sensors =  Sensor.listAll(Sensor.class);
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(sensors));
    }

    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final List<Sensor> mValues;

        public SimpleItemRecyclerViewAdapter(List<Sensor> items) {
            mValues = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.sensor_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mItem = mValues.get(position);
            holder.mGivenNameView.setText(mValues.get(position).getGivenName());
            holder.mMACView.setText(mValues.get(position).getMAC());

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mTwoPane) {
                        Bundle arguments = new Bundle();
                        arguments.putLong(SensorDetailFragment.ARG_ITEM_ID, holder.mItem.getId());
                        SensorDetailFragment fragment = new SensorDetailFragment();
                        fragment.setArguments(arguments);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.sensor_detail_container, fragment)
                                .commit();
                    } else {
                        Context context = v.getContext();
                        Intent intent = new Intent(context, SensorDetailActivity.class);
                        intent.putExtra(SensorDetailFragment.ARG_ITEM_ID, holder.mItem.getId());

                        context.startActivity(intent);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mGivenNameView;
            public final TextView mMACView;
            public Sensor mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mGivenNameView = (TextView) view.findViewById(R.id.given_name);
                mMACView = (TextView) view.findViewById(R.id.mac_addr);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mGivenNameView.getText() + "(" + mMACView.getText() +"')";
            }
        }
    }
}
