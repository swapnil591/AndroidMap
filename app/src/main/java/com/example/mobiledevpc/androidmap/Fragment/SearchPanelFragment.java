package com.example.mobiledevpc.androidmap.Fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.example.mobiledevpc.androidmap.R;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.model.LatLng;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;


public class SearchPanelFragment extends Fragment {


    private View view;
    private EditText sourceEditText;
    private EditText destinationEditText;
    int SOURCE_PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    int DESTINATION_PLACE_AUTOCOMPLETE_REQUEST_CODE = 2;
    private FloatingActionButton currentlocationFAB;
    private OnSearchPanelListener onSearchPanelListener;
    private FloatingActionButton navigationFAB;
    private FloatingActionButton googlemapFAB;
    private FloatingActionButton infoFAB;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_seach_panel, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sourceEditText = view.findViewById(R.id.searchEditText);
        destinationEditText = view.findViewById(R.id.destinationEditText);
        currentlocationFAB = view.findViewById(R.id.floatingActionButton);
        navigationFAB = view.findViewById(R.id.navigationFAB);
        googlemapFAB = view.findViewById(R.id.googlemapFAB);
        infoFAB = view.findViewById(R.id.infoFAB);
        sourceEditText.setOnClickListener(v -> {
            try {
                //call for search location auto search
                Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN).build(getActivity());
                startActivityForResult(intent, SOURCE_PLACE_AUTOCOMPLETE_REQUEST_CODE);
            } catch (GooglePlayServicesRepairableException e) {
                Log.e("Error", e.getMessage());
            } catch (GooglePlayServicesNotAvailableException e) {
                Log.e("Error", e.getMessage());
            }
        });

        destinationEditText.setOnClickListener(v -> {
            try {
                //call for destination location auto search
                Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN).build(getActivity());
                startActivityForResult(intent, DESTINATION_PLACE_AUTOCOMPLETE_REQUEST_CODE);
            } catch (GooglePlayServicesRepairableException e) {
                Log.e("Error", e.getMessage());
            } catch (GooglePlayServicesNotAvailableException e) {
                Log.e("Error", e.getMessage());
            }
        });

        currentlocationFAB.setOnClickListener(v -> {
            // request for current location
            onSearchPanelListener.getCurrentLocation();
            sourceEditText.setText("Current Location");
            destinationEditText.requestFocus();
        });

        navigationFAB.setOnClickListener(v -> {
            // start navigation
            onSearchPanelListener.startNavigation();
        });

        infoFAB.setOnClickListener(v -> {
            // add infofragment
            onSearchPanelListener.addInfoFragment();
        });

        googlemapFAB.setOnClickListener(v -> {
            //open current route in google map
            //google map intent refrence from https://developers.google.com/maps/documentation/urls/android-intents
            String destLoc = destinationEditText.getText().toString().trim();
            if (!sourceEditText.getText().toString().trim().equalsIgnoreCase("Current Location")) {
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                        Uri.parse("http://maps.google.com/maps?saddr=" + sourceEditText.getText().toString().trim() + "&daddr=" +destLoc));
                intent.setPackage("com.google.android.apps.maps");
                startActivity(intent);
            } else
            {
                Uri gmmIntentUri = Uri.parse("google.navigation:q="+ destLoc);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == SOURCE_PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place sourceplace = PlaceAutocomplete.getPlace(getActivity(), data);
                sourceEditText.setText(sourceplace.getName());
                destinationEditText.requestFocus();
                onSearchPanelListener.setSourceLocation(sourceplace.getLatLng());
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(getActivity(), data);
                Log.e("Error", status.toString());
            } else if (resultCode == RESULT_CANCELED) {
                Log.e("Error", "User canceled the operation");
            }
        } else if (requestCode == DESTINATION_PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place destinationplace = PlaceAutocomplete.getPlace(getActivity(), data);
                destinationEditText.setText(destinationplace.getName());
                onSearchPanelListener.setDestinationLocation(destinationplace.getLatLng());
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(getActivity(), data);
                Log.e("Error", status.toString());
            } else if (resultCode == RESULT_CANCELED) {
                Log.e("Error", "User canceled the operation");
            }
        } else {
            Toast.makeText(getActivity(), "Invalid request code " + requestCode, Toast.LENGTH_SHORT).show();
        }
    }

    public void changeFABVisibility() {
        navigationFAB.setVisibility(View.VISIBLE);
        googlemapFAB.setVisibility(View.VISIBLE);
        infoFAB.setVisibility(View.VISIBLE);
    }

    //refrence https://developer.android.com/training/basics/fragments/communicating
    public interface OnSearchPanelListener {
        void getCurrentLocation();
        void setSourceLocation(LatLng location);
        void setDestinationLocation(LatLng location);
        void startNavigation();
        void addInfoFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            onSearchPanelListener = (OnSearchPanelListener) context;
        } catch (Exception e) {
            Toast.makeText(context, "interface not implemented by parent activity ", Toast.LENGTH_SHORT).show();
        }
    }
}
