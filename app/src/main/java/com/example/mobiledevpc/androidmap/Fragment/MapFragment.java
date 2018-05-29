package com.example.mobiledevpc.androidmap.Fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.mobiledevpc.androidmap.Util.AnimateUtil;
import com.example.mobiledevpc.androidmap.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;


public class MapFragment extends Fragment implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private View view;
    private MapView mapView;

    private Location currentLocation;
    private LocationManager locationManager;

    private boolean mIsRunning = true;
    private boolean navigationFlag = false;

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1;
    private static final long MIN_TIME_BW_UPDATES = 10000;

    private LatLng sourceLocation;
    private OnMapEventListener onMapEventListener;
    private Marker nav_marker;
    private Polyline routePolylines;

    //check gps status
    private void checkGpsStatus() {
        LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            getLocationManager();
            //start listening for location updates
            requestForLocationUpdate();
        } else
            showDialogForEnableGPS();

    }

    //show gps setting dialog
    public void showDialogForEnableGPS() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        final String action = Settings.ACTION_LOCATION_SOURCE_SETTINGS;
        final String message = "Do you want open GPS setting?";

        builder.setMessage(message)
                .setPositiveButton("OK",
                        (d, id) -> {
                            startActivity(new Intent(action));
                            d.dismiss();
                        })
                .setNegativeButton("Cancel",
                        (d, id) -> d.cancel());
        builder.create().show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //remove all markers and routes from map
        clearMap();
        mIsRunning = false;
    }

    @Override
    public void onStop() {
        super.onStop();
        //stop location updation
        if (navigationFlag)
            locationManager.removeUpdates(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_map, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapView = view.findViewById(R.id.mapView);
        if (mapView != null) {
            mapView.onCreate(null);
            mapView.onResume();
            mapView.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }

    //show current location on map with green drop pin on map
    public LatLng showcurrentLocation() {
        //check for gps status befor request current location
        checkGpsStatus();
       // requestForLocationUpdate();
        if (currentLocation != null) {

            LatLng latlng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            sourceLocation = latlng;
            showMarkerOnMap(latlng, BitmapDescriptorFactory.HUE_GREEN);
            locationManager.removeUpdates(this);
        }
        return sourceLocation;
    }

    //show markers on map
    private void showMarkerOnMap(LatLng latlng, float hueGreen) {
        mMap.addMarker(new MarkerOptions().position(latlng)
                .icon(BitmapDescriptorFactory.defaultMarker(hueGreen)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 17));
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
        if (navigationFlag) {
            //send chenged location toupdate info fragment
            onMapEventListener.locationChanged(location);
        }
        //check if there is previous marker on map and remove it
        if (nav_marker != null)
            nav_marker.remove();
        //show navigation marker on map
        showNavigationMarker(latlng);
        //Toast.makeText(getActivity(), location.getProvider()+" latitude-" + latlng.latitude + ", longitude" + latlng.longitude, Toast.LENGTH_SHORT).show();
    }

    private void showNavigationMarker(LatLng latlng) {
        nav_marker = mMap.addMarker(new MarkerOptions().position(latlng)
                .flat(true)
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
        nav_marker.setRotation((float) getAngle(0));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 17));
        AnimateUtil.animateMarker(nav_marker, latlng); //Animate marker

    }

    //get marker rotation
    private double getAngle(int i) {
        if ((i + 1) >= routePolylines.getPoints().size()) {
            throw new RuntimeException("index out of bonds");
        }
        LatLng startPoint = routePolylines.getPoints().get(i);
        LatLng endPoint = routePolylines.getPoints().get(i + 1);
        return getAngle(startPoint, endPoint);
    }

    private double getAngle(LatLng startPoint, LatLng endPoint) {
        double slope = getSlope(startPoint, endPoint);
        if (slope == Double.MAX_VALUE) {
            if (endPoint.latitude > startPoint.latitude)
                return 0;
            else
                return 180;
        }
        float deltAngle = 0;
        if ((endPoint.latitude - startPoint.latitude) * slope < 0)
            deltAngle = 180;
        double radio = Math.atan(slope);
        double angle = 180 * (radio / Math.PI) + deltAngle - 90;
        return angle;
    }

    private double getSlope(LatLng startPoint, LatLng endPoint) {
        if (endPoint.longitude == startPoint.longitude) {
            return Double.MAX_VALUE;
        }
        double slope = ((endPoint.latitude - startPoint.latitude) / (endPoint.longitude - startPoint.longitude));
        return slope;
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
        //getLocationManager();
    }

    //initialise location manager
    private void getLocationManager() {
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
    }

    @SuppressLint("MissingPermission")
    private void requestForLocationUpdate() {
        Location gpscurrentLocation = null;
        Location networkcurrentLocation = null;

        if (locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
            networkcurrentLocation = locationManager
                    .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        if (locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
            gpscurrentLocation = locationManager
                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        if (gpscurrentLocation != null && networkcurrentLocation != null)
            currentLocation = getBetterLocation(gpscurrentLocation, networkcurrentLocation);
        else if (gpscurrentLocation != null) {
            currentLocation = gpscurrentLocation;
        } else if (networkcurrentLocation != null) {
            currentLocation = networkcurrentLocation;
        } else {
            Toast.makeText(getActivity(), "no gps provider avilable", Toast.LENGTH_SHORT).show();
        }
    }

    //check for better location by coparing time and accuracy of location
    private Location getBetterLocation(Location gpscurrentLocation, Location networkcurrentLocation) {
        long timeDelta = gpscurrentLocation.getTime() - networkcurrentLocation.getTime();
        int accuracyDelta = (int) (gpscurrentLocation.getAccuracy() - networkcurrentLocation.getAccuracy());
        boolean isNewer = timeDelta > 0;
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;
        if (isMoreAccurate)
            return gpscurrentLocation;
        else if (isNewer && !isLessAccurate)
            return gpscurrentLocation;
        else if (isNewer && !isSignificantlyLessAccurate)
            return gpscurrentLocation;
        else
            return networkcurrentLocation;
    }

    @Override
    public void onProviderDisabled(String provider) {
        getLocationManager();
    }

    public void setSourceLocation(LatLng location) {
        clearMap();
        this.sourceLocation = location;
        showMarkerOnMap(sourceLocation, BitmapDescriptorFactory.HUE_GREEN);
    }

    //set destination marker drop pin on map and show polyline path between source and destination
    public void setDestLocation(LatLng destLocation, List<LatLng> polyPoints) {
        clearMap();
        showMarkerOnMap(sourceLocation, BitmapDescriptorFactory.HUE_GREEN);
        showMarkerOnMap(destLocation, BitmapDescriptorFactory.HUE_RED);
        PolylineOptions polylineOption = new PolylineOptions();
        polylineOption.width(10);
        polylineOption.color(Color.RED);
        polylineOption.startCap(new SquareCap());
        polylineOption.endCap(new SquareCap());
        polylineOption.jointType(JointType.ROUND);
        polylineOption.addAll(polyPoints);
        routePolylines = mMap.addPolyline(polylineOption);
        mMap.setMinZoomPreference(2.0f);
        mMap.setMaxZoomPreference(17.0f);
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(sourceLocation).include(destLocation);
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 50));

    }

    private void clearMap() {
        mMap.clear();
    }

    //cheang map configuration to navigation and start location updation
    public void createNavigationView() {
        mMap.setBuildingsEnabled(true);
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(sourceLocation)
                .bearing(30)
                .zoom(20)
                .tilt(30)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        navigationFlag = true;
        requestForLocationUpdate();
    }


    public void createMapSnapshot() {
        GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback() {
            public OutputStream fileOutputStream;
            public FileOutputStream out;
            Bitmap bitmap;

            @Override
            public void onSnapshotReady(Bitmap snapshot) {
                bitmap = snapshot;
                try {
                    out = new FileOutputStream("/mnt/sdcard/Download/mapsnapshot.png");
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                    out.close();
                    onMapEventListener.readySnapshot(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    onMapEventListener.readySnapshot(false);
                }
            }
        };
        mMap.snapshot(callback);
    }

    //refrence https://developer.android.com/training/basics/fragments/communicating
    public interface OnMapEventListener {
        void readySnapshot(boolean status);
        void locationChanged(Location currLocation);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            onMapEventListener = (OnMapEventListener) context;
        } catch (Exception e) {
            Toast.makeText(context, "interface not implemented by parent activity ", Toast.LENGTH_SHORT).show();
        }
    }

}