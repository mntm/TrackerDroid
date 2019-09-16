package ca.polymtl.inf8405.g2.trackerdroid;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import ca.polymtl.inf8405.g2.trackerdroid.data.Path;
import ca.polymtl.inf8405.g2.trackerdroid.data.Trace;
import ca.polymtl.inf8405.g2.trackerdroid.manager.Manager;

public class MapsDialogFragment extends DialogFragment implements OnMapReadyCallback, RoutingListener, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;

    private HashMap<LatLng, Trace> traces = new HashMap<>();

    private AbstractRouting.TravelMode travelMode;
    private MapView mapView;

    public MapsDialogFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        assert getArguments() != null;
        Path path = (Path) getArguments().getSerializable(getString(R.string.waypoints_arg_name));
        assert path != null;
        for (Trace t : path.getTraces()) {
            traces.put(new LatLng(t.getLat(), t.getLng()), t);
        }
        travelMode = (AbstractRouting.TravelMode) getArguments().getSerializable(getString(R.string.travelmode_arg_name));

        this.setCancelable(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View inflate = inflater.inflate(R.layout.activity_maps, container, false);
        MapsInitializer.initialize(Objects.requireNonNull(this.getContext()));

        mapView = inflate.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        return inflate;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (traces.size() > 1 && travelMode != null) {
            this.drawOnMap(new ArrayList<>(traces.keySet()), travelMode);
            this.moveCamera(traces.keySet().iterator().next());
        } else {
            LatLng currentPosition = Manager.getInstance().getCurrentPosition();
            if (currentPosition != null) {
                this.moveCamera(currentPosition);
            }

        }
    }

    private void moveCamera(LatLng position) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 17));
    }

    public void drawOnMap(List<LatLng> waypoints, AbstractRouting.TravelMode travelMode) {
        mMap.clear();

        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.addAll(waypoints);
        mMap.addPolyline(polylineOptions);

        ArrayList<Route> routes = new ArrayList<>();
        Route route = new Route();
        route.addPoints(waypoints);
        routes.add(route);

        this.onRoutingSuccess(routes,0);
    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> routes, int shortestRouteIndex) {
        Log.d("Routing", "Success routing");
        //add routes(s) to the map.
        for (int i = 0; i < routes.size(); i++) {

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.width(10 + i * 3);
            polyOptions.color(Color.BLUE);
            polyOptions.addAll(routes.get(i).getPoints());
            mMap.addPolyline(polyOptions);

            Route route = routes.get(i);

            for (int j = 0; j < route.getPoints().size(); j++) {
                MarkerOptions options = new MarkerOptions();

                options.position(route.getPoints().get(i));

                StringBuilder sb = new StringBuilder();

                Trace trace = traces.get(options.getPosition());
                sb.append("Speed: ").append(trace.getSpeed()).append(" m/s");
                options.title("")
                        .snippet(sb.toString());

                mMap.addMarker(options);
                mMap.setOnMarkerClickListener(this);
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        new AlertDialog.Builder(getContext()).setMessage(marker.getSnippet()).setCancelable(true).create().show();
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onRoutingFailure(RouteException e) {
        Log.d("Routing", "Failure routing");
        e.printStackTrace();
    }

    @Override
    public void onRoutingStart() {
        Log.d("Routing", "Start routing");
    }

    @Override
    public void onRoutingCancelled() {
        Log.d("Routing", "Cancel routing");
    }
}
