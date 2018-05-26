package com.example.mobiledevpc.androidmap.Fragment;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.example.mobiledevpc.androidmap.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
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

    private boolean isGPSEnabled;
    private boolean isNetworkEnabled;
    private boolean navigationFlag = false;

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1;
    private static final long MIN_TIME_BW_UPDATES = 5000;

    private LatLng sourceLocation;
    private OnMapEventListener onMapEventListener;
    private Marker nav_marker;


    private void checkGpsStatus() {
        LocationManager lm = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);
        if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            getLocationManager();
            requestForLocationUpdate();
        }
        else
            showDialogForEnableGPS();
    }

    public  void showDialogForEnableGPS()
    {
        final AlertDialog.Builder builder =  new AlertDialog.Builder(getContext());
        final String action = Settings.ACTION_LOCATION_SOURCE_SETTINGS;
        final String message = "Do you want open GPS setting?";

        builder.setMessage(message)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int id) {
                                startActivity(new Intent(action));
                                d.dismiss();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int id) {
                                d.cancel();
                            }
                        });
        builder.create().show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        clearMap();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(navigationFlag)
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

    public LatLng showcurrentLocation() {
        checkGpsStatus();
        requestForLocationUpdate();
        if (currentLocation != null) {

            LatLng latlng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            sourceLocation = latlng;
            showMarkerOnMap(latlng, BitmapDescriptorFactory.HUE_GREEN);
            locationManager.removeUpdates(this);
        }
        return sourceLocation;
    }

    private void showMarkerOnMap(LatLng latlng, float hueGreen) {
        mMap.addMarker(new MarkerOptions().position(latlng)
                .icon(BitmapDescriptorFactory.defaultMarker(hueGreen)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 17));
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
        if(navigationFlag) {
            onMapEventListener.locationChanged(location);
        }
        
        if(nav_marker!=null)
            nav_marker.remove();

        showNavigationMarker(latlng);
    }

    private void showNavigationMarker(LatLng latlng) {
         nav_marker = mMap.addMarker(new MarkerOptions().position(latlng)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 17));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) {
        getLocationManager();
    }

    private void getLocationManager() {
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        isGPSEnabled = locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnabled = locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (isGPSEnabled == false && isNetworkEnabled == false) {
            Toast.makeText(getActivity(), "no gps provider avilable", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("MissingPermission")
    private void requestForLocationUpdate() {
        if (isGPSEnabled) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
            if (locationManager != null) {
                currentLocation = locationManager
                        .getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
        }

        if (currentLocation == null && isNetworkEnabled) {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
            if (locationManager != null) {
                currentLocation = locationManager
                        .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        }
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
        mMap.addPolyline(polylineOption);
        mMap.setMinZoomPreference(2.0f);
        mMap.setMaxZoomPreference(17.0f);
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(sourceLocation).include(destLocation);
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 50));
    }

    private void clearMap() {
        mMap.clear();
    }

    public void createNavigationView() {
        mMap.setBuildingsEnabled(true);
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(sourceLocation)
                .bearing(30)
                .zoom(20)
                .tilt(30)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        navigationFlag =true;
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
