package com.example.mobiledevpc.androidmap.Util;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.util.Property;
import android.view.animation.LinearInterpolator;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class AnimateUtil {


    public static void animateMarker(Marker marker, LatLng finalPosition) {
        final LatLngInterpolator latLngInterpolator = new LatLngInterpolator.Linear();

        TypeEvaluator<LatLng> typeEvaluator = new TypeEvaluator<LatLng>() {
            @Override
            public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
                return latLngInterpolator.interpolate(fraction, startValue, endValue);
            }
        };
        Property<Marker, LatLng> property = Property.of(Marker.class, LatLng.class, "position");
        ObjectAnimator animator = ObjectAnimator
                .ofObject(marker, property, typeEvaluator, finalPosition);
        animator.setDuration(3000);
        animator.setInterpolator(new LinearInterpolator());
        animator.start();
    }

    interface LatLngInterpolator {

        LatLng interpolate(float fraction, LatLng a, LatLng b);

        class Linear implements LatLngInterpolator {

            @Override
            public LatLng interpolate(float fraction, LatLng a, LatLng b) {
                double lat = (b.latitude - a.latitude) * fraction + a.latitude;
                double lngDelta = b.longitude - a.longitude;

                // Take the shortest path across the 180th meridian.
                if (Math.abs(lngDelta) > 180) {
                    lngDelta -= Math.signum(lngDelta) * 360;
                }
                double lng = lngDelta * fraction + a.longitude;
                return new LatLng(lat, lng);
            }
        }
    }

}
