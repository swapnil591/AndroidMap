package com.example.mobiledevpc.androidmap.Api;

import android.os.AsyncTask;
import android.util.Log;


import com.example.mobiledevpc.androidmap.Activity.MainActivity;
import com.example.mobiledevpc.androidmap.Model.ParentRoute;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GetRouteDetails extends AsyncTask<String,Void,String> {


    private final MainActivity mainActivity;
    String data;


    public GetRouteDetails(MainActivity mainActivity) {

        this.mainActivity = mainActivity;
    }


    @Override
    protected String doInBackground(String... mapurl) {

        InputStream istream = null;

        HttpURLConnection httpURLConnection = null;
        try{
            URL url = new URL(mapurl[0]);
            Log.d("url",mapurl[0]);
            httpURLConnection = (HttpURLConnection)url.openConnection();

            httpURLConnection.connect();

            istream = httpURLConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(istream));

            StringBuffer stringBuffer = new StringBuffer();

            String line = null;

            while ((line = br.readLine()) != null){

                stringBuffer.append(line);
            }

            data = stringBuffer.toString();
            br.close();



        }catch (Exception e){

        }finally {
//            try {
//               istream.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            httpURLConnection.disconnect();
        }


        return data;
    }

    @Override
    protected void onPostExecute(String o) {
        super.onPostExecute(o);

        Gson gson = new Gson();
        ParentRoute parentRoute = gson.fromJson(o, ParentRoute.class);
        if( parentRoute!=null && parentRoute.getStatus().equalsIgnoreCase("OK"))
            mainActivity.onGetRouteDirectionData(parentRoute);
        //else
          //  Toast.makeText(mainActivity, "can't find route", Toast.LENGTH_SHORT).show();
    }
}
