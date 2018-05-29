package com.example.mobiledevpc.androidmap.Fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mobiledevpc.androidmap.Model.Leg;
import com.example.mobiledevpc.androidmap.Model.ParentRoute;
import com.example.mobiledevpc.androidmap.R;

import java.io.FileInputStream;

public class InfoFragment extends Fragment {


    private ImageView snapImageView;
    private ParentRoute parentRoute;
    private TextView distanceTextView;
    private TextView durationTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_info,container,false);
        snapImageView = view.findViewById(R.id.snapshotimg);
        distanceTextView = view.findViewById(R.id.distancevalue);
        durationTextView = view.findViewById(R.id.durationvalue);
        return  view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Leg route = parentRoute.getRoutes().get(0).getLegs().get(0);
        setLabelValue(route);
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            //get snapshot image file from storage
            FileInputStream in = new FileInputStream("/mnt/sdcard/Download/mapsnapshot.png");
            Bitmap b = BitmapFactory.decodeStream(in);
            snapImageView.setImageBitmap(b);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setRouteData(ParentRoute parentRoute) {
        this.parentRoute = parentRoute;
    }

    public void updateNavigationInfo(Leg currLocation) {
      setLabelValue(currLocation);
    }

    private void setLabelValue(Leg route) {
        //update distance and duration to destination
        distanceTextView.setText(route.getDistance().getText());
        durationTextView.setText(route.getDuration().getText());
    }
}
