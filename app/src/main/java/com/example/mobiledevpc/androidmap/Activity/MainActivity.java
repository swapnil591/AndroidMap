package com.example.mobiledevpc.androidmap.Activity;

import android.location.Location;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.example.mobiledevpc.androidmap.Api.GetRouteDetails;
import com.example.mobiledevpc.androidmap.Fragment.InfoFragment;
import com.example.mobiledevpc.androidmap.Fragment.MapFragment;
import com.example.mobiledevpc.androidmap.Fragment.SearchPanelFragment;
import com.example.mobiledevpc.androidmap.Model.ParentRoute;
import com.example.mobiledevpc.androidmap.Model.Route;
import com.example.mobiledevpc.androidmap.R;
import com.google.android.gms.maps.model.LatLng;
import io.fabric.sdk.android.Fabric;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        SearchPanelFragment.OnSearchPanelListener,
        MapFragment.OnMapEventListener{

    private SearchPanelFragment searchPanelFragment;
    private MapFragment mapFragment;
    private String str_source;
    private ParentRoute parentRoute;
    private LatLng destLocation;
    private FragmentTransaction fragmentTransaction;
    private FragmentManager fragmentManager;
    private LatLng sourceLocation;
    private InfoFragment infoFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        mapFragment = new MapFragment();
        //adding mapFragment in MainActivity
        fragmentTransaction.replace(R.id.mapContainer, mapFragment, "map");
        searchPanelFragment = new SearchPanelFragment();
        //adding search panel fragment in MainActivty
        fragmentTransaction.add(R.id.mapContainer, searchPanelFragment, "searchpanel");
        fragmentTransaction.commit();
    }

    @Override
    public void getCurrentLocation() {
        sourceLocation = mapFragment.showcurrentLocation();
        if(sourceLocation != null)
            str_source = "origin=" + sourceLocation.latitude + "," + sourceLocation.longitude;
    }

    @Override
    public void setSourceLocation(LatLng sourceLocation) {
        this.sourceLocation = sourceLocation;
        str_source = "origin=" + sourceLocation.latitude + "," + sourceLocation.longitude;
        mapFragment.setSourceLocation(sourceLocation);
    }

    @Override
    public void setDestinationLocation(LatLng destLocation) {
        this.destLocation = destLocation;
        String str_dest = "destination=" + destLocation.latitude + "," + destLocation.longitude;
        String param = str_source + "&" + str_dest + "&sensor=false";
        //direction api refrence from https://developers.google.com/maps/documentation/directions/intro
        new GetRouteDetails(this).execute("https://maps.googleapis.com/maps/api/directions/json?" + param);
    }

    @Override
    public void startNavigation() {
        mapFragment.createNavigationView();
    }

    @Override
    public void addInfoFragment() {
        mapFragment.createMapSnapshot();
    }

    @Override
    public void readySnapshot(boolean status) {
        if (status) {
            //snapshoot is ready add infofragment in MainActivity
            fragmentTransaction = fragmentManager.beginTransaction();
            infoFragment = new InfoFragment();
            fragmentTransaction.add(R.id.mapContainer, infoFragment, "infopanel");
            fragmentTransaction.addToBackStack("infopanel");
            fragmentTransaction.commit();
            infoFragment.setRouteData(parentRoute);
        } else {
            Toast.makeText(this, "Failed to capture snapshot. Try again.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void locationChanged(Location currLocation) {
        if (infoFragment != null) {
            String curr_source = "origin=" + currLocation.getLongitude() + "," + currLocation.getLongitude();
            String str_dest = "destination=" + destLocation.latitude + "," + destLocation.longitude;
            String param = curr_source + "&" + str_dest + "&sensor=false";
            new GetRouteDetails(this).execute("https://maps.googleapis.com/maps/api/directions/json?" + param);
        }
    }

    public void onGetRouteDirectionData(ParentRoute parentRoute) {
        this.parentRoute = parentRoute;
        Route route = parentRoute.getRoutes().get(0);
        if(route !=null) {
            if (infoFragment == null) {
                String overviewPolyline = route.getOverviewPolyline().getPoints();
                List<LatLng> polyPoints = decodePolyLine(overviewPolyline);
                mapFragment.setDestLocation(destLocation, polyPoints);
                searchPanelFragment.changeFABVisibility();
            } else {
                //update distance and duration from destination
                infoFragment.updateNavigationInfo(parentRoute.getRoutes().get(0).getLegs().get(0));
            }
        }
    }

    //decode polyline refrence from http://tecnepa.blogspot.in/2014/05/androiddecodepolystring-str-function-to.html
    private List<LatLng> decodePolyLine(final String poly) {
        int len = poly.length();
        int index = 0;
        List<LatLng> decoded = new ArrayList<LatLng>();
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            do {
                b = poly.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = poly.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            decoded.add(new LatLng(
                    lat / 100000d, lng / 100000d
            ));
        }
        return decoded;
    }
}
