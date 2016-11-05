package ua.cm.sensingtheenvironment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ua.cm.sensingtheenvironment.database.Sensor;

/**
 * A fragment representing a single Sensor detail screen.
 * This fragment is either contained in a {@link SensorListActivity}
 * in two-pane mode (on tablets) or a {@link SensorDetailActivity}
 * on handsets.
 */
public class SensorDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    /**
     * The dummy content this fragment is presenting.
     */
    private Sensor mItem;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SensorDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mItem = Sensor.findById(Sensor.class, getArguments().getLong(ARG_ITEM_ID));
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.

            //Activity activity = this.getActivity();
            //Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar);
            //activity.setSupportActionBar(toolbar);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.sensor_detail, container, false);

        // Show the dummy content as text in a TextView.
        if (mItem != null) {
            ((TextView) rootView.findViewById(R.id.sensor_detail)).setText(mItem.getMAC());
        }

        return rootView;
    }
}
