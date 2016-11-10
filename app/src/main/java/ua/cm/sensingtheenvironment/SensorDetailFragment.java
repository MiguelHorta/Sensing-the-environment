package ua.cm.sensingtheenvironment;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.StreetViewPanoramaCamera;

import java.util.logging.Level;
import java.util.logging.Logger;

import ua.cm.sensingtheenvironment.database.Sensor;

/**
 * A fragment representing a single Sensor detail screen.
 * This fragment is either contained in a {@link SensorListActivity}
 * in two-pane mode (on tablets) or a {@link SensorDetailActivity}
 * on handsets.
 */

public class SensorDetailFragment extends Fragment implements OnMapReadyCallback {
    private static Logger log = Logger.getLogger("SenseTheEnv");
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    /**
     * The content this fragment is presenting.
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
        log.log(Level.INFO, String.format("%s", getArguments().getLong(ARG_ITEM_ID)));
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
            ((TextView) rootView.findViewById(R.id.given_name_content)).setText(mItem.getGivenName());
            ((TextView) rootView.findViewById(R.id.mac_content)).setText(mItem.getMAC());
            ((TextView) rootView.findViewById(R.id.desc_content)).setText(mItem.getDesc());
            SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager()
                    .findFragmentById(R.id.last_loc_content);
            mapFragment.getMapAsync(this);
        }
        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.sensor_detail_edit);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity(), EditSensor.class);
                i.putExtra(EditSensor.ARG_SENSOR_ID, getArguments().getLong(ARG_ITEM_ID));
                startActivity(i);
                getActivity().finish();
            }
        });
        return rootView;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        // Add a marker in Sydney and move the camera
        LatLng marker = new LatLng(mItem.getLatitude(), mItem.getLongitude());
        googleMap.addMarker(new MarkerOptions().position(marker).title("Last Seen " +mItem.getGivenName()));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marker,15));
    }


}
